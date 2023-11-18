package net.weavemc.weave.api.mixin

import kotlin.reflect.KClass

annotation class Constant(
    val id: String = "",
    val constantType: ConstantType = ConstantType.NULL,
    val valueInt: Int = 0,
    val valueFloat: Float = 0.0f,
    val valueLong: Long = 0L,
    val valueDouble: Double = 0.0,
    val valueString: String = "",
    val valueClass: KClass<*> = Any::class,
    val shift: Int = 0,
) {
    enum class ConstantType {
        NULL,
        INT,
        FLOAT,
        LONG,
        DOUBLE,
        STRING,
        CLASS,
    }
}
