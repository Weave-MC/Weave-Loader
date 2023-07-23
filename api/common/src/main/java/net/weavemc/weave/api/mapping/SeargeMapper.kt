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
            notchSeargeMethods.find { it.firstName == methodMapping.firstName }?.secondOwner ?: methodMapping.firstOwner,
            notchSeargeMethods.find { it.firstName == methodMapping.firstName }?.secondName ?: methodMapping.firstName,
            methodMapping.secondOwner,
            methodMapping.secondName
        )
    }
    private val seargeMcpFields = notchMcpFields.map { fieldMapping ->
        XSrgReader.FieldMapping(
            notchSeargeFields.find { it.firstName == fieldMapping.firstName }?.secondOwner ?: fieldMapping.firstOwner,
            notchSeargeFields.find { it.firstName == fieldMapping.firstName }?.secondName ?: fieldMapping.firstName,
            fieldMapping.secondOwner,
            fieldMapping.secondName
        )
    }

    override fun mapClass(name: String?): String? = notchMcpClasses.find { it.secondName == name }?.firstName

    override fun mapMethod(owner: String?, name: String?): String? = seargeMcpMethods.find { it.secondOwner == owner && it.secondName == name }?.firstName

    override fun mapField(owner: String?, name: String?): String? = seargeMcpFields.find { it.secondOwner == owner && it.secondName == name }?.firstName

    override fun reverseMapClass(name: String?): String? = notchMcpClasses.find { it.firstName == name }?.secondName

    override fun reverseMapMethod(owner: String?, name: String?): String? = seargeMcpMethods.find { it.firstOwner == owner && it.firstName == name }?.secondName

    override fun reverseMapField(owner: String?, name: String?): String? = seargeMcpFields.find { it.firstOwner == owner && it.firstName == name }?.secondName
}
