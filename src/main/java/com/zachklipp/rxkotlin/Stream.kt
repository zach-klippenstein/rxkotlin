@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlin.coroutines.coroutineContext

/**
 * A cold stream. The stream begins producing values once it is subscribed to.
 * Since streams are cold, every subscriber will get its own set of values.
 * A `Stream` is basically just a factory for [ReceiveChannel]s.
 *
 * Streams cannot contain `null`s.
 *
 * # Creating streams
 *
 * There are a number of builder functions that create streams for you.
 *  - [stream] – the most flexible of the builders. Accepts a coroutine builder block just like `produce`.
 *    The block is executed once for every subscriber.
 *    Its coroutine scope is a [SendChannel], and every send will block until the subscriber is ready to receive.
 *  - [streamOf] – the simplest of the builders. Takes a vararg list of values and will immediately emit all of
 *    them to its subscribers.
 *  - [Iterable.asStream], [Sequence.asStream] – return streams that emit their source's contents.
 *
 * # Subscribing to streams
 *
 * Subscription is represented as a [ReceiveChannel]. The channel must be [canceled][ReceiveChannel.cancel]
 * to unsubscribe. From a suspend function, you can subscribe with:
 *  - [Stream.openSubscription] – gives you the raw channel and leaves you to close it when you're done.
 *  - [Stream.subscribe] – you pass a block that accepts the channel, and the channel will automatically
 *    be closed for you when you're done.
 *
 * # Operating on and composing streams
 *
 * Streams can be manipulated using a wide variety of operators defined as extension functions.
 * All the standard functional operators are there: `map`, `flatMap`, `reduce`, etc.
 * You can also define your own operators. Operators that accept functions accept suspend functions.
 *
 * All built-in non-terminal operators can be given a description string to help with debugging.
 * [Stream.dumpChain] will return a list of operators from most-downstream to most-upstream, with their
 * descriptions. If you have an operator that is composed of a number of other operators, you can wrap
 * them up so they appear as a single operator using [Stream.compose].
 */
interface Stream<out T : Any> {
    /**
     * TODO kdoc
     */
    val upstream: Stream<*>?

    /**
     * Subscribes to the stream by opening a [channel][ReceiveChannel] that will receive elements.
     *
     * Subscribes to the stream using the current [CoroutineScope].
     *
     * The returned channel is a "rendezvous" channel (has no buffer). It will propagate backpressure
     * upstream directly.
     */
    fun CoroutineScope.openSubscription(): ReceiveChannel<T>
}

/**
 * Subscribes to this stream using the current context.
 *
 * The returned channel _must_ be [canceled][ReceiveChannel.cancel] when you're done reading from it.
 */
suspend fun <T : Any> Stream<T>.openSubscription(): ReceiveChannel<T> =
// Important to not use coroutineContext, since the act of opening a subscription
// will likely result in operators launching coroutines, which won't exit until
// the stream does, which means this function would deadlock (nothing reading from the
// channel until it's returned, but can't return until stream completes).
    with(CoroutineScope(coroutineContext)) {
        openSubscription()
    }

/**
 * Subscribes to the channel by opening a subscription channel, passing it to [block],
 * then always closing the channel before returning.
 */
suspend fun <T : Any, R> Stream<T>.subscribe(block: suspend ReceiveChannel<T>.() -> R): R =
    openSubscription().consume { block() }

fun Stream<*>.dumpChain(): String = StringBuilder().also { dumpChain(it) }.toString()

fun Stream<*>.dumpChain(builder: StringBuilder) {
    builder.appendln(this)
    upstream?.operatorChain?.forEach { operator ->
        builder.append("  ↳").appendln(operator)
    }
}

val Stream<*>.operatorChain: Sequence<Stream<*>>
    get() = generateSequence(this) { it.upstream }
