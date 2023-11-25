package net.weavemc.api.mixin

class Args(vararg args: Any?) {
    private val args: Array<Any?>

    init {
        this.args = arrayOf(*args)
    }

    fun get(index: Int): Any? {
        return args[index]
    }

    fun set(index: Int, value: Any?) {
        args[index] = value
    }
}