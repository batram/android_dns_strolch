package de.batram.dnsstrolch

import android.app.Application
import de.batram.dnsstrolch.ui.FilterWrangler

class DNSStrolch : Application() {
    companion object {
        lateinit var filterWrangler: FilterWrangler
    }

    override fun onCreate() {
        super.onCreate()
        filterWrangler = FilterWrangler(applicationContext)
        TunnelVision.innitTunnelLib(filterWrangler, applicationContext)
        TunnelVision.setupTunnel(applicationContext)
    }
}