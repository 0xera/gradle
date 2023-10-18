package com.example

import com.h0tk3y.kotlin.staticObjectNotation.Adding
import com.h0tk3y.kotlin.staticObjectNotation.Builder

class Abc {
    var a: Int = 0
    fun b(): Int = 1
    var str: String = ""
    @Adding
    fun c(x: Int, configure: C.() -> Unit = { }) =
        C().apply {
            this.x = x;
            configure();
            cItems.add(this)
        }

    internal val cItems = mutableListOf<C>()
}

class C(var x: Int = 0) {
    @Builder
    fun d(newD: D): C {
        this.d = newD
        return this
    }
    
    var d: D = D()
    val y = "test"
    fun f(y: String) = 0
}

class D {
    var id: String = "none"
}

fun newD(id: String): D = D().also { it.id = id }
