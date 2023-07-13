package cd.ghost.myapplication.navigation

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import cd.ghost.myapplication.MyApplication
import cd.ghost.myapplication.R
import cd.ghost.myapplication.navigation.di.ActivityComponent
import cd.ghost.myapplication.navigation.di.DaggerActivityComponent
import cd.ghost.tasks.di.TasksComponent
import cd.ghost.tasks.di.TasksComponentProvider
import com.google.android.material.navigation.NavigationView
import javax.inject.Inject

class MainActivity : AppCompatActivity(), NavControllerHolder, TasksComponentProvider {

    @Inject
    lateinit var destinationLauncher: DestinationLauncher

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appComponent = (application as MyApplication).appComponent

        activityComponent = DaggerActivityComponent
            .factory()
            .create(appComponent)
            .apply { inject(this@MainActivity) }

        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setStatusBarBackground(cd.ghost.common.R.color.colorPrimaryDark)

        setSupportActionBar(findViewById(R.id.toolbar))

        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHost.navController

        destinationLauncher.onCreate(this)

        appBarConfiguration =
            AppBarConfiguration.Builder(R.id.tasksFragment /* R.id.statistics_fragment_dest */)
                .setDrawerLayout(drawerLayout).build()

        setupActionBarWithNavController(navController, appBarConfiguration)

        findViewById<NavigationView>(R.id.nav_view).setupWithNavController(navController)
    }


    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_START_DESTINATION, navController.saveState())

    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle?, persistentState: PersistableBundle?
    ) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        navController.restoreState(savedInstanceState?.getBundle(KEY_START_DESTINATION))
    }

    private companion object {
        const val KEY_START_DESTINATION = "startDestination"
    }

    override fun onStart() {
        super.onStart()
        destinationLauncher.onStart()
    }

    override fun onStop() {
        super.onStop()
        destinationLauncher.onStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        destinationLauncher.onDestroy()
    }

    override fun navController(): NavController {
        return navController
    }

    override fun provideTaskComponent(): TasksComponent.Factory {
        return activityComponent.taskSubcomponent()
    }

}