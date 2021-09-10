// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME

// MODULE: m1
// FILE: m1.kt

class SomeClass {
    @JvmField var foo: MutableList<Name>? = null
}

class Name(@JvmField val name: String)

// MODULE: m2(m1)
// FILE: m2.kt

fun box(): String {
    val sc = SomeClass()
    sc.foo = mutableListOf(Name("OK"))
    sc.foo?.let {
        return@box it[0].name
    }
    return "FAIL"
}