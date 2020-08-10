package de.batram.dnsstrolch

import android.app.Activity
import android.content.Context
import android.util.Log.e
import android.util.Log.v
import android.view.View
import android.widget.Button
import de.batram.dnsstrolch.ui.Filter
import de.batram.dnsstrolch.ui.FilterWrangler
import java.lang.Exception
import kotlin.concurrent.thread

class TunnelVision {
    enum class State {
        Setup,
        Connected,
        Disconnected,
        Failed,
    }


    companion object {
        external fun handoff_tunnel(size: Int) : Int
        external fun innit(filterList: String, dohServerName: String, dohServerIp: String, dohServerQuery: String)
        external fun update_settings(dohServerName: String, dohServerIp: String, dohServerQuery: String)
        external fun add_filter(filterList: String, type: Int)
        external fun remove_filter(filterList: String, type: Int)
        external fun hash_domain(domain: String) : String
        external fun close_tunnel()

        var state : State = State.Setup;
        var buttonLinked = false;
        lateinit var connectActivity : Activity;


        @JvmStatic fun block_callback(domain: String, ogFilter: String, type: Int) {
            v("bcall_back", domain + " " + ogFilter + " " + type)
            updateState(State.Connected)

            var filter = Filter(domain, Filter.Type.Unknown)

            filter.type = Filter.Type.values()[type];

            if(filter.type == Filter.Type.Hardcoded){
                var split = ogFilter.split(" ")
                if(split.size == 2){
                    filter.hardcodedIP = ogFilter.split(" ")[1]
                } else {
                    e("could not parse hardcoded filter", ogFilter)
                    throw Exception("could not parse hardcoded filter: $ogFilter");
                }
            }

            if(filter.type == Filter.Type.Hashed){
                filter.domain = ogFilter
            }

            DNSStrolch.filterWrangler.incomingDnsCallback(filter)
        }

        fun innitTunnelLib(filterWrangler: FilterWrangler, context: Context) {
            try {
                System.loadLibrary("tunnelvision")

                v("listing: ", filterWrangler.filterSet.joinToString(separator = "\n"))
                v("listing2: ", filterWrangler.filterSet.toString())

                val filtersAsString = filterWrangler.filterSet.joinToString(separator = "\n")
                var sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE);

                val dohServerName = sharedPref.getString("dohServerName", "cloudflare-dns.com").toString()
                val dohServerIp = sharedPref.getString("dohServerIp", "104.16.249.249").toString()
                val dohServerQuery = sharedPref.getString("dohServerQuery", "/dns-query?dns=").toString()

                innit(filtersAsString, dohServerName, dohServerIp, dohServerQuery);
            } catch (ex: Throwable) {
                e("tunnelvision load3", ex.toString())
                updateState(State.Failed)
            }
        }

        fun updateState(newState: State){
            if(state != newState){
                state = newState
                if (::connectActivity.isInitialized) {
                    connectActivity.runOnUiThread {
                        val connectButton: Button = connectActivity.findViewById<View>(R.id.connect_button) as Button
                        if (state == State.Connected || state == State.Setup) {
                            connectButton.text = "Disconnect"
                        } else {
                            connectButton.text = "Connect"
                        }
                    }
                }
            }
        }

        fun setupTunnel(context: Context) {
            val checkFile = VpnSchrubler().open(context)
            updateState(State.Setup)

            if (checkFile != null) {
                var vpnFile = checkFile.detachFd();
                v("tunnelvision got fd,", vpnFile.toString())

                val threadoo = thread {
                    if(handoff_tunnel(vpnFile) != 1){
                        updateState(State.Failed)
                    } else {
                        updateState(State.Disconnected)
                    }
                }

                if (threadoo.state == Thread.State.NEW) {
                    threadoo.start()
                }
            } else {
                v("tunnelvision", "noped no file descriptor")
                updateState(State.Failed)
            }
        }
    }
}

