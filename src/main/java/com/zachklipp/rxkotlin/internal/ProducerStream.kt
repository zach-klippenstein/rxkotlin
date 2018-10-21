@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin.internal

import com.zachklipp.rxkotlin.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

internal abstract class ProducerStream<T : Any> : Stream<T> {
    override val upstream: Stream<*>? get() = null
    override fun CoroutineScope.openSubscription(): ReceiveChannel<T> =
        produce { produceStream() }

    abstract suspend fun ProducerScope<T>.produceStream()
}
