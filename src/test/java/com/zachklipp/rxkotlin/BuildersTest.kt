@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin

import kotlinx.coroutines.channels.none
import kotlinx.coroutines.channels.single
import kotlinx.coroutines.channels.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

/**
 * TODO write documentation.
 */
class BuildersTest {

    @Rule
    @JvmField
    val thrown = ExpectedException.none()!!

    @Test
    fun streamOf_empty() = runTest {
        val stream = streamOf<Int>()

        stream.subscribe {
            assertTrue(none())
        }

        assertEquals("streamOf()", stream.toString())
    }

    @Test
    fun streamOf_single() = runTest {
        val stream = streamOf(42)

        stream.subscribe {
            assertEquals(42, single())
        }

        assertEquals("streamOf(42)", stream.toString())
    }

    @Test
    fun streamOf_multi_lessThan3() = runTest {
        val stream = streamOf(42, 256)

        stream.subscribe {
            assertEquals(listOf(42, 256), toList())
        }

        assertEquals("streamOf(42, 256)", stream.toString())
    }

    @Test
    fun streamOf_multi_moreThan3() = runTest {
        val stream = streamOf(1, 2, 3, 4, 5)

        stream.subscribe {
            assertEquals(listOf(1, 2, 3, 4, 5), toList())
        }

        assertEquals("streamOf(1, 2, 3, …) [size=5]", stream.toString())
    }

    @Test
    fun stream_empty() = runTest {
        val stream = stream<Int> { }

        stream.subscribe {
            assertTrue(none())
        }
    }

    @Test
    fun stream_single() = runTest {
        val stream = stream<Int> { send(42) }

        stream.subscribe {
            assertEquals(42, single())
        }
    }

    @Test
    fun stream_multi() = runTest {
        val stream = stream<Int> {
            send(42)
            send(256)
        }

        stream.subscribe {
            assertEquals(listOf(42, 256), toList())
        }
    }

    @Test
    fun stream_errorBeforeEmitting() = runTest {
        val stream = stream<Int> {
            throw RuntimeException("fail")
        }

        stream.subscribe {
            thrown.expect(RuntimeException::class.java)
            thrown.expectMessage("fail")
            receive()
        }
    }

    @Test
    fun stream_errorAfterEmitting() = runTest {
        val stream = stream<Int> {
            send(42)
            throw RuntimeException("fail")
        }

        stream.subscribe {
            receive()

            thrown.expect(RuntimeException::class.java)
            thrown.expectMessage("fail")

            receive()
        }
    }
}
