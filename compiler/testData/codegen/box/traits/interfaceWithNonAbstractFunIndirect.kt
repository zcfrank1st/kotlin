interface I {
    fun foo(): String = "foo"

    fun bar(x: String = "default") = "bar:$x"
}

interface J : I

class A : I, J

fun box(): String {
    val foo = A().foo()
    if (foo != "foo") return "fail1: $foo"

    val bar1 = A().bar()
    if (bar1 != "bar:default") return "fail2: $bar1"

    val bar2 = A().bar("q")
    if (bar2 != "bar:q") return "fail3: $bar1"

    return "OK"
}