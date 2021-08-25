// FIR_IDENTICAL
// FILE: test/W.kt
package test

import test.Y.Companion.func

class W {
    fun foo() {}
}

abstract class Base

class Y : Base() {
    companion object {
        fun Base.func() {
        }
    }
}

fun Base.func() = func()

// FILE: main.kt
import test.W
import test.Y
import test.func

val W: W = W()

fun main() {
    W.foo() // Property wins Classifier, no error
    Y().func()
}

