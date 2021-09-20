import kotlin.reflect.KProperty

open class A {
    inline operator fun String.getValue(thisRef: Any?, property: KProperty<*>): String =
        property.name + this
}

class C : A() {
    val O by "K"
}

fun box() = C().O
