package cd.ghost.myapplication.navigation.di

import cd.ghost.addedittask.di.AddEditTaskSubcomponent
import cd.ghost.detailtask.di.DetailTaskSubcomponent
import cd.ghost.statistics.di.StatisticsSubcomponent
import cd.ghost.tasks.di.TasksSubcomponent
import dagger.Module

@Module(
    subcomponents = [
        TasksSubcomponent::class,
        DetailTaskSubcomponent::class,
        AddEditTaskSubcomponent::class,
        StatisticsSubcomponent::class
    ]
)
class SubcomponentsModule