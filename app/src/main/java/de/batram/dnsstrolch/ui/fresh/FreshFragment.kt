package de.batram.dnsstrolch.ui.fresh

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

class FreshFragment : Fragment() {

    private lateinit var freshViewModel: FreshViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        freshViewModel =
            ViewModelProviders.of(this).get(FreshViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_fresh, container, false)

        //TODO: Pause updates on swipe and scroll
        val recyclerView: RecyclerView = root.findViewById(R.id.allowed_filters)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(context)
            adapter = context?.let {
                ListAdapter(it, DNSStrolch.filterWrangler.freshList,
                    { filter ->
                        Log.v("left ", filter.domain)
                        DNSStrolch.filterWrangler.allow(filter)
                        false
                    }, { filter ->
                        Log.v("right ", filter.domain)
                        DNSStrolch.filterWrangler.ignore(filter)
                        false
                    }
                )
            }
            addOnItemTouchListener(SwipeableListOnItemTouchListener())

            setOnTouchListener { _, event ->
                if(event.action == MotionEvent.ACTION_UP){
                    DNSStrolch.filterWrangler.releaseTouchyWait()
                }
                false
            }

        }
        DNSStrolch.filterWrangler.activity = activity;
        DNSStrolch.filterWrangler.freshListDisplays.add(recyclerView.adapter as ListAdapter)

        return root
    }
}
