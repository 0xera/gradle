package org.gradle.script.lang.kotlin

import groovy.lang.Closure

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat

import org.junit.Assert.assertEquals
import org.junit.Test

class GroovyInteroperabilityTest {

    @Test
    fun `can use closure with single argument call`() {
        val list = arrayListOf<Int>()
        closureOf<MutableList<Int>> { add(42) }.call(list)
        assertEquals(42, list.first())
    }

    @Test
    fun `can use closure with delegate call`() {
        val list = arrayListOf<Int>()
        delegateClosureOf<MutableList<Int>> { add(42) }.apply {
            delegate = list
            call()
        }
        assertEquals(42, list.first())
    }

    @Test
    fun `can use KotlinClosure to control return type`() {
        fun stringClosure(function: String.() -> String) =
            KotlinClosure(function)

        assertEquals(
            "GROOVY",
            stringClosure { toUpperCase() }.call("groovy"))
    }

    @Test
    fun `can invoke Closure`() {

        val invocations = mutableListOf<String>()

        val c0 =
            object : Closure<Boolean>(null, null) {
                @Suppress("unused")
                fun doCall() = invocations.add("c0")
            }
        val c1 =
            object : Closure<Boolean>(null, null) {
                @Suppress("unused")
                fun doCall(x: Any) = invocations.add("c1($x)")
            }
        val c2 =
            object : Closure<Boolean>(null, null) {
                @Suppress("unused")
                fun doCall(x: Any, y: Any) = invocations.add("c2($x, $y)")
            }

        assert(c0())
        assert(c1(42))
        assert(c2(11, 33))

        assertThat(
            invocations,
            equalTo(listOf("c0", "c1(42)", "c2(11, 33)")))
    }
}
