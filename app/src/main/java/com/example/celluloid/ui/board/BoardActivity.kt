package com.example.celluloid.ui.board

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.example.celluloid.BuildConfig
import com.example.celluloid.R
import com.example.celluloid.data.sources.AuthSource

class BoardActivity : AppCompatActivity() {
    companion object {
        const val USERNAME = BuildConfig.APPLICATION_ID + ".UserName"
        const val PWHASH = BuildConfig.APPLICATION_ID + ".PasswordHash"
    }

    private val extras by lazy { intent.extras }
    private val nav by lazy { findNavController(R.id.nav_host_fragment) }
    private val drawer by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val view by lazy { findViewById<NavigationView>(R.id.nav_view) }
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val welcome by lazy { view.getHeaderView(0).findViewById<TextView>(R.id.welcome) }
    private val appbar by lazy { AppBarConfiguration(nav.graph, drawer) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(nav, appbar)
        view.setupWithNavController(nav)

        val username = extras?.getString(USERNAME)
            ?: throw IllegalArgumentException("Activity argument missed: $USERNAME")
        welcome.text = getString(R.string.welcome, username)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appbar) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(view)) {
            drawer.closeDrawer(view)
        } else {
            super.onBackPressed()
        }
    }
}
