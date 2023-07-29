package net.weavemc.weave.api.mapping

import net.weavemc.weave.api.GameInfo

class SeargeMapper(gameVersion: GameInfo.Version) : IMapper {
    private val notchSearge by lazy { XSrgReader("/weave/mapping/${gameVersion.versionName}-srg.xsrg") }
    private val mcpNotch by lazy { XSrgReader("/weave/mapping/${gameVersion.versionName}-mcp.xsrg") }

    // First = Notch, Second = Searge
    private val notchSeargeMethods = notchSearge.methodMappings
    private val notchSeargeFields = notchSearge.fieldMappings

    // First = Notch, Second = MCP
    private val notchMcpClasses = mcpNotch.classMappings
    private val notchMcpMethods = mcpNotch.methodMappings
    private val notchMcpFields = mcpNotch.fieldMappings

    // First = Searge, Second = MCP
    private val seargeMcpMethods = notchMcpMethods.map { methodMapping ->
        XSrgReader.MethodMapping(
            notchSeargeMethods.find { it.firstOwner == methodMapping.firstOwner && it.firstName == methodMapping.firstName && it.firstDescriptor == methodMapping.firstDescriptor }?.secondOwner ?: methodMapping.firstOwner,
            notchSeargeMethods.find { it.firstOwner == methodMapping.firstOwner && it.firstName == methodMapping.firstName && it.firstDescriptor == methodMapping.firstDescriptor }?.secondName ?: methodMapping.firstName,
            notchSeargeMethods.find { it.firstOwner == methodMapping.firstOwner && it.firstName == methodMapping.firstName && it.firstDescriptor == methodMapping.firstDescriptor }?.secondDescriptor ?: methodMapping.firstDescriptor,
            methodMapping.secondOwner,
            methodMapping.secondName,
            methodMapping.secondDescriptor
        )
    }
    private val seargeMcpFields = notchMcpFields.map { fieldMapping ->
        XSrgReader.FieldMapping(
            notchSeargeFields.find { it.firstOwner == fieldMapping.firstOwner && it.firstName == fieldMapping.firstName }?.secondOwner ?: fieldMapping.firstOwner,
            notchSeargeFields.find { it.firstOwner == fieldMapping.firstOwner && it.firstName == fieldMapping.firstName }?.secondName ?: fieldMapping.firstName,
            fieldMapping.secondOwner,
            fieldMapping.secondName
        )
    }

    override fun mapClass(name: String): String? = notchMcpClasses.find { it.secondName == name }?.firstName

    override fun mapMethod(owner: String, name: String, descriptor: String): MappedMethod? {
        val method = seargeMcpMethods.find { it.secondOwner == owner && it.secondName == name && it.secondDescriptor == descriptor } ?: return null
        return MappedMethod(
            method.firstOwner,
            method.firstName,
            method.firstDescriptor
        )
    }

    override fun mapField(owner: String, name: String): MappedField? {
        val field = seargeMcpFields.find { it.secondOwner == owner && it.secondName == name } ?: return null
        return MappedField(
            field.firstOwner,
            field.firstName
        )
    }

    override fun reverseMapClass(name: String): String? = notchMcpClasses.find { it.firstName == name }?.secondName

    override fun reverseMapMethod(owner: String, name: String, descriptor: String): MappedMethod? {
        val method = seargeMcpMethods.find { it.firstOwner == owner && it.firstName == name && it.firstDescriptor == descriptor } ?: return null
        return MappedMethod(
            method.secondOwner,
            method.secondName,
            method.secondDescriptor
        )
    }

    override fun reverseMapField(owner: String, name: String): MappedField? {
        val field = seargeMcpFields.find { it.firstOwner == owner && it.firstName == name } ?: return null
        return MappedField(
            field.secondOwner,
            field.secondName
        )
    }
}
