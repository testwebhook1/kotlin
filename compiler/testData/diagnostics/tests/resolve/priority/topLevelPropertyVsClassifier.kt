// FIR_IDENTICAL
// FILE: test/W.kt
package test

class W {
    fun foo() {}
}

// FILE: main.kt
import test.W;

val W: W = W()

fun main() {
    W.foo() // Property wins Classifier, no error
}