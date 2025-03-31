package net.weavemc.loader.mappings

import org.objectweb.asm.commons.Remapper
import java.io.DataInputStream

internal class KinRemapper(resource: String) : Remapper() {
    private val classes = mutableMapOf<String, String>()
    private val classesReverse = mutableMapOf<String, String>() // TODO: use proper BiMap structure?
    private val methods = mutableMapOf<String, String>()
    private val fields = mutableMapOf<String, String>()

    init {
        try {
            (javaClass.getResourceAsStream(resource)?.let(::DataInputStream) ?: error("Could not find $resource"))
                .readKin()
        } catch (e: Exception) {
            println("[Weave] An error occurred while trying to read mappings, defaulting to NONE!")
            e.printStackTrace()
        }
    }

    private fun DataInputStream.readKin() {
        check(readInt() == 99151942) { "Invalid magic" }
        check(readByte() == 1.toByte()) { "Invalid version" }
        repeat(readInt() shl 1) { readUTF() }
        repeat(readInt()) { readClassMapping(readUTF(), readUTF()) }
    }

    private fun constructName(prefix: String, suffix: String) =
        if (prefix.isEmpty()) suffix else "$prefix\$$suffix"

    private fun DataInputStream.readClassMapping(obfedName: String, deobfedName: String) {
        classes[obfedName] = deobfedName
        classesReverse[deobfedName] = obfedName

        repeat(readInt()) {
            readClassMapping(constructName(obfedName, readUTF()), constructName(deobfedName, readUTF()))
        }

        repeat(readInt()) {
            val obfedFieldName = readUTF()
            readUTF() // desc, ignored
            fields["$obfedName.$obfedFieldName"] = readUTF()
        }

        repeat(readInt()) {
            methods["$obfedName.${readUTF()}${readUTF()}"] = readUTF()
        }
    }

    fun mapReverse(internalName: String) = classesReverse[internalName] ?: internalName

    override fun map(internalName: String) =
        classes.getOrDefault(internalName, internalName)

    override fun mapMethodName(owner: String, name: String, descriptor: String) =
        methods.getOrDefault("$owner.$name$descriptor", name)

    override fun mapFieldName(owner: String, name: String, descriptor: String) =
        fields.getOrDefault("$owner.$name", name)
}
