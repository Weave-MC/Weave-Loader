package net.weavemc.testingmod

import net.weavemc.api.ModInitializer
import java.lang.instrument.Instrumentation

class TestingMod : ModInitializer {
    override fun preInit(inst: Instrumentation) {
        println("[TestingMod] Pre-init")
    }
}