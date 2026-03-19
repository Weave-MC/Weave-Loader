@file:Suppress("UNCHECKED_CAST")

package net.weavemc.loader.impl.util

import kotlin.reflect.KProperty

public class SystemPropertyDelegate<T>(
    private val key: String,
    private val defaultValue: T = null as T
) {
    public operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val value = System.getProperty(key) ?: return defaultValue

        return when (defaultValue) {
            is Boolean -> value.toBoolean() as T
            is Int -> value.toInt() as T
            else -> value as T
        }
    }
}

public fun <T> systemProperty(key: String, defaultValue: T = null as T): SystemPropertyDelegate<T> =
    SystemPropertyDelegate(key, defaultValue)