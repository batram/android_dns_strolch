package de.batram.dnsstrolch.ui.ignored

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samsao.swipeablelstitem.listeners.SwipeableListOnItemTouchListener
import de.batram.dnsstrolch.R
import de.batram.dnsstrolch.DNSStrolch
import de.batram.dnsstrolch.ui.ListAdapter

class IgnoredFragment : Fragment() {

    private lateinit var ignoredViewModel: IgnoredViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ignoredViewModel =
            ViewModelProviders.of(this).get(IgnoredViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_ignored, container, false)

        val recyclerView: RecyclerView = root.findViewById(R.id.allowed_filters)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = context?.let {
                ListAdapter(it, DNSStrolch.filterWrangler.ignoreList,
                    { filter ->
                        Log.v("left ", filter.domain)
                        DNSStrolch.filterWrangler.allow(filter)
                        true
                    }, { filter ->
                        Log.v("right ", filter.domain)
                        false
                    }
                )
            }

            addOnItemTouchListener(SwipeableListOnItemTouchListener())

            setOnTouchListener { _, event ->
                if(event.action == MotionEvent.ACTION_UP){
                    DNSStrolch.filterWrangler.releaseTouchyWait();
                }
                false
            }
        }
        DNSStrolch.filterWrangler.freshListDisplays.add(recyclerView.adapter as ListAdapter)

        return root
    }
}
