package net.weavemc.loader.mappings

import org.objectweb.asm.commons.Remapper

internal class XSrgMapper(resource: String) : Remapper() {
    private val classes = mutableMapOf<String, String>()
    private val methods = mutableMapOf<String, String>()
    private val fields = mutableMapOf<String, String>()

    init {
        this.javaClass.getResourceAsStream(resource)!!
            .bufferedReader()
            .useLines {
                it.filter { it.isNotEmpty() }.forEach { line ->
                    val split = line.drop(4).split(' ')
                    when(line.take(4)) {
                        "CL: " -> classes[split[0]] = split[1]
                        "MD: " -> {
                            val clazz = split[0].substringBeforeLast('/')
                            val method = split[0].substringAfterLast('/')
                            methods["$clazz.$method${split[1]}"] = split[2].substringAfterLast('/')
                        }
                        "FD: " -> {
                            val clazz = split[0].substringBeforeLast('/')
                            val field = split[0].substringAfterLast('/')
                            fields["$clazz.$field"] = split[2].substringAfterLast('/')
                        }
                    }
                }
            }
    }

    fun mapReverse(internalName: String) =
        classes.entries.find { it.value == internalName }?.key ?: internalName

    override fun map(internalName: String) =
        classes.getOrDefault(internalName, internalName)

    override fun mapMethodName(owner: String, name: String, descriptor: String) =
        methods.getOrDefault("$owner.$name$descriptor", name)

    override fun mapFieldName(owner: String, name: String, descriptor: String) =
        fields.getOrDefault("$owner.$name", name)
}
