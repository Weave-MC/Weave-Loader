package net.weavemc.weave.api.mapping

class McpMapper : IMapper {
    override fun mapUniversal(name: String?): String? = name

    override fun mapClass(name: String?): String? = name

    override fun mapMethod(owner: String?, name: String?): String? = name

    override fun mapField(owner: String?, name: String?): String? = name

    override fun reverseMapUniversal(name: String?): String? = name

    override fun reverseMapClass(name: String?): String? = name

    override fun reverseMapMethod(owner: String?, name: String?): String? = name

    override fun reverseMapField(owner: String?, name: String?): String? = name
}
