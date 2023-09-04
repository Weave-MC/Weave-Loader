package net.weavemc.weave.api.mapping

import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.Remapper

class MappingsRemapper(
    private val mappings: Mappings,
    private val from: String,
    private val to: String,
    private val shouldRemapDesc: Boolean = mappings.namespaces.indexOf(from) != 0,
    private val loader: (name: String) -> ByteArray?
) : Remapper() {
    val mappingsType: String = mappings.javaClass.simpleName
    private val map = mappings.asASMMapping(from, to)
    private val baseMapper by lazy {
        MappingsRemapper(mappings, from, mappings.namespaces[0], shouldRemapDesc = false, loader)
    }

    override fun map(internalName: String): String = map[internalName] ?: internalName
//    override fun mapInnerClassName(name: String, ownerName: String?, innerName: String?) = map(name)

    override fun mapMethodName(owner: String, name: String, desc: String): String {
        if (name == "<init>" || name == "<clinit>") return name

        // Source: https://github.com/FabricMC/tiny-remapper/blob/d14e8f99800e7f6f222f820bed04732deccf5109/src/main/java/net/fabricmc/tinyremapper/AsmRemapper.java#L74
        return if (desc.startsWith("(")) {
            val actualDesc = if (shouldRemapDesc) baseMapper.mapMethodDesc(desc) else desc
            walk(owner, name) { map["$it.$name$actualDesc"] }
        } else mapFieldName(owner, name, desc)
    }

    override fun mapFieldName(owner: String, name: String, desc: String?) = walk(owner, name) { map["$it.$name"] }
    override fun mapRecordComponentName(owner: String, name: String, desc: String) =
        mapFieldName(owner, name, desc)

    private inline fun walk(
        owner: String,
        name: String,
        applicator: (owner: String) -> String?
    ): String {
        val queue = ArrayDeque<String>()
        val seen = hashSetOf<String>()
        queue.addLast(owner)

        while (queue.isNotEmpty()) {
            val curr = queue.removeLast()
            val new = applicator(curr)
            if (new != null) return new

            val bytes = loader(curr) ?: continue
            val reader = ClassReader(bytes)

            reader.superName?.let { if (seen.add(it)) queue.addLast(it) }
            queue += reader.interfaces.filter { seen.add(it) }
        }

        return name
    }

    fun reverse(loader: (name: String) -> ByteArray? = this.loader) =
        MappingsRemapper(mappings, to, from, loader = loader)
}
