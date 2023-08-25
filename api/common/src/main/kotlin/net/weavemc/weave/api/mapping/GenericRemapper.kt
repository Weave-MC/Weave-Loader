package net.weavemc.weave.api.mapping

class GenericRemapper : IMapper {
    override fun mapClass(name: String): String = name

    override fun mapMethod(owner: String, name: String, descriptor: String): MappedMethod = MappedMethod(owner, name, descriptor)

    override fun mapField(owner: String, name: String): MappedField = MappedField(owner, name)

    override fun reverseMapClass(name: String): String = name

    override fun reverseMapMethod(owner: String, name: String, descriptor: String): MappedMethod = MappedMethod(owner, name, descriptor)

    override fun reverseMapField(owner: String, name: String): MappedField = MappedField(owner, name)
}
