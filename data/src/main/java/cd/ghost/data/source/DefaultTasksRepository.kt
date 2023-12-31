package cd.ghost.data.source

import cd.ghost.data.Result
import cd.ghost.data.Result.Error
import cd.ghost.data.Result.Success
import cd.ghost.data.Task
import cd.ghost.data.source.di.TaskLocalDataSource
import cd.ghost.data.source.di.TaskRemoteDataSource
import cd.ghost.data.succeeded
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject

class DefaultTasksRepository @Inject constructor(
    @TaskRemoteDataSource private val tasksRemoteDataSource: TasksDataSource,
    @TaskLocalDataSource private val tasksLocalDataSource: TasksDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TasksRepository {

    private var cachedTasks: ConcurrentMap<String, Task>? = null

    override suspend fun getTasks(forceUpdate: Boolean): Result<List<Task>> {
        return withContext(ioDispatcher) {
            // Respond immediately with cache if available and not dirty
            if (!forceUpdate) {
                cachedTasks?.let { cachedTasks ->
                    return@withContext Success(cachedTasks.values.sortedBy { it.id })
                }
            }

            val newTasks = fetchTasksFromRemoteOrLocal(forceUpdate)

            // Refresh the cache with the new tasks
            (newTasks as? Success)?.let { refreshCache(it.data) }

            cachedTasks?.values?.let { tasks ->
                return@withContext Success(tasks.sortedBy { it.id })
            }

            (newTasks as? Success)?.let {
                if (it.data.isEmpty()) {
                    return@withContext Success(it.data)
                }
            }

            return@withContext Error(Exception("Illegal state"))
        }
    }

    private suspend fun fetchTasksFromRemoteOrLocal(forceUpdate: Boolean): Result<List<Task>> {
        // remote fetch
        when (val remoteData = tasksRemoteDataSource.getTasks()) {
            is Error -> {
                // Timber.w("Remote data source fetch failed")
            }

            is Success -> {
                refreshLocalDataSource(remoteData.data)
                return remoteData
            }

            else -> throw IllegalStateException()
        }

        // Don't read from local if it's forced
        if (forceUpdate) {
            return Error(Exception("Refresh failed"))
        }

        // Local if remote fails
        val localTasks = tasksLocalDataSource.getTasks()
        if (localTasks.succeeded) return localTasks
        return Error(Exception("Error fetching from remote and local"))
    }

    override suspend fun getTask(taskId: String, forceUpdate: Boolean): Result<Task> {
        return withContext(ioDispatcher) {
            if (!forceUpdate) {
                getTaskWithId(taskId)?.let {
                    return@withContext Success(it)
                }
            }

            val newTask = fetchTaskFromRemoteOrLocal(taskId, forceUpdate)

            (newTask as? Success)?.let { cacheTask(it.data) }

            return@withContext newTask
        }
    }

    private suspend fun fetchTaskFromRemoteOrLocal(
        taskId: String,
        forceUpdate: Boolean
    ): Result<Task> {
        // Remote first
        val remoteTask = tasksRemoteDataSource.getTask(taskId)
        when (remoteTask) {
            is Error -> {
                // Timber.w("Remote data source fetch failed")
            }

            is Success -> {
                refreshLocalDataSource(remoteTask.data)
                return remoteTask
            }

            else -> throw IllegalStateException()
        }

        // Don't read from local if it's forced
        if (forceUpdate) {
            return Error(Exception("Refresh failed"))
        }

        // Local if remote fails
        val localTasks = tasksLocalDataSource.getTask(taskId)
        if (localTasks.succeeded) return localTasks
        return Error(Exception("Error fetching from remote and local"))
    }


    override suspend fun saveTask(task: Task) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(task) {
            coroutineScope {
                launch { tasksRemoteDataSource.saveTask(it) }
                launch { tasksLocalDataSource.saveTask(it) }
            }
        }
    }

    override suspend fun completeTask(task: Task) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(task) {
            it.isCompleted = true
            coroutineScope {
                launch { tasksRemoteDataSource.completeTask(it) }
                launch { tasksLocalDataSource.completeTask(it) }
            }
        }
    }

    override suspend fun completeTask(taskId: String) {
        withContext(ioDispatcher) {
            getTaskWithId(taskId)?.let {
                completeTask(it)
            }
        }
    }

    override suspend fun activateTask(task: Task) {
        // Do in memory cache update to keep the app UI up to date
        cacheAndPerform(task) {
            it.isCompleted = false
            coroutineScope {
                launch { tasksRemoteDataSource.activateTask(it) }
                launch { tasksLocalDataSource.activateTask(it) }
            }

        }
    }

    override suspend fun activateTask(taskId: String) {
        withContext(ioDispatcher) {
            getTaskWithId(taskId)?.let {
                activateTask(it)
            }
        }
    }

    override suspend fun clearCompletedTasks() {
        coroutineScope {
            launch { tasksRemoteDataSource.clearCompletedTasks() }
            launch { tasksLocalDataSource.clearCompletedTasks() }
        }
        withContext(ioDispatcher) {
            cachedTasks?.entries?.removeAll { it.value.isCompleted }
        }
    }

    override suspend fun deleteAllTasks() {
        withContext(ioDispatcher) {
            coroutineScope {
                launch { tasksRemoteDataSource.deleteAllTasks() }
                launch { tasksLocalDataSource.deleteAllTasks() }
            }
        }
        cachedTasks?.clear()
    }

    override suspend fun deleteTask(taskId: String) {
        coroutineScope {
            launch { tasksRemoteDataSource.deleteTask(taskId) }
            launch { tasksLocalDataSource.deleteTask(taskId) }
        }

        cachedTasks?.remove(taskId)
    }

    private fun getTaskWithId(id: String) = cachedTasks?.get(id)

    private suspend fun refreshLocalDataSource(task: Task) {
        tasksLocalDataSource.saveTask(task)
    }

    private suspend fun refreshLocalDataSource(tasks: List<Task>) {
        tasksLocalDataSource.deleteAllTasks()
        for (task in tasks) {
            tasksLocalDataSource.saveTask(task)
        }
    }

    private fun refreshCache(tasks: List<Task>) {
        cachedTasks?.clear()
        tasks.sortedBy { it.id }.forEach {
            cacheAndPerform(it) {

            }
        }
    }

    private fun cacheTask(task: Task): Task {
        val cachedTask = Task(task.title, task.description, task.isCompleted, task.id)
        // Create if it doesn't exist.
        if (cachedTasks == null) {
            cachedTasks = ConcurrentHashMap()
        }
        cachedTasks?.put(cachedTask.id, cachedTask)
        return cachedTask
    }

    private inline fun cacheAndPerform(task: Task, perform: (Task) -> Unit) {
        val cachedTask = cacheTask(task)
        perform(cachedTask)
    }
}