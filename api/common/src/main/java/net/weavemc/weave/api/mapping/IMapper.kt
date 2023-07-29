package net.weavemc.weave.api.mapping

interface IMapper {

    fun mapClass(name: String): String?
    fun mapMethod(owner: String, name: String, descriptor: String): MappedMethod?
    fun mapField(owner: String, name: String): MappedField?
    fun reverseMapClass(name: String): String?
    fun reverseMapMethod(owner: String, name: String, descriptor: String): MappedMethod?
    fun reverseMapField(owner: String, name: String): MappedField?
}

data class MappedMethod(
    val owner: String,
    val name: String,
    val descriptor: String
)
data class MappedField(
    val owner: String,
    val name: String
)
