package com.eveningoutpost.dexdrip.utilitymodels

import com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify

open class PrefsViewString : ObservableArrayMapNoNotify<String, String>() {

    override val size: Int get() = super.size
    override val keys: MutableSet<String> get() = super.keys
    override val values: MutableCollection<String> get() = super.values
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>> get() = super.entries

    open fun getString(name: String): String = Pref.getString(name, "")

    open fun setString(name: String, value: String) {
        Pref.setString(name, value)
        super.put(name, value)
    }

    override fun get(key: String?): String? {
        if (key == null) return null
        var value = super.get(key)
        if (value == null) {
            value = getString(key)
            super.putNoNotify(key, value)
        }
        val transformed = transformValue(key, value ?: "")
        if (transformed != value) {
            put(key, transformed)
        }
        return transformed
    }

    override fun put(key: String, value: String): String? {
        val current = super.get(key)
        if (current == null || current != value) {
            setString(key, value)
        }
        return value
    }

    /**
     * Hook for Java subclasses to snap/transform a loaded value without overriding get(),
     * which would conflict with Kotlin/Java Map bridge methods in collection:1.4.x.
     */
    protected open fun transformValue(key: String, value: String): String = value
}
