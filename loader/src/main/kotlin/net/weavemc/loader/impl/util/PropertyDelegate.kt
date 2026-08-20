@file:Suppress("UNCHECKED_CAST")

package net.weavemc.loader.impl.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty
import kotlin.time.Duration

public class SystemPropertyDelegate<T>(
    private val key: String,
    private val defaultValueProvider: () -> T,
    private val cached: Boolean = true,
    private val parser: ((String) -> T)? = null,
) {
    @Volatile
    private var cachedValue: Any? = UNINITIALISED

    public operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!cached) {
            return parseValue()
        }

        if (cachedValue === UNINITIALISED) {
            synchronized(this) {
                if (cachedValue === UNINITIALISED) {
                    cachedValue = parseValue()
                }
            }
        }

        return cachedValue as T
    }

    public operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        val systemProperties = System.getProperties()!!

        if (value == null) {
            systemProperties.remove(key)
        } else {
            systemProperties[key] = value
        }

        if (cached) {
            synchronized(this) {
                cachedValue = value ?: parseValue()
            }
        }
    }

    private fun parseValue(): T {
        val defaultValue by lazy { defaultValueProvider() }

        val value = System.getProperty(key) ?: return defaultValue

        return runCatching {
            if (parser != null) {
                parser(value)
            } else {
                when (defaultValue) {
                    is Boolean -> value.toBoolean() as T
                    is Int -> value.toInt() as T
                    is Long -> value.toLong() as T
                    is Double -> value.toDouble() as T
                    is Float -> value.toFloat() as T
                    is Duration -> Duration.parse(value) as T
                    is Path -> Paths.get(value) as T
                    is File -> File(value) as T
                    is String -> value as T
                    else -> value as T
                }
            }
        }.getOrDefault(defaultValue)
    }

    private companion object {
        private val UNINITIALISED = Any()
    }
}

public fun <T> systemProperty(
    key: String,
    defaultValue: T,
    cached: Boolean = true
): SystemPropertyDelegate<T> = SystemPropertyDelegate(key, { defaultValue }, cached)

public fun <T> systemProperty(
    key: String,
    defaultValueProvider: () -> T,
    cached: Boolean = true
): SystemPropertyDelegate<T> = SystemPropertyDelegate(key, defaultValueProvider, cached)

public fun <T> systemProperty(
    key: String,
    defaultValue: T,
    cached: Boolean = true,
    parser: (String) -> T
): SystemPropertyDelegate<T> = SystemPropertyDelegate(key, { defaultValue }, cached, parser)

public fun <T> systemProperty(
    key: String,
    defaultValueProvider: () -> T,
    cached: Boolean = true,
    parser: (String) -> T
): SystemPropertyDelegate<T> = SystemPropertyDelegate(key, defaultValueProvider, cached, parser)