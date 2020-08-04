package com.kaufland.generation

import com.kaufland.CoachBaseBinderProcessor
import com.squareup.kotlinpoet.FileSpec
import java.io.File

import java.io.IOException

import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment

class CodeGenerator(private val filer: Filer) {

    @Throws(IOException::class)
    fun generate(entityToGenerate: FileSpec, processingEnvironment: ProcessingEnvironment) {

        val codePath = processingEnvironment.options[CoachBaseBinderProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val fileWithHeader = entityToGenerate.toBuilder().addComment(HEADER).build()

        //used for kapt returns null for legacy annotationprocessor declarations
        if (codePath != null) {
            fileWithHeader.writeTo(File(codePath))
        }else{
            fileWithHeader.writeTo(filer)
        }
    }

    companion object {

        private val HEADER = ("DO NOT EDIT THIS FILE.\n"
                + "Generated using Couchbasebinder\n\n"
                + "Do not edit this class!!!!.\n")
    }
}
