package cd.ghost.myapplication.navigation.di

import cd.ghost.addedittask.di.AddEditTaskSubcomponent
import cd.ghost.common.di.ActivityScope
import cd.ghost.common.di.ViewModelBuilderModule
import cd.ghost.detailtask.di.DetailTaskSubcomponent
import cd.ghost.myapplication.di.AppComponent
import cd.ghost.myapplication.navigation.MainActivity
import cd.ghost.statistics.di.StatisticsSubcomponent
import cd.ghost.tasks.di.TasksSubcomponent
import dagger.Component

@ActivityScope
@Component(
    dependencies = [
        AppComponent::class
    ],
    modules = [
        ActivityModule::class,
        ActionsBindModule::class,
        SubcomponentsModule::class,
        ViewModelBuilderModule::class,
    ]
)
interface ActivityComponent {

    @Component.Factory
    interface Factory {
        fun create(appComponent: AppComponent): ActivityComponent
    }

    fun inject(activity: MainActivity)

    fun taskSubcomponent(): TasksSubcomponent.Factory

    fun detailTaskSubcomponent(): DetailTaskSubcomponent.Factory

    fun addEditTaskSubcomponent(): AddEditTaskSubcomponent.Factory

    fun statisticsSubcomponent(): StatisticsSubcomponent.Factory

}