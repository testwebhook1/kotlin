// EXPECTED_REACHABLE_NODES: 1286
// MODULE: module_1
// FILE: bar.kt
// MODULE_KIND: UMD
fun bar() = "bar"

// MODULE: main(module_1)
// FILE: box.kt
// MODULE_KIND: UMD
fun box(): String {
    assertEquals("bar", bar())
    return "OK"
}