/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.new

import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File
import java.io.FileFilter

class JsAdditionalSourceProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        val globalCommonFiles = getFilesInDirectoryByExtension(COMMON_FILES_DIR_PATH, KotlinFileType.EXTENSION)
            .map { File(it).toTestFile() }

        val localCommonFilePath = module.files.first().originalFile.parent + "/" + COMMON_FILES_NAME + "." + KotlinFileType.EXTENSION
        val localCommonFile = File(localCommonFilePath).takeIf { it.exists() }?.toTestFile() ?: return globalCommonFiles

        return globalCommonFiles + localCommonFile
    }

    companion object {
        private const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        private const val DIST_DIR_JS_PATH = "dist/js/"

        private const val COMMON_FILES_NAME = "_common"
        private const val COMMON_FILES_DIR = "_commonFiles/"
        private const val COMMON_FILES_DIR_PATH = TEST_DATA_DIR_PATH + COMMON_FILES_DIR

        private fun getFilesInDirectoryByExtension(directory: String, extension: String): List<String> {
            val dir = File(directory)
            if (!dir.isDirectory) return emptyList()

            return dir.listFiles(FileFilter { it.extension == extension })?.map { it.absolutePath } ?: emptyList()
        }
    }
}