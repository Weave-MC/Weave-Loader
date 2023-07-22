package net.weavemc.weave.api.mapping.client

import net.weavemc.weave.api.mapping.IMapper
import org.objectweb.asm.commons.Remapper

open class RemapperWrapper(private val mapper: IMapper) : Remapper() {
    override fun map(internalName: String?): String? = mapper.mapClass(internalName) ?: internalName

    override fun mapMethodName(owner: String?, name: String?, descriptor: String?): String? = mapper.mapMethod(owner, name) ?: name

    override fun mapFieldName(owner: String?, name: String?, descriptor: String?): String? = mapper.mapField(owner, name) ?: name

    fun getMapperName(): String = mapper::class.simpleName!!
}
