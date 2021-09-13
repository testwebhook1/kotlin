// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME

// MODULE: m1
// FILE: ContainerDescriptor.java

package same;

import org.jetbrains.annotations.Nullable;

public final class ContainerDescriptor {
    @Nullable String extensionPoint = "OK";
}

// MODULE: m2(m1)
// FILE: DynamicPlugins.kt

package same

object DynamicPlugins {
    fun box(): String {
        return doIt()
    }
}

private fun doIt(): String {
    val cd = ContainerDescriptor()
    cd.extensionPoint?.let {
        return@doIt it
    }
    return "FAIL"
}

fun box() = DynamicPlugins.box()
