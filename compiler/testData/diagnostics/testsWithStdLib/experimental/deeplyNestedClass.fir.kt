// !OPT_IN: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI

@ExperimentalAPI
class C {
    class D {
        class E {
            class F
        }
    }
}

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
fun use1() {
    C.D.E.F()
}

@ExperimentalAPI
fun use2(f: C.D.E.F) = f.hashCode()

// FILE: usage-use.kt

package usage2

import api.*

@OptIn(ExperimentalAPI::class)
fun use1() {
    C.D.E.F()
}

@OptIn(ExperimentalAPI::class)
fun use2(f: C.D.E.F) = f.hashCode()

// FILE: usage-none.kt

package usage3

import api.*

fun use1() {
    C.D.<!OPT_IN_USAGE!>E<!>.F()
}

fun use2(f: <!OPT_IN_USAGE!>C.D.E.F<!>) = f.hashCode()

// FILE: around-kt48570.kt

@RequiresOptIn
annotation class SomeOptInMarker

@SomeOptInMarker
class A {
    class B {
        class C

        object D {
            fun bar() {}
        }
    }
}

// Should we report something?
fun foo(arg: <!OPT_IN_USAGE_ERROR!>A.B.C<!>) {  }

fun bar() {
    // Should we report something?
    A.B.<!OPT_IN_USAGE_ERROR!>D<!>.bar()
}

// Should we report something?
fun create() = A.<!OPT_IN_USAGE_ERROR!>B<!>.C()

fun use() {
    // Should we report something?
    val c = create()
}
