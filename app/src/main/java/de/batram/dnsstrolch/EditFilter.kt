package de.batram.dnsstrolch

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import de.batram.dnsstrolch.ui.FILTER_DINGENS
import de.batram.dnsstrolch.ui.Filter

class EditFilter : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.filter_edit)


        val filter = intent.getSerializableExtra(FILTER_DINGENS) as Filter

        findViewById<TextView>(R.id.filterTitle).apply {
            text = filter.domain
        }

        val textInput = findViewById<TextInputEditText>(R.id.filterTextEdit).apply {
            setText(filter.domain)
        }

        val hardcodedIPTextInput = findViewById<TextInputEditText>(R.id.hardcodedIPTextEdit).apply {
            if(filter.type == Filter.Type.Hardcoded)
                setText(filter.hardcodedIP)
        }

        val radioGroup = findViewById(R.id.filterTypeSelection) as RadioGroup

        var typeId = when (filter.type) {
            Filter.Type.Allowed -> R.id.filterTypeAllowed
            Filter.Type.StarAllowed -> R.id.filterTypeAllowed
            Filter.Type.Hardcoded -> R.id.filterTypeHardcoded
            Filter.Type.TMPList -> R.id.filterTypeTMP
            Filter.Type.Ignored -> R.id.filterTypeIgnored
            Filter.Type.Hashed -> R.id.filterTypeHashed
            else -> -1
        }

        if(typeId != -1){
            radioGroup.check(typeId)
        }

        val stateToggle: ToggleButton = findViewById<View>(R.id.filterStateToggle) as ToggleButton
        stateToggle.isChecked = filter.enabled

        val saveButton: Button = findViewById<View>(R.id.filterSaveButton) as Button

        saveButton.setOnClickListener { view ->
            var newFilter = Filter(textInput.text.toString(), Filter.Type.Unknown);

            var type = when (radioGroup.getCheckedRadioButtonId()) {
                R.id.filterTypeAllowed -> Filter.Type.Allowed
                R.id.filterTypeHardcoded -> {
                    newFilter.hardcodedIP = hardcodedIPTextInput.text.toString()
                    Filter.Type.Hardcoded
                }
                R.id.filterTypeTMP -> Filter.Type.Allowed //TODO: how do we handle tmp types?
                R.id.filterTypeIgnored -> Filter.Type.Ignored
                R.id.filterTypeHashed -> Filter.Type.Hashed
                else -> Filter.Type.Unknown
            }
            newFilter.type = type
            newFilter.hits = filter.hits
            newFilter.enabled = stateToggle.isChecked

            DNSStrolch.filterWrangler.update(filter, newFilter)
            this.finish()
        }

        val removeButton: Button = findViewById<View>(R.id.filterRemoveButton) as Button

        removeButton.setOnClickListener { v ->
            AlertDialog.Builder(v.context)
                .setTitle("Remove Filter")
                .setMessage("Remove the filter [${filter.domain}]?")
//                .setIcon(R.drawable.ic_dialog_alert)
                .setPositiveButton(
                    "Yes",
                    DialogInterface.OnClickListener { _dialog, _whichButton ->
                        DNSStrolch.filterWrangler.remove(filter)
                        this.finish()
                        Toast.makeText(
                            v.context,
                            "Filter removed",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                .setNegativeButton("No", null).show()
        }

        val hashButton: Button = findViewById<View>(R.id.filterHashButton) as Button

        hashButton.setOnClickListener { v ->
            val hashed = TunnelVision.hash_domain(filter.domain)
            Toast.makeText(
                v.context,
                "Filter hashed:" + hashed,
                Toast.LENGTH_SHORT
            ).show()
            radioGroup.check(R.id.filterTypeHashed)
            textInput.setText(hashed)
        }


        stateToggle.setOnClickListener { view ->
            filter.enabled = filter.enabled.not()
        }

    }
}