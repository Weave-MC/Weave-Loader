package club.maxstats.weave.loader.util

import sun.misc.Unsafe
import java.lang.invoke.MethodHandles.Lookup
import java.lang.invoke.MethodType
import java.lang.reflect.Field
import java.net.URL
import java.net.URLClassLoader

val theUnsafe by lazy {
    Unsafe::class.java.getDeclaredField("theUnsafe").also { it.isAccessible = true }[null] as Unsafe
}

@Suppress("DEPRECATION")
fun Unsafe.getStaticField(field: Field): Any = getObject(staticFieldBase(field), staticFieldOffset(field))

val trustedLookup by lazy { theUnsafe.getStaticField(Lookup::class.java.getDeclaredField("IMPL_LOOKUP")) as Lookup }

private val addURLHandle by lazy {
    trustedLookup.findVirtual(URLClassLoader::class.java, "addURL", MethodType.methodType(Void.TYPE, URL::class.java))
}

fun URLClassLoader.addURL(url: URL) {
    addURLHandle.invokeExact(this, url)
}