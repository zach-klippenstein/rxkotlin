@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin.internal

import com.zachklipp.rxkotlin.ConnectableStream
import com.zachklipp.rxkotlin.Stream
import com.zachklipp.rxkotlin.toChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

internal class MultiCastStream<T : Any>(
    override val upstream: Stream<T>
) : NamedStreamOperator<T>(), ConnectableStream<T> {
    override val description: String get() = "publish"

    // TODO verify BroadcastChannel actually works like I think it does.
    private val broadcaster = BroadcastChannel<T>(1)
    // TODO this isn't thread-safe
    private var connection: Job? = null

    override fun CoroutineScope.openSubscription(): ReceiveChannel<T> =
        broadcaster.openSubscription()

    override fun CoroutineScope.connect(): Job {
        // Short circuit if already connected.
        connection?.let { return it }
        return launch {
            upstream.toChannel(broadcaster)
        }.also { job ->
            connection = job
            job.invokeOnCompletion { connection = null }
        }
    }
}
