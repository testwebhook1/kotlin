/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.sequence.exec;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.TestRoot;
import org.junit.runner.RunWith;

/*
 * This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("jvm-debugger/test")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("testData/sequence/streams/sequence")
public abstract class SequenceTraceTestCaseGenerated extends AbstractSequenceTraceTestCase {
    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/append")
    public static class Append extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("PlusArray.kt")
        public void testPlusArray() throws Exception {
            runTest("testData/sequence/streams/sequence/append/PlusArray.kt");
        }

        @TestMetadata("PlusElement.kt")
        public void testPlusElement() throws Exception {
            runTest("testData/sequence/streams/sequence/append/PlusElement.kt");
        }

        @TestMetadata("PlusSequence.kt")
        public void testPlusSequence() throws Exception {
            runTest("testData/sequence/streams/sequence/append/PlusSequence.kt");
        }

        @TestMetadata("PlusSingle.kt")
        public void testPlusSingle() throws Exception {
            runTest("testData/sequence/streams/sequence/append/PlusSingle.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/distinct")
    public static class Distinct extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("Distinct.kt")
        public void testDistinct() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/Distinct.kt");
        }

        @TestMetadata("DistinctBy.kt")
        public void testDistinctBy() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/DistinctBy.kt");
        }

        @TestMetadata("DistinctByBigPrimitives.kt")
        public void testDistinctByBigPrimitives() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/DistinctByBigPrimitives.kt");
        }

        @TestMetadata("DistinctByNullableElement.kt")
        public void testDistinctByNullableElement() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/DistinctByNullableElement.kt");
        }

        @TestMetadata("DistinctByNullableKey.kt")
        public void testDistinctByNullableKey() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/DistinctByNullableKey.kt");
        }

        @TestMetadata("DistinctByNullableKeyAndElement.kt")
        public void testDistinctByNullableKeyAndElement() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/DistinctByNullableKeyAndElement.kt");
        }

        @TestMetadata("DistinctObjects.kt")
        public void testDistinctObjects() throws Exception {
            runTest("testData/sequence/streams/sequence/distinct/DistinctObjects.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/filter")
    public static class Filter extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("Drop.kt")
        public void testDrop() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/Drop.kt");
        }

        @TestMetadata("DropWhile.kt")
        public void testDropWhile() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/DropWhile.kt");
        }

        @TestMetadata("Filter.kt")
        public void testFilter() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/Filter.kt");
        }

        @TestMetadata("FilterIndexed.kt")
        public void testFilterIndexed() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/FilterIndexed.kt");
        }

        @TestMetadata("FilterIsInstance.kt")
        public void testFilterIsInstance() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/FilterIsInstance.kt");
        }

        @TestMetadata("FilterNot.kt")
        public void testFilterNot() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/FilterNot.kt");
        }

        @TestMetadata("Minus.kt")
        public void testMinus() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/Minus.kt");
        }

        @TestMetadata("MinusElement.kt")
        public void testMinusElement() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/MinusElement.kt");
        }

        @TestMetadata("Take.kt")
        public void testTake() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/Take.kt");
        }

        @TestMetadata("TakeWhile.kt")
        public void testTakeWhile() throws Exception {
            runTest("testData/sequence/streams/sequence/filter/TakeWhile.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/flatMap")
    public static class FlatMap extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FlatMap.kt")
        public void testFlatMap() throws Exception {
            runTest("testData/sequence/streams/sequence/flatMap/FlatMap.kt");
        }

        @TestMetadata("Flatten.kt")
        public void testFlatten() throws Exception {
            runTest("testData/sequence/streams/sequence/flatMap/Flatten.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/map")
    public static class Map extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("Map.kt")
        public void testMap() throws Exception {
            runTest("testData/sequence/streams/sequence/map/Map.kt");
        }

        @TestMetadata("MapIndexed.kt")
        public void testMapIndexed() throws Exception {
            runTest("testData/sequence/streams/sequence/map/MapIndexed.kt");
        }

        @TestMetadata("MapNotNull.kt")
        public void testMapNotNull() throws Exception {
            runTest("testData/sequence/streams/sequence/map/MapNotNull.kt");
        }

        @TestMetadata("WithIndex.kt")
        public void testWithIndex() throws Exception {
            runTest("testData/sequence/streams/sequence/map/WithIndex.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/misc")
    public static class Misc extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("AsSequence.kt")
        public void testAsSequence() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/AsSequence.kt");
        }

        @TestMetadata("Chunked.kt")
        public void testChunked() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/Chunked.kt");
        }

        @TestMetadata("ChunkedWithTransform.kt")
        public void testChunkedWithTransform() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ChunkedWithTransform.kt");
        }

        @TestMetadata("ConstrainOnce.kt")
        public void testConstrainOnce() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ConstrainOnce.kt");
        }

        @TestMetadata("OnEach.kt")
        public void testOnEach() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/OnEach.kt");
        }

        @TestMetadata("RequireNoNulls.kt")
        public void testRequireNoNulls() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/RequireNoNulls.kt");
        }

        @TestMetadata("Windowed.kt")
        public void testWindowed() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/Windowed.kt");
        }

        @TestMetadata("WindowedWithBigStep.kt")
        public void testWindowedWithBigStep() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/WindowedWithBigStep.kt");
        }

        @TestMetadata("WindowedWithPartial.kt")
        public void testWindowedWithPartial() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/WindowedWithPartial.kt");
        }

        @TestMetadata("WindowedWithStep.kt")
        public void testWindowedWithStep() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/WindowedWithStep.kt");
        }

        @TestMetadata("ZipWithGreater.kt")
        public void testZipWithGreater() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ZipWithGreater.kt");
        }

        @TestMetadata("ZipWithLesser.kt")
        public void testZipWithLesser() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ZipWithLesser.kt");
        }

        @TestMetadata("ZipWithNextMany.kt")
        public void testZipWithNextMany() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ZipWithNextMany.kt");
        }

        @TestMetadata("ZipWithNextSingle.kt")
        public void testZipWithNextSingle() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ZipWithNextSingle.kt");
        }

        @TestMetadata("ZipWithSame.kt")
        public void testZipWithSame() throws Exception {
            runTest("testData/sequence/streams/sequence/misc/ZipWithSame.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("testData/sequence/streams/sequence/sort")
    public static class Sort extends AbstractSequenceTraceTestCase {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("Sorted.kt")
        public void testSorted() throws Exception {
            runTest("testData/sequence/streams/sequence/sort/Sorted.kt");
        }

        @TestMetadata("SortedBy.kt")
        public void testSortedBy() throws Exception {
            runTest("testData/sequence/streams/sequence/sort/SortedBy.kt");
        }

        @TestMetadata("SortedByDescending.kt")
        public void testSortedByDescending() throws Exception {
            runTest("testData/sequence/streams/sequence/sort/SortedByDescending.kt");
        }

        @TestMetadata("SortedDescending.kt")
        public void testSortedDescending() throws Exception {
            runTest("testData/sequence/streams/sequence/sort/SortedDescending.kt");
        }

        @TestMetadata("SortedWith.kt")
        public void testSortedWith() throws Exception {
            runTest("testData/sequence/streams/sequence/sort/SortedWith.kt");
        }
    }
}
