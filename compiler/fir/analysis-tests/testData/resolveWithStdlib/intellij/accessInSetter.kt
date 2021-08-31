// FULL_JDK
// MODULE: m1
// FILE: m1.kt

import javax.swing.JList

class DrawableGrid : JList<String>() {
    override fun setSelectionInterval(anchor: Int, lead: Int) {
        super.setSelectionInterval(anchor, lead)
    }
}

// MODULE: m2(m1)
// FILE: m2.kt

class My(private val originalValue: String?) {
    private val drawableGrid = createDrawableGrid()

    private var useAll = originalValue == null || isSampleValueAll(originalValue)
        set(value) {
            field = value
            drawableGrid.<!UNRESOLVED_REFERENCE!>isEnabled<!> = !value
            if (value) drawableGrid.<!UNRESOLVED_REFERENCE!>clearSelection<!>() else drawableGrid.<!UNRESOLVED_REFERENCE!>selectedIndex<!> = 0
        }

    private fun createDrawableGrid() = DrawableGrid().<!OVERLOAD_RESOLUTION_AMBIGUITY!>apply<!> {
        <!UNRESOLVED_REFERENCE!>isOpaque<!> = false
        <!UNRESOLVED_REFERENCE!>visibleRowCount<!> = 3
        <!UNRESOLVED_REFERENCE!>addListSelectionListener<!> { _ ->
        }
    }

    private fun isSampleValueAll(value: String?) = value?.endsWith(']')?.not() ?: false
}
