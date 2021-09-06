// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: Base.kt

package base

abstract class Base(@JvmField protected val myProject: String)

// FILE: box.kt

package box

import base.Base

class My : Base("OK") {
    private val status get() = myProject

    fun result() = status
}

fun box() = My().result()
