package de.batram.dnsstrolch


import android.content.Intent
import android.os.Bundle
import android.util.Log.v
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.batram.dnsstrolch.ui.FILTER_DINGENS
import de.batram.dnsstrolch.ui.Filter
import de.batram.dnsstrolch.ui.FilterWrangler


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_allowed, R.id.navigation_fresh, R.id.navigation_ignored, R.id.navigation_settings
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.colorAccent)

        val connectButton: Button = findViewById<View>(R.id.connect_button) as Button
        if (TunnelVision.state == TunnelVision.State.Connected || TunnelVision.state == TunnelVision.State.Setup) {
            connectButton.text = "Disconnect"
        } else {
            connectButton.text = "Connect"
        }

        connectButton.setOnClickListener { view ->
            if (TunnelVision.state == TunnelVision.State.Connected || TunnelVision.state == TunnelVision.State.Setup) {
                v("connect button", "try to close!!!!")
                TunnelVision.close_tunnel()
            } else {
                val intent = android.net.VpnService.prepare(view.context)
                if (intent != null) {
                    startActivityForResult(intent, 1)
                }
                TunnelVision.setupTunnel(view.context)
            }
        }
        TunnelVision.connectActivity = this

        val newFilterButton: Button = findViewById<View>(R.id.newFilterButton) as Button
        newFilterButton.setOnClickListener { view ->
            var filter = Filter( "", Filter.Type.Unknown);
            val myIntent = Intent(view.context, EditFilter::class.java).apply {
                putExtra(FILTER_DINGENS, filter)
            }
            view.context.startActivity(myIntent)
        }

        val sortButton: Button = findViewById<View>(R.id.sortButton) as Button
        sortButton.setOnClickListener { view ->
            //TODO: DropDown select sort type
            val popupMenu = PopupMenu(this, view)
            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.sort_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener {
                val sType = when(it.itemId){
                    R.id.sortMenuAlpha -> FilterWrangler.SortType.ALPHABET
                    R.id.sortMenuTime -> FilterWrangler.SortType.TIME
                    R.id.sortMenuRDomain -> FilterWrangler.SortType.RDOMAIN
                    R.id.sortMenuHitCount -> FilterWrangler.SortType.HITCOUNT
                    else -> FilterWrangler.SortType.ALPHABET
                }
                DNSStrolch.filterWrangler.sortLists(sType);
                true
            }
            popupMenu.show()
        }

    }
}
