package net.weavemc.weave.api.mapping

import net.weavemc.weave.api.GameInfo

class NotchMapper(gameVersion: GameInfo.Version) : IMapper {
    private val xSrgReader by lazy { XSrgReader("/weave/mapping/${gameVersion.versionName}-mcp.xsrg") }

    // First = Notch, Second = MCP
    private val classes = xSrgReader.classMappings
    private val methods = xSrgReader.methodMappings
    private val fields = xSrgReader.fieldMappings

    override fun mapClass(name: String): String? = classes.find { it.secondName == name }?.firstName

    override fun mapMethod(owner: String, name: String, descriptor: String): MappedMethod? {
        val method = methods.find { it.secondOwner == owner && it.secondName == name && it.secondDescriptor == descriptor } ?: return null
        return MappedMethod(
            method.firstOwner,
            method.firstName,
            method.firstDescriptor
        )
    }

    override fun mapField(owner: String, name: String): MappedField? {
        val field = fields.find { it.secondOwner == owner && it.secondName == name } ?: return null
        return MappedField(
            field.firstOwner,
            field.firstName
        )
    }

    override fun reverseMapClass(name: String): String? = classes.find { it.firstName == name }?.secondName

    override fun reverseMapMethod(owner: String, name: String, descriptor: String): MappedMethod? {
        val method = methods.find { it.firstOwner == owner && it.firstName == name } ?: return null
        return MappedMethod(
            method.secondOwner,
            method.secondName,
            method.secondDescriptor
        )
    }

    override fun reverseMapField(owner: String, name: String): MappedField? {
        val field = fields.find { it.firstOwner == owner && it.firstName == name } ?: return null
        return MappedField(
            field.secondOwner,
            field.secondName
        )
    }
}
