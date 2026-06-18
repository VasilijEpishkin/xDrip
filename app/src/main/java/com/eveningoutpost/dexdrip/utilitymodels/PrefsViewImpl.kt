package com.eveningoutpost.dexdrip.utilitymodels

import com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify

class PrefsViewImpl : ObservableArrayMapNoNotify<String, Boolean>(), PrefsView {

    override val size: Int get() = super.size
    override val keys: MutableSet<String> get() = super.keys
    override val values: MutableCollection<Boolean> get() = super.values
    override val entries: MutableSet<MutableMap.MutableEntry<String, Boolean>> get() = super.entries

    private var runnable: Runnable? = null

    override fun getbool(name: String?): Boolean {
        if (name == null) return false
        return PrefHandle.parse(name)?.getBoolean() ?: false
    }

    override fun setbool(name: String, value: Boolean) {
        val handle = PrefHandle.parse(name) ?: return
        Pref.setBoolean(handle.key, value)
        super.put(handle.key, value)
        doRunnable()
    }

    override fun togglebool(name: String) = setbool(name, !getbool(name))

    fun setRefresh(runnable: Runnable): PrefsViewImpl {
        this.runnable = runnable
        return this
    }

    private fun doRunnable() = runnable?.run()

    override fun get(key: String?): Boolean? {
        if (key == null) return false
        val handle = PrefHandle.parse(key) ?: return false
        var value = super.get(handle.key)
        if (value == null) {
            value = getbool(key)
            super.putNoNotify(handle.key, value)
        }
        return value
    }

    override fun put(key: String, value: Boolean): Boolean? {
        val handle = PrefHandle.parse(key) ?: return value
        if (super.get(handle.key) != value) {
            Pref.setBoolean(handle.key, value)
            super.put(handle.key, value)
            doRunnable()
        }
        return value
    }

    @JvmName("putBoolByObject")
    fun put(key: Any, value: Boolean) {
        val handle = PrefHandle.parse(key as String) ?: return
        if (super.get(handle.key) != value) {
            super.put(handle.key, value)
        }
    }

    class PrefHandle(val key: String, val defaultValue: Boolean) {

        fun getBoolean(): Boolean = Pref.getBoolean(key, defaultValue)

        companion object {
            @JvmStatic
            fun parse(identifier: String?): PrefHandle? {
                if (identifier == null) return null
                val parts = identifier.split(":", limit = 2)
                return if (parts.size == 2) {
                    PrefHandle(parts[0], parts[1].toBoolean())
                } else {
                    PrefHandle(identifier, false)
                }
            }
        }
    }
}
