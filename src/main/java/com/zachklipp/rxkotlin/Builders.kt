@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin

import com.zachklipp.rxkotlin.internal.ProducerStream
import kotlinx.coroutines.channels.ProducerScope

/**
 * TODO write documentation.
 *
 * `block` is invoked in the _subscriber's_ context.
 */
fun <T : Any> stream(
    description: String = "{stream builder}",
    block: suspend ProducerScope<T>.() -> Unit
): Stream<T> = object : ProducerStream<T>() {
    override fun toString(): String = description
    override suspend fun ProducerScope<T>.produceStream() = block()
}

/**
 * TODO documentation
 */
fun <T : Any> streamOf(vararg values: T): Stream<T> = object : ProducerStream<T>() {
    override fun toString(): String = buildString {
        append("streamOf(")
        append(
            values.joinToString(
                separator = ", ",
                limit = 3,
                truncated = "…"
            )
        )
        append(")")
        if (values.size > 3) {
            append(" [size=${values.size}]")
        }
    }

    override suspend fun ProducerScope<T>.produceStream() {
        for (v in values) {
            send(v)
        }
    }
}

fun <T : Any> Sequence<T>.asStream(description: String = "fromSequence"): Stream<T> =
    asIterable().asStream(description)

fun <T : Any> Iterable<T>.asStream(description: String = "fromIterable"): Stream<T> =
    object : ProducerStream<T>() {
        override fun toString(): String = buildString {
            append(description)
            (this@asStream as? Collection)?.let {
                append(" [size=${it.size}]")
            }
        }

        override suspend fun ProducerScope<T>.produceStream() {
            for (v in this@asStream) {
                send(v)
            }
        }
    }
