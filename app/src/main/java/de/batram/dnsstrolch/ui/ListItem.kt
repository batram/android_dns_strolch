package de.batram.dnsstrolch.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.samsao.swipeablelstitem.views.SwipeableView
import de.batram.dnsstrolch.EditFilter
import de.batram.dnsstrolch.R
import de.batram.dnsstrolch.DNSStrolch

const val FILTER_DINGENS = "de.batram.dnsstrolch.ui.FILTER_DINGENS"


class ListItem(context: Context?) : SwipeableView(context) {
    private lateinit var filterDomain: TextView
    private lateinit var filterHits: TextView
    private lateinit var filterIcon: ImageView
    private lateinit var secondaryFilterIcon: ImageView
    private lateinit var mLayout: View

    public override fun getContent(parent: ViewGroup): View {
        mLayout = LayoutInflater.from(context).inflate(R.layout.list_item, this, false)
        filterDomain = mLayout.findViewById(R.id.itemDomain)
        return mLayout
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setup(filter: Filter) {
        filterDomain = mLayout.findViewById(R.id.itemDomain)
        filterDomain.text = filter.domain

        filterDomain.setOnClickListener {
            val myIntent = Intent(mLayout.context, EditFilter::class.java).apply {
                putExtra(FILTER_DINGENS, filter)
            }
            mLayout.context.startActivity(myIntent)
        }

        filterDomain.setOnLongClickListener{ v ->
            DNSStrolch.filterWrangler.releaseTouchyWait();

            AlertDialog.Builder(v.context)
                .setTitle("Remove Filter")
                .setMessage("Remove the filter [${filter.domain}]?")
                .setPositiveButton(
                    "Yes",
                    DialogInterface.OnClickListener { _dialog, _whichButton ->
                        DNSStrolch.filterWrangler.remove(filter)
                        Toast.makeText(
                            v.context,
                            "Filter removed",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                .setNegativeButton("No", null).show()

            return@setOnLongClickListener true;
        };

        filterDomain.setOnTouchListener{ v, event ->
            if(event.action == MotionEvent.ACTION_UP){
                if(event.eventTime - event.downTime > 30 && DNSStrolch.filterWrangler.waitOnTouch){
                    DNSStrolch.filterWrangler.releaseTouchyWait()
                    v.performClick()
                    return@setOnTouchListener true;
                }
                DNSStrolch.filterWrangler.releaseTouchyWait()

            } else if(event.action == MotionEvent.ACTION_DOWN) {
                filterDomain.background.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context, R.color.colorAccent), PorterDuff.Mode.OVERLAY)
                DNSStrolch.filterWrangler.waitOnTouch = true;
                DNSStrolch.filterWrangler.touchedFilter = filterDomain;
            }
            return@setOnTouchListener false;
        }

        filterHits = mLayout.findViewById(R.id.filterHits)
        filterHits.text = filter.hits.toString()

        filterIcon = mLayout.findViewById(R.id.filterStatusIcon)
        filterIcon.setImageResource(filter.getPrimaryIcon())

        secondaryFilterIcon = mLayout.findViewById(R.id.filterSecondaryStatus)
        secondaryFilterIcon.setImageResource(filter.getSecondaryIcon())

        if(filter.getSecondaryIcon() != R.drawable.snowflake){
            secondaryFilterIcon.alpha = 0.9F
        } else {
            secondaryFilterIcon.alpha = 0.05F
        }
    }

    init {
        setRightSwipeText("allow")
        setRightSwipeTextColor(ContextCompat.getColor(getContext(), android.R.color.white))
        setRightSwipeBackground(ContextCompat.getDrawable(getContext(), android.R.color.holo_green_light))

        setLeftSwipeText("ignore")
        setLeftSwipeTextColor(ContextCompat.getColor(getContext(), android.R.color.white))
        setLeftSwipeBackground(ContextCompat.getDrawable(getContext(), android.R.color.holo_red_light))
    }
}