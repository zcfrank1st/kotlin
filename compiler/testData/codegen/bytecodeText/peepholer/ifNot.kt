fun test(a: Any, b: Any, s: Set<Any>) {
    if (a != b) println("1")
    if (a !in s) println("2")
}

// 0 ICONST_1
// 0 IXOR