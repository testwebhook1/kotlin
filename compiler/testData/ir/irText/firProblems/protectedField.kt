// WITH_RUNTIME
// FILE: protectedField1.kt

package first

abstract class Base(@JvmField protected val myProject: String)

// FILE: protectedField2.kt

package second

import first.Base

class My : Base("OK") {
    private val status get() = myProject
}
