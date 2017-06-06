// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import java.io.*
import kotlin.test.*

class Foo

fun box(): String {
    val baos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(baos)
    oos.writeObject(Foo::class)
    oos.close()

    val bais = ByteArrayInputStream(baos.toByteArray())
    val ois = ObjectInputStream(bais)
    assertEquals(Foo::class, ois.readObject())
    ois.close()

    return "OK"
}
