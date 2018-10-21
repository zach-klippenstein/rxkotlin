@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

/**
 * TODO write documentation.
 */
class OperatorsTest {

    @Rule
    @JvmField
    val thrown = ExpectedException.none()!!

    @Test
    fun map_works() = runTest {
        val stream = streamOf(1, 2, 3)
        assertEquals(listOf("1", "2", "3"), stream.map { it.toString() }.toList())
    }

    // TODO more tests
}
