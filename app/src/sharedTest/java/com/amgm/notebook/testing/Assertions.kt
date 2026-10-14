package com.amgm.notebook.testing

/**
 * [block] が型 T の例外を投げることを検証し、その例外を返す。
 * suspend 関数は `assertThrowsInstance<X> { runBlocking { repo.save(x) } }` の形で呼ぶ。
 */
inline fun <reified T : Throwable> assertThrowsInstance(block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        throw e
    }
    throw AssertionError("${T::class.java.simpleName} が投げられるはずでした")
}
