package com.kaufland.generation

import com.kaufland.model.entity.BaseEntityHolder
import com.kaufland.util.TypeUtil
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import javax.lang.model.type.TypeMirror

object CblConstantGeneration {

    fun addConstants(holder: BaseEntityHolder): FunSpec {
        val builder = FunSpec.builder("addConstants").addModifiers(KModifier.PRIVATE).addParameter("map", TypeUtil.mapStringObject())

        for (fieldHolder in holder.fieldConstants) {

            if (fieldHolder.isConstant) {
                builder.addStatement("map.put(\$N, " + getConvertedValue(fieldHolder.typeMirror!!, fieldHolder.defaultValue) + ")", fieldHolder.constantName)
            }
        }
        return builder.build()
    }

    fun addAddCall(nameOfMap: String): CodeBlock {
        return CodeBlock.builder().addStatement("addConstants(\$N)", nameOfMap).build()
    }

    private fun getConvertedValue(clazz: TypeMirror, value: String): String {

        return if (clazz.toString() == String::class.java.canonicalName) {
            "\"" + value + "\""
        } else value
    }

}
