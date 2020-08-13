package de.batram.dnsstrolch.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import de.batram.dnsstrolch.TunnelVision

class FilterWrangler(applicationContext: Context) {
    var touchedFilter: TextView? = null;
    var activity: FragmentActivity? = null
    var freshListDisplays = mutableListOf<ListAdapter>();
    var ignoreList = mutableListOf<Filter>();
    var allowList = mutableListOf<Filter>();
    var freshList = mutableListOf<Filter>();
    var filterSet = mutableSetOf<String>();
    var context: Context = applicationContext;
    var sharedPref = context.getSharedPreferences("state", MODE_PRIVATE);
    var waitOnTouch: Boolean = false;

    enum class SortType {
        ALPHABET,
        RDOMAIN,
        TIME,
        HITCOUNT
    }

    init {
        initFilterList()
    }

    fun incomingDnsCallback(filter: Filter){
        var indexFirst = freshList.indexOfFirst { it == filter }
        if(indexFirst != -1){
            //TODO: add specific domain hits under wildcard filter (needs UI upgrade)
            var existingFilter = freshList[indexFirst]

            //Move hit filter to the top if below top 6
            if(indexFirst > 6){
                freshList.remove(existingFilter)
                freshList.add(0, existingFilter)
            }

            //TODO: Do we need a wonderful hit animation :D
            existingFilter.hit()
        } else {
            filter.hit()
            freshList.add(0, filter)
        }

        if(filter.isAllowed()){
            var indexAllow = allowList.indexOfFirst { it == filter }
            if(indexAllow != -1){
                allowList[indexAllow].hit()
            }
        } else if(filter.type == Filter.Type.Ignored){
            var indexIgnore = ignoreList.indexOfFirst { it == filter }
            if(indexIgnore != -1){
                ignoreList[indexIgnore].hit()
            }
        }

        notifyListeners()
    }

    fun ignore(filter: Filter) {
        var newFilter = filter.clone()
        newFilter.type = Filter.Type.Ignored
        update(filter, newFilter)
    }

    fun allow(filter: Filter) {
        var newFilter = filter.clone()
        newFilter.type = Filter.Type.Allowed
        update(filter, newFilter)
    }

    fun remove(filter: Filter) {
        allowList.remove(filter)
        ignoreList.remove(filter)
        TunnelVision.remove_filter(filter.storeName(), filter.type.ordinal)
        filterSet.remove(filter.storeName())

        //Removes decoration on fresh list
        filter.type = Filter.Type.Unknown;
        sharedPref.edit().putStringSet("filter_set", filterSet).commit();

        notifyListeners()
    }

    fun update(oldFilter: Filter, newFilter: Filter) {
        var indexFresh = freshList.indexOfFirst { it == oldFilter }
        if(indexFresh != -1) {
            freshList.removeAt(indexFresh)
            freshList.add(indexFresh, newFilter)
        }
        if(oldFilter.isAllowed()){
            var indexAllow = allowList.indexOfFirst { it == oldFilter }
            if(indexAllow != -1) {
                allowList.removeAt(indexAllow)
                TunnelVision.remove_filter(oldFilter.storeName(), oldFilter.type.ordinal)
                filterSet.remove(oldFilter.storeName())
            }
        } else if(oldFilter.type == Filter.Type.Ignored) {
            var indexIgnore = ignoreList.indexOfFirst { it == oldFilter }
            if(indexIgnore != -1) {
                ignoreList.removeAt(indexIgnore)
                TunnelVision.remove_filter(oldFilter.storeName(), oldFilter.type.ordinal)
                filterSet.remove(oldFilter.storeName())
            }
        }

        if(newFilter.isAllowed()){
            allowList.add(0, newFilter)
        } else if(newFilter.type == Filter.Type.Ignored){
            ignoreList.add(0, newFilter)
        }

        TunnelVision.add_filter(newFilter.storeName(), newFilter.type.ordinal)
        filterSet.add(newFilter.storeName())

        sharedPref.edit().putStringSet("filter_set", filterSet).commit();

        notifyListeners()
    }

    fun releaseTouchyWait(){
        if(waitOnTouch){
            touchedFilter?.background?.colorFilter = null
            waitOnTouch = false;
            activity?.runOnUiThread {
                freshListDisplays.forEach {
                    it.notifyDataSetChanged()
                }
            }
        }
    }

    private fun notifyListeners(){
        if(waitOnTouch){
            return;
        }

        activity?.runOnUiThread {
            freshListDisplays.forEach {
                it.notifyDataSetChanged()
            }
        }
    }

    private fun initFilterList() {
        sharedPref = context.getSharedPreferences("state", MODE_PRIVATE);
        //Try to clone the list, because you should not work directly on the returned instance
        filterSet = sharedPref.getStringSet("filter_set", getDefaultList())?.toMutableSet() ?: mutableSetOf()

        filterSet.forEach {
            if(it.startsWith(Filter.IGNORE_PREFIX)){
                ignoreList.add(Filter(it.removePrefix(Filter.IGNORE_PREFIX), Filter.Type.Ignored))
            } else if(it.startsWith(Filter.HARDCODED_PREFIX)){
                val split = it.removePrefix(Filter.HARDCODED_PREFIX).split(" ")
                var filter = Filter(split[0], Filter.Type.Hardcoded);
                filter.hardcodedIP = split[1];
                allowList.add(filter)
            }  else if(it.startsWith(Filter.HASH_PREFIX)){
                var filter = Filter(it, Filter.Type.Hashed);
                allowList.add(filter)
            } else {
                allowList.add(Filter(it, Filter.Type.Allowed));
            }
        }

        filterSet
    }

    private fun getDefaultList(): MutableSet<String> {
        var default_list = """
        """

        default_list = sharedPref.getString("filter_list", default_list).toString()

        var defaultStringList = mutableSetOf<String>();
        //TODO: Sort by domain name
        default_list.split("\n").map {
            if(it.trim().isNotEmpty()){
                defaultStringList.add(it.trim());
            }
        }

        return defaultStringList;
    }

    fun sortReverseDomain(){
        ignoreList.sortBy { it.domain.split('.').reversed().joinToString(separator=".") }
        allowList.sortBy { it.domain.split('.').reversed().joinToString(separator=".") }
        freshList.sortBy { it.domain.split('.').reversed().joinToString(separator=".") }
    }

    fun sortByTime(){
        ignoreList.sortByDescending { it.lastHit }
        allowList.sortByDescending { it.lastHit }
        freshList.sortByDescending { it.lastHit }
    }

    fun sortByHitCount() {
        ignoreList.sortByDescending { it.hits }
        allowList.sortByDescending { it.hits }
        freshList.sortByDescending { it.hits }
    }

    fun sortAlphabetically() {
        ignoreList.sortBy { it.domain }
        allowList.sortBy { it.domain }
        freshList.sortBy { it.domain }
    }

    fun sortLists(sType: SortType) {
        when(sType){
            SortType.RDOMAIN -> sortReverseDomain()
            SortType.TIME -> sortByTime()
            SortType.HITCOUNT -> sortByHitCount()
            SortType.ALPHABET -> sortAlphabetically()
        }

        notifyListeners()
    }
}
