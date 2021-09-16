/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link GenerateNewCompilerTests.kt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/components/compileTimeConstantEvaluator")
@TestDataPath("$PROJECT_ROOT")
public class CompileTimeConstantEvaluatorTestGenerated extends AbstractCompileTimeConstantEvaluatorTest {
    @Test
    public void testAllFilesPresentInCompileTimeConstantEvaluator() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/compileTimeConstantEvaluator"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("annotationInAnnotation_arrayOf.kt")
    public void testAnnotationInAnnotation_arrayOf() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/annotationInAnnotation_arrayOf.kt");
    }

    @Test
    @TestMetadata("annotationInAnnotation_collectionLiteral.kt")
    public void testAnnotationInAnnotation_collectionLiteral() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/annotationInAnnotation_collectionLiteral.kt");
    }

    @Test
    @TestMetadata("annotationInAnnotation_multipleAnnotations_arrayOf.kt")
    public void testAnnotationInAnnotation_multipleAnnotations_arrayOf() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/annotationInAnnotation_multipleAnnotations_arrayOf.kt");
    }

    @Test
    @TestMetadata("annotationInAnnotation_multipleAnnotations_collectionLiteral.kt")
    public void testAnnotationInAnnotation_multipleAnnotations_collectionLiteral() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/annotationInAnnotation_multipleAnnotations_collectionLiteral.kt");
    }

    @Test
    @TestMetadata("enumAsAnnotationValue.kt")
    public void testEnumAsAnnotationValue() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/enumAsAnnotationValue.kt");
    }

    @Test
    @TestMetadata("propertyInit_Byte.kt")
    public void testPropertyInit_Byte() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/propertyInit_Byte.kt");
    }

    @Test
    @TestMetadata("propertyInit_Double.kt")
    public void testPropertyInit_Double() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/propertyInit_Double.kt");
    }

    @Test
    @TestMetadata("propertyInit_Float.kt")
    public void testPropertyInit_Float() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/propertyInit_Float.kt");
    }

    @Test
    @TestMetadata("propertyInit_Int.kt")
    public void testPropertyInit_Int() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/propertyInit_Int.kt");
    }

    @Test
    @TestMetadata("propertyInit_Long.kt")
    public void testPropertyInit_Long() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/propertyInit_Long.kt");
    }

    @Test
    @TestMetadata("propertyInit_UInt.kt")
    public void testPropertyInit_UInt() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/propertyInit_UInt.kt");
    }

    @Test
    @TestMetadata("stringLiteral.kt")
    public void testStringLiteral() throws Exception {
        runTest("analysis/analysis-api/testData/components/compileTimeConstantEvaluator/stringLiteral.kt");
    }
}
