package de.batram.dnsstrolch

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log.v


class VpnSchrubler {
    class VpnService : android.net.VpnService() {
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            v("tunnelvision", "start command")
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onCreate() {
            v("tunnelvision", "onCreate")
            super.onCreate()
        }

        override fun onDestroy() {
            v("tunnelvision", "onDestroy")
            TunnelVision.updateState(TunnelVision.State.Disconnected)
            super.onDestroy()
        }

        fun build(): Builder {
            var builder = Builder().setSession("DNSStrolch TunnelVision")
                .addRoute("10.11.12.13", 32)
                .addAddress("10.11.12.15", 32)
                .addDnsServer("10.11.12.13")
                .allowFamily(OsConstants.AF_INET)
                .setBlocking(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            return builder;
            //.allowFamily(OsConstants.AF_INET6)
        }
    }

    fun buttonMeUp(context: Context) {
        val intent = android.net.VpnService.prepare(context)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        when (intent) {
            null -> v("tunnelvision", "got nothing ...")
            else -> context.startActivity(intent)
        }
    }

    fun open(context: Context): ParcelFileDescriptor? {
        val builder = VpnService().build()

        val configureIntent = Intent(context, MainActivity::class.java)
        configureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        builder.setConfigureIntent(PendingIntent.getActivity(context, 0, configureIntent, 0))

        return builder.establish()
    }

}