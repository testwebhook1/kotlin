// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// SKIP_MINIFICATION
// SKIP_DCE_DRIVEN
// SKIP_NODE_JS

// MODULE: exportedBaseClass
// FILE: lib.kt
@JsExport
abstract class Base {
    abstract val ok: String
    abstract var bar: String
}

// Non-exported
open class Derived1 : Base() {
    override val ok: String
        get() = "1"

    private var _bar = "1"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }
}

class Derived2 : Derived1() {
    override val ok: String
        get() = "2"

    private var _bar = "2"

    override var bar: String
        get() = _bar
        set(value) { _bar = value }
}

@JsExport
fun getDerived1(): Base = Derived1()

@JsExport
fun getDerived2(): Base = Derived2()

// FILE: test.js
function assertEquals(expected, actual, msg) {
    if (expected !== actual) {
        throw "Unexpected value: expected = '" + expected + "', actual = '" + actual + "' — " + msg;
    }
}

function box() {
    var derived1 = exportedBaseClass.getDerived1();
    assertEquals('1', derived1.ok, "derived1.ok");
    assertEquals('1', derived1.bar, "derived1.bar initial value");
    derived1.bar = '11';
    assertEquals('11', derived1.bar, "derived1.bar after write");

    var derived2 = exportedBaseClass.getDerived2();
    assertEquals('2', derived2.ok, "derived2.ok");
    assertEquals('2', derived2.bar, "derived2.bar initial value");
    derived2.bar = '22';
    assertEquals('22', derived2.bar, "derived2.bar after write");

    return 'OK';
}