package de.batram.dnsstrolch.ui

import de.batram.dnsstrolch.R
import java.io.Serializable

class Filter(domain: String, type: Type) : Serializable {
    var domain: String = domain
    var type: Type = type
    var hits: Int = 0
    var lastHit = System.currentTimeMillis()
    var enabled: Boolean = true;
    lateinit var hardcodedIP : String;

    enum class Type {
        Allowed,
        StarAllowed,
        Hardcoded,
        Unknown,
        TMPList,
        Ignored,
        Hashed
    }

    companion object {
        const val IGNORE_PREFIX = "!"
        const val HARDCODED_PREFIX = "="
        const val TMP_PREFIX = "§"
        const val HASH_PREFIX = "#"
    }

    fun isAllowed() : Boolean {
        return when (type) {
            Type.Hardcoded,
            Type.StarAllowed,
            Type.Allowed,
            Type.TMPList,
            Type.Hashed -> true
            else -> false
        }
    }

    fun hit(){
        lastHit = System.currentTimeMillis()
        hits += 1;
    }

    public override fun equals(other: Any?): Boolean {
        if (other !is Filter)
            return false
        return domain == other.domain
    }

    fun storeName() : String{
        return when (type) {
            Type.Hardcoded -> {
                HARDCODED_PREFIX + "$domain $hardcodedIP"
            }
            Type.Allowed -> domain
            Type.TMPList -> TMP_PREFIX + domain
            Type.Ignored -> IGNORE_PREFIX + domain
            Type.Unknown -> throw Exception("Can not store UNKOWN!")
            else -> {
                domain
            }
        }
    }

    fun getPrimaryIcon(): Int {
        return when (type) {
            Type.Allowed, Type.StarAllowed -> R.drawable.ic_fingerprint_black_24dp
            Type.Ignored -> R.drawable.ic_security_black_24dp
            Type.Unknown -> R.drawable.ic_blocked_no_filter_24dp
            else -> {
                R.drawable.ic_fingerprint_black_24dp
            }
        }
    }

    fun getSecondaryIcon(): Int {
        return when (type) {
            Type.Hashed -> R.drawable.ic_grid_on_black_24dp
            Type.TMPList -> R.drawable.ic_timer_black_24dp
            Type.Hardcoded -> R.drawable.ic_swap_calls_black_24dp
            else -> {
                R.drawable.snowflake
            }
        }
    }

    fun clone() : Filter {
        var newFilter = Filter(this.domain, this.type);
        newFilter.hits = this.hits
        newFilter.enabled = this.enabled
        return newFilter
    }
}