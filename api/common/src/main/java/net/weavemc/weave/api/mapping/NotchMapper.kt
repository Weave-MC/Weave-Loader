package net.weavemc.weave.api.mapping

import net.weavemc.weave.api.GameInfo

class NotchMapper(gameVersion: GameInfo.Version) : IMapper {
    private val xSrgReader by lazy { XSrgReader("/weave/mapping/$gameVersion") }

    private val classes = xSrgReader.classMappings
    private val methods = xSrgReader.methodMappings
    private val fields = xSrgReader.fieldMappings

    override fun mapClass(name: String?): String? = classes.find { it.mcpName == name }?.notchName

    override fun mapMethod(owner: String?, name: String?): String? = methods.find { it.mcpOwner == owner && it.mcpName == name }?.notchName

    override fun mapField(owner: String?, name: String?): String? = fields.find { it.mcpOwner == owner && it.mcpName == name }?.notchName

    override fun reverseMapClass(name: String?): String? = classes.find { it.notchName == name }?.mcpName

    override fun reverseMapMethod(owner: String?, name: String?): String? = methods.find { it.notchOwner == owner && it.notchName == name }?.mcpName

    override fun reverseMapField(owner: String?, name: String?): String? = fields.find { it.notchOwner == owner && it.notchName == name }?.mcpName
}
