package com.schwarz.crystalprocessor.generation.model

import com.schwarz.crystalapi.schema.*
import com.schwarz.crystalprocessor.model.entity.SchemaClassHolder
import com.schwarz.crystalprocessor.model.field.CblBaseFieldHolder
import com.schwarz.crystalprocessor.model.field.CblFieldHolder
import com.schwarz.crystalprocessor.util.ConversionUtil
import com.schwarz.crystalprocessor.util.TypeUtil
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * This class for generation of the Schema classes.
 *
 * To generate a SchemaClass add the
 * ```
 * @SchemaClass
 * ```
 * annotation to your Class and all the Fields will be generated into a new File.
 */
class SchemaGeneration {
    private val pathAttributeName = "path"
    fun generateModel(holder: SchemaClassHolder, schemaClassPaths: List<String>): FileSpec {
        val packageName = holder.sourcePackage
        val schemaClassName = holder.entitySimpleName

        val schemaClass: TypeSpec.Builder = buildSchemaClass(schemaClassName)

        buildAndAddFieldProperties(holder, schemaClass, schemaClassPaths)

        return FileSpec.builder(packageName, schemaClassName).addType(schemaClass.build()).build()
    }

    private fun buildSchemaClass(className: String): TypeSpec.Builder {
        val pathParameter = ParameterSpec.builder(pathAttributeName, String::class).defaultValue("%S", "").build()

        return TypeSpec.classBuilder(className)
            .addModifiers(KModifier.OPEN)
            .addSuperinterface(Schema::class)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(pathParameter)
                    .build()
            )
    }

    private fun buildAndAddFieldProperties(
        holder: SchemaClassHolder,
        schemaClass: TypeSpec.Builder,
        schemaClassPaths: List<String>,
    ) {
        buildAndAddConstantFieldProperties(holder, schemaClass, schemaClassPaths)
        buildAndAddNormalFieldProperties(holder, schemaClass, schemaClassPaths)
    }

    private fun buildAndAddConstantFieldProperties(
        holder: SchemaClassHolder,
        schemaClass: TypeSpec.Builder,
        schemaClassPaths: List<String>
    ) {
        holder.fieldConstants.forEach { (fieldName, fieldObject) ->
            val defaultVariableName = "DEFAULT_${fieldObject.constantName}"

            val constantProperty = PropertySpec.builder(
                defaultVariableName,
                fieldObject.fieldType
            ).initializer(
                ConversionUtil.convertStringToDesiredFormat(
                    fieldObject.typeMirror,
                    fieldObject.constantValue
                )
            )

            schemaClass.addProperty(constantProperty.build())

            buildAndAddFieldProperty(
                schemaClass = schemaClass,
                fieldName = fieldName,
                fieldObject = fieldObject,
                schemaClassPaths = schemaClassPaths
            )
        }
    }

    private fun buildAndAddNormalFieldProperties(
        holder: SchemaClassHolder,
        schemaClass: TypeSpec.Builder,
        schemaClassPaths: List<String>
    ) {
        holder.fields.forEach { (fieldName, fieldObject) ->
            buildAndAddFieldProperty(
                schemaClass = schemaClass,
                fieldName = fieldName,
                fieldObject = fieldObject,
                schemaClassPaths = schemaClassPaths,
            )
        }
    }

    private fun buildAndAddFieldProperty(
        schemaClass: TypeSpec.Builder,
        fieldName: String,
        fieldObject: CblBaseFieldHolder,
        schemaClassPaths: List<String>,
    ): TypeSpec.Builder = schemaClass.addProperty(
        buildFieldProperty(fieldObject, fieldName, schemaClassPaths)
    )

    private fun buildFieldProperty(
        fieldObject: CblBaseFieldHolder,
        fieldName: String,
        schemaClassPaths: List<String>,
    ): PropertySpec {
        val isObject = schemaClassPaths.contains(fieldObject.typeMirror.toString())

        val outerType = getOuterPropertyType(fieldObject.isIterable, isObject)

        val innerType: TypeName = getInnerPropertyType(fieldObject)

        return PropertySpec.builder(
            fieldName,
            outerType.parameterizedBy(innerType)
        ).initializer(
            createPropertyFormat(fieldName, innerType, fieldObject.isIterable, isObject),
            outerType,
        ).build()
    }

    private fun createPropertyFormat(
        fieldName: String,
        propertyType: TypeName,
        isIterable: Boolean,
        isObject: Boolean
    ): String {
        val propertyAccessPath =
            "if ($pathAttributeName.isBlank()) \"$fieldName\" else \"\$$pathAttributeName.$fieldName\""

        return when {
            isIterable && isObject -> buildObjectListFormat(propertyType, fieldName, propertyAccessPath)
            isObject -> buildObjectFormat(propertyType, propertyAccessPath)
            else -> buildSimpleFormat(fieldName)
        }
    }

    private fun buildObjectListFormat(propertyType: TypeName, fieldName: String, propertyAccessPath: String): String =
        """%T(
            $propertyType($propertyAccessPath),
            "$fieldName",
            $pathAttributeName,
        )"""

    private fun buildSimpleFormat(fieldName: String): String =
        """%T("$fieldName", $pathAttributeName)"""

    private fun buildObjectFormat(propertyType: TypeName, propertyAccessPath: String): String =
        """%T(
            $propertyType($propertyAccessPath),
            $pathAttributeName,
        )"""

    private fun getOuterPropertyType(
        isIterable: Boolean,
        isObject: Boolean
    ) = when {
        isIterable && isObject -> CMObjectList::class.asTypeName()
        isIterable -> CMList::class.asTypeName()
        isObject -> CMObject::class.asTypeName()
        else -> CMField::class.asTypeName()
    }

    private fun getInnerPropertyType(field: CblBaseFieldHolder): TypeName {
        val name: String? = if (field is CblFieldHolder) field.subEntitySimpleName else null

        return TypeUtil.parseMetaType(field.typeMirror, false, name)
    }
}
