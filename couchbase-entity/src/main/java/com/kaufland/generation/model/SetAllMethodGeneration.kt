package com.kaufland.generation.model

import com.kaufland.model.entity.BaseEntityHolder
import com.kaufland.model.entity.EntityHolder
import com.kaufland.util.TypeUtil
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import kaufland.com.coachbasebinderapi.PersistenceConfig

class SetAllMethodGeneration {

    fun generate(holder: BaseEntityHolder, useMDocChanges: Boolean): FunSpec {

        val attributeName = if(useMDocChanges) "mDocChanges" else "mDoc"
        val setAllBuilder = FunSpec.builder("setAll").addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
            .addParameter("map", TypeUtil.mapStringAnyNullable()).addStatement(
                "$attributeName.putAll(map)"
            )

        return setAllBuilder.build()
    }
}
