@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin.internal

import com.zachklipp.rxkotlin.Stream
import com.zachklipp.rxkotlin.subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce

internal class TransformedStream<T : Any, R : Any>(
    override val upstream: Stream<T>,
    override val description: String,
    private val transformer: suspend CoroutineScope.(source: ReceiveChannel<T>, dest: SendChannel<R>) -> Unit
) : NamedStreamOperator<R>() {
    override fun CoroutineScope.openSubscription(): ReceiveChannel<R> = produce {
        upstream.subscribe {
            transformer(this, channel)
        }
    }
}
