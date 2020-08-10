package de.batram.dnsstrolch.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import de.batram.dnsstrolch.R
import de.batram.dnsstrolch.TunnelVision

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View?  {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        var sharedPref = context?.getSharedPreferences("settings", Context.MODE_PRIVATE);

        val dohServerName = sharedPref?.getString("dohServerName", "cloudflare-dns.com").toString()
        val dohServerIp = sharedPref?.getString("dohServerIp", "104.16.249.249").toString()
        val dohServerQuery = sharedPref?.getString("dohServerQuery", "/dns-query?dns=").toString()

        val dohServerIpInput = root.findViewById<TextInputEditText>(R.id.settingsDohServerIP).apply {
            setText(dohServerIp)
        }

        val dohServerNameInput = root.findViewById<TextInputEditText>(R.id.settingsDohServerName).apply {
            setText(dohServerName)
        }

        val dohServerQueryInput = root.findViewById<TextInputEditText>(R.id.settingsDohServerQuery).apply {
            setText(dohServerQuery)
        }

        val settingsSaveButton: Button = root.findViewById<View>(R.id.settingsSaveButton) as Button
        settingsSaveButton.setOnClickListener { view ->
            val dohServerIp = dohServerIpInput.text.toString();
            val dohServername = dohServerNameInput.text.toString()
            val dohServerQuery = dohServerQueryInput.text.toString()
            sharedPref?.edit()?.putString("dohServerIp", dohServerIpInput.text.toString())?.commit();
            sharedPref?.edit()?.putString("dohServerName", dohServerNameInput.text.toString())?.commit();
            sharedPref?.edit()?.putString("dohServerQuery", dohServerQueryInput.text.toString())?.commit();

            TunnelVision.update_settings(dohServername, dohServerIp, dohServerQuery)
        }

        return root
    }

}