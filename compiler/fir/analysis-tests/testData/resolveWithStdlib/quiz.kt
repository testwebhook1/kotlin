open class Base
interface Generator<out T>
class Derived1 : Base(), Comparable<Derived1>, Generator<Derived1> {
    override fun compareTo(other: Derived1) = 0
}
class Derived2 : Base(), Comparable<Derived2>, Generator<Derived2> {
    override fun compareTo(other: Derived2) = 0

}

fun foo(arg: Any) {
    if (arg is Derived1 || arg is Derived2) {
        println(arg)
        arg.compareTo(<!ARGUMENT_TYPE_MISMATCH!>Base()<!>)
    }
}
