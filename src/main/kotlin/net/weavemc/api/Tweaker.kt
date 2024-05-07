package net.weavemc.api

import java.lang.instrument.Instrumentation

interface Tweaker {
    fun tweak(inst: Instrumentation)
}