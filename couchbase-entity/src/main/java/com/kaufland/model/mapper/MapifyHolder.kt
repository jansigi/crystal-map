package com.kaufland.model.mapper

import com.kaufland.ProcessingContext
import com.kaufland.ProcessingContext.asDeclaringName
import com.kaufland.ProcessingContext.asTypeElement
import com.kaufland.ProcessingContext.isAssignable
import com.kaufland.javaToKotlinType
import com.kaufland.util.FieldExtractionUtil
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import kaufland.com.coachbasebinderapi.mapify.Mapify
import kaufland.com.coachbasebinderapi.mapify.Mapifyable
import java.io.Serializable
import java.math.BigDecimal
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier


class MapifyHolder(val element: Element, val mapify: Mapify, env: ProcessingEnvironment) {

    val fieldName = element.simpleName.toString()

    val mapName = if (mapify.name.isNotBlank()) mapify.name else fieldName

    val typeName = element.asType().asTypeName().javaToKotlinType()

    val accessible = element.modifiers.contains(Modifier.PUBLIC)

    val declaringName: ProcessingContext.DeclaringName = element.asDeclaringName()

    val reflectedFieldName = "a${fieldName.capitalize()}"

    val accessorName = "${fieldName}_"

    val typeHandleMode: TypeHandleMode = when {
        declaringName.isPlainType() -> TypeHandleMode.PLAIN
        isMapifyable(declaringName) -> TypeHandleMode.MAPIFYABLE
        else -> TypeHandleMode.UNKNOWN
    }

    private fun isMapifyable(declaringName: ProcessingContext.DeclaringName): Boolean {
        return (declaringName.isProcessingType() || declaringName.asTypeElement()?.let { it.isAssignable(List::class.java) || it.isAssignable(Map::class.java) || it.isAssignable(Serializable::class.java) || it.getAnnotation(Mapifyable::class.java) != null } ?: false) && declaringName.typeParams.all { isMapifyable(it) }
    }

    enum class TypeHandleMode {
        PLAIN,
        MAPIFYABLE,
        UNKNOWN
    }
}
