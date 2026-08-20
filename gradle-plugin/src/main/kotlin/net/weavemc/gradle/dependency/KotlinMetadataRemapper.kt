package net.weavemc.gradle.dependency

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature

internal class KotlinMetadataClassVisitor(
    parent: ClassVisitor,
    private val remapper: Remapper
) : ClassVisitor(Opcodes.ASM9, parent) {
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return if (descriptor == "Lkotlin/Metadata;") {
            object : AnnotationNode(Opcodes.ASM9, descriptor) {
                override fun visitEnd() {
                    super.visitEnd()

                    requireNotNull(values) { "Somehow Metadata's values are null" }
                    val map = values.chunked(2) { (k, v) -> k to v }.toMap()

                    @Suppress("UNCHECKED_CAST")
                    val metadata = Metadata(
                        kind = map["k"] as Int,
                        metadataVersion = (map["mv"] as List<Int>).toIntArray(),
                        data1 = (map["d1"] as? List<String>)?.toTypedArray(),
                        data2 = (map["d2"] as? List<String>)?.toTypedArray(),
                        extraString = map["xs"] as? String,
                        extraInt = map["xi"] as? Int,
                        packageName = map["pn"] as? String
                    )
                    val classMetadata = KotlinClassMetadata.readStrict(metadata)

                    remapKotlinMetadata(classMetadata, remapper)
                    applyMetadataToNode(classMetadata, this)

                    cv.visitAnnotation(desc, visible)?.let { parentAnnotationVisitor ->
                        accept(parentAnnotationVisitor)
                    }
                }
            }
        } else {
            super.visitAnnotation(descriptor, visible)
        }
    }
}

internal fun remapKotlinMetadata(metadata: KotlinClassMetadata, remapper: Remapper) {
    when (metadata) {
        is KotlinClassMetadata.Class -> {
            remapKmClass(metadata.kmClass, remapper)
        }

        is KotlinClassMetadata.FileFacade -> {
            remapKmPackage(metadata.kmPackage, remapper)
        }

        is KotlinClassMetadata.MultiFileClassPart -> {
            remapKmPackage(metadata.kmPackage, remapper)
            metadata.facadeClassName = remapper.map(metadata.facadeClassName)
        }

        is KotlinClassMetadata.MultiFileClassFacade -> {
            metadata.partClassNames = metadata.partClassNames.map { remapper.map(it) }
        }

        else -> {}
    }
}

internal fun applyMetadataToNode(metadata: KotlinClassMetadata, node: AnnotationNode) {
    val newMetadata = metadata.write()

    node.values = mutableListOf()

    fun addValue(key: String, value: Any?) {
        if (value != null) {
            node.values.add(key)
            node.values.add(value)
        }
    }

    addValue("k", newMetadata.kind)
    addValue("mv", newMetadata.metadataVersion)
    if (newMetadata.data1.isNotEmpty()) {
        addValue("d1", newMetadata.data1.toList())
    }
    if (newMetadata.data2.isNotEmpty()) {
        addValue("d2", newMetadata.data2.toList())
    }
    if (newMetadata.extraString.isNotEmpty()) {
        addValue("xs", newMetadata.extraString)
    }
    if (newMetadata.packageName.isNotEmpty()) {
        addValue("pn", newMetadata.packageName)
    }
    if (newMetadata.extraInt != 0) {
        addValue("xi", newMetadata.extraInt)
    }
}

private fun remapKmClass(kmClass: KmClass, remapper: Remapper) {
    kmClass.name = remapper.map(kmClass.name)

    kmClass.supertypes.forEach { remapKmType(it, remapper) }

    kmClass.nestedClasses.let {
        val remapped = it.map { name -> remapper.map(name) }
        it.clear()
        it.addAll(remapped)
    }
    kmClass.sealedSubclasses.let {
        val remapped = it.map { name -> remapper.map(name) }
        it.clear()
        it.addAll(remapped)
    }

    kmClass.functions.forEach { remapKmFunction(it, remapper) }
    kmClass.properties.forEach { remapKmProperty(it, remapper) }
    kmClass.constructors.forEach { remapKmConstructor(it, remapper) }
}

private fun remapKmPackage(kmPackage: KmPackage, remapper: Remapper) {
    kmPackage.functions.forEach { remapKmFunction(it, remapper) }
    kmPackage.properties.forEach { remapKmProperty(it, remapper) }
}

private fun remapKmFunction(function: KmFunction, remapper: Remapper) {
    function.valueParameters.forEach { remapKmType(it.type, remapper) }
    function.receiverParameterType?.let { remapKmType(it, remapper) }
    remapKmType(function.returnType, remapper)

    function.signature?.let {
        val newName = remapper.mapMethodName(function.name, it.name, it.descriptor)
        val newDesc = remapper.mapMethodDesc(it.descriptor)
        function.signature = JvmMethodSignature(newName, newDesc)
    }
}

private fun remapKmProperty(property: KmProperty, remapper: Remapper) {
    property.receiverParameterType?.let { remapKmType(it, remapper) }
    remapKmType(property.returnType, remapper)

    property.fieldSignature?.let {
        val newName = remapper.mapFieldName("", it.name, it.descriptor)
        val newDesc = remapper.mapDesc(it.descriptor)
        property.fieldSignature = JvmFieldSignature(newName, newDesc)
    }

    property.getterSignature?.let {
        property.getterSignature = JvmMethodSignature(
            remapper.mapMethodName("", it.name, it.descriptor),
            remapper.mapMethodDesc(it.descriptor)
        )
    }
    property.setterSignature?.let {
        property.setterSignature = JvmMethodSignature(
            remapper.mapMethodName("", it.name, it.descriptor),
            remapper.mapMethodDesc(it.descriptor)
        )
    }
}

private fun remapKmConstructor(constructor: KmConstructor, remapper: Remapper) {
    constructor.valueParameters.forEach { remapKmType(it.type, remapper) }
    constructor.signature?.let {
        constructor.signature = JvmMethodSignature(
            it.name,
            remapper.mapMethodDesc(it.descriptor)
        )
    }
}

private fun remapKmType(type: KmType, remapper: Remapper) {
    val classifier = type.classifier
    if (classifier is KmClassifier.Class) {
        type.classifier = KmClassifier.Class(remapper.map(classifier.name))
    }

    type.arguments.forEach {
        it.type?.let { type -> remapKmType(type, remapper) }
    }

    type.abbreviatedType?.let { remapKmType(it, remapper) }
}