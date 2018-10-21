@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin

import com.zachklipp.rxkotlin.internal.MultiCastStream
import com.zachklipp.rxkotlin.internal.NamedStreamOperator
import com.zachklipp.rxkotlin.internal.ReplayStream
import com.zachklipp.rxkotlin.internal.TransformedStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.whileSelect

/**
 * TODO write documentation.
 */
fun <T : Any, R : Any> Stream<T>.map(
    description: String = "map",
    transform: suspend (T) -> R
): Stream<R> = transform(description) { source, dest ->
    for (v in source) {
        dest.send(transform(v))
    }
}

/**
 * TODO docs
 */
fun <T : Any, R : Any> Stream<T>.flatMapSequence(
    description: String = "flatMapSequence",
    transform: suspend (T) -> Sequence<R>
): Stream<R> = flatMapIterable(description) {
    transform(it).asIterable()
}

/**
 * TODO docs
 */
fun <T : Any, R : Any> Stream<T>.flatMapIterable(
    description: String = "flatMapIterable",
    transform: suspend (T) -> Iterable<R>
): Stream<R> = transform(description) { source, dest ->
    for (v in source) {
        for (transformed in transform(v)) {
            dest.send(transformed)
        }
    }
}

/**
 * Merging (concurrent) flatmap.
 */
fun <T : Any, R : Any> Stream<T>.flatMap(
    description: String = "flatMap",
    transform: suspend (T) -> Stream<R>
): Stream<R> = transform(description) { source, dest ->
    for (v in source) {
        val subStream = transform(v)
        // Pipe the transformed source into the dest concurrently so we can
        // immediately read the next value from source.
        launch {
            subStream.toChannel(dest)
        }
    }
}

/**
 * Concatenating (sequential) flatmap.
 */
fun <T : Any, R : Any> Stream<T>.concatMap(
    description: String = "concatMap",
    transform: suspend (T) -> Stream<R>
): Stream<R> = transform(description) { source, dest ->
    for (v in source) {
        val subStream = transform(v)
        subStream.toChannel(dest)
    }
}

/**
 * Cancel-previous flatmap.
 */
fun <T : Any, R : Any> Stream<T>.switchMap(
    description: String = "switchMap",
    transform: suspend (T) -> Stream<R>
): Stream<R> = transform(description) { source, dest ->
    var subStreamSub: ReceiveChannel<R>? = null

    whileSelect {
        // Always give priority to the source channel, so it
        // can cut the substream off.
        source.onReceiveOrNull { v ->
            subStreamSub?.cancel()
            if (v == null) return@onReceiveOrNull false
            subStreamSub = transform(v).openSubscription()
            return@onReceiveOrNull true
        }

        subStreamSub?.onReceiveOrNull { v ->
            if (v == null) subStreamSub = null
            else {
                dest.send(v)
            }
            return@onReceiveOrNull true
        }
    }
}

/**
 * Returns a stream that will only subscribe upstream once, no matter how many downstream
 * subscribers there are.
 */
fun <T : Any> Stream<T>.publish(): ConnectableStream<T> = MultiCastStream(this)

/**
 * Returns a Stream that caches `n` items and replays the most recent `n` items to each subscriber.
 */
fun <T : Any> Stream<T>.replay(n: Int): Stream<T> = ReplayStream(this, n)

suspend fun <T : Any, C : SendChannel<T>> Stream<T>.toChannel(destination: C): C =
    subscribe { toChannel(destination) }

/**
 * TODO docs
 */
fun <T : Any> Stream<T>.scan(
    description: String = "scan",
    scanner: suspend (T, T) -> T
): Stream<T> = transform(description) { source, dest ->
    var previousValue = source.receive()
    for (v in source) {
        dest.send(scanner(previousValue, v))
        previousValue = v
    }
}

/**
 * TODO docs
 */
suspend fun <T : Any, A> Stream<T>.reduce(
    initialValue: A,
    reducer: suspend (A, T) -> A
): A = subscribe {
    var accumulator = initialValue
    for (v in this) {
        accumulator = reducer(accumulator, v)
    }
    return@subscribe accumulator
}

/**
 * TODO docs
 */
suspend fun <T : Any> Stream<T>.toList(): List<T> =
    subscribe { toList() }

suspend fun <T : Any> Stream<T>.first(
    predicate: suspend (T) -> Boolean = { true }
): T = subscribe { first { predicate(it) } }

suspend fun <T : Any> Stream<T>.firstOrNull(
    predicate: suspend (T) -> Boolean = { true }
): T? = subscribe { firstOrNull { predicate(it) } }

suspend fun <T : Any> Stream<T>.last(
    predicate: suspend (T) -> Boolean = { true }
): T = subscribe { last { predicate(it) } }

suspend fun <T : Any> Stream<T>.lastOrNull(
    predicate: suspend (T) -> Boolean = { true }
): T? = subscribe { firstOrNull { predicate(it) } }

suspend fun <T : Any> Stream<T>.single(
    predicate: suspend (T) -> Boolean = { true }
): T = subscribe { single { predicate(it) } }

suspend fun <T : Any> Stream<T>.none(
    predicate: suspend (T) -> Boolean = { true }
): Boolean = subscribe { none { predicate(it) } }

fun <T : Any> Stream<T>.drop(n: Int): Stream<T> =
    transform("drop($n)") { source, dest ->
        source.drop(n, coroutineContext).toChannel(dest)
    }

fun <T : Any> Stream<T>.take(n: Int): Stream<T> =
    transform("take($n)") { source, dest ->
        source.take(n, coroutineContext).toChannel(dest)
    }

fun <T : Any> Stream<T>.filter(
    description: String = "filter",
    predicate: suspend (T) -> Boolean
): Stream<T> = transform(description) { source, dest ->
    source.filter(coroutineContext, predicate).toChannel(dest)
}

fun <T : Any, R : Any> Stream<T>.mapNotNull(
    description: String = "mapNotNull", mapper: suspend (T) -> R?
): Stream<R> = transform(description) { source, dest ->
    source.mapNotNull(coroutineContext, mapper).toChannel(dest)
}

/**
 * Returns `block(this)` but grouped with a description.
 *
 * Allows creating complex operators composed from other operators that only show
 * up as a single element in the operator chain.
 */
fun <T : Any, R : Any> Stream<T>.compose(
    description: String,
    block: (Stream<T>) -> Stream<R>
): Stream<R> {
    val source = this
    val childStream = block(this)
    return object : NamedStreamOperator<R>(), Stream<R> by childStream {
        override val description: String = description
        override val upstream: Stream<*> get() = source
    }
}

/**
 * TODO docse
 */
fun <T : Any, R : Any> Stream<T>.transform(
    description: String,
    transformer: suspend CoroutineScope.(source: ReceiveChannel<T>, dest: SendChannel<R>) -> Unit
): Stream<R> = TransformedStream(this, description, transformer)

