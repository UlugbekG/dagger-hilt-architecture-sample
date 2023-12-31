package cd.ghost.myapplication.navigation.actions

import cd.ghost.detailtask.DetailTaskFragment.DetailTaskArgument
import cd.ghost.myapplication.R
import cd.ghost.tasks.TasksRouter
import javax.inject.Inject



class TasksDestinations @Inject constructor(
    private val destinationLauncher: DestinationLauncher
) : TasksRouter {

    override fun navigateToAddOrNewTask() {
        destinationLauncher.launch(R.id.action_tasksFragment_to_addEditTaskFragment)
    }

    override fun navigateToDisplayTask(taskId: String) {
        destinationLauncher.launch(
            destinationId = R.id.action_tasksFragment_to_detailTaskFragment,
            args = DetailTaskArgument(taskId)
        )
    }

}