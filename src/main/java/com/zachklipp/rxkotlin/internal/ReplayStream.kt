@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.zachklipp.rxkotlin.internal

import com.zachklipp.rxkotlin.ConnectableStream
import com.zachklipp.rxkotlin.Stream
import com.zachklipp.rxkotlin.subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.concurrent.LinkedBlockingDeque

internal class ReplayStream<T : Any>(
    override val upstream: Stream<T>,
    private val n: Int
) : NamedStreamOperator<T>(), ConnectableStream<T> {
    override val description: String get() = "replay($n)"

    private var newSubscribers = Channel<SendChannel<T>>(UNLIMITED)
    // TODO this isn't thread-safe
    private var connection: Job? = null

    override fun CoroutineScope.openSubscription(): ReceiveChannel<T> {
        val channel = Channel<T>(RENDEZVOUS)
        newSubscribers.offer(channel)
        return channel
    }

    override fun CoroutineScope.connect(): Job {
        connection?.let { return it }

        return launch {
            subscribe {
                val source = this
                val subscribers = mutableListOf<SendChannel<T>>()
                val buffer: Deque<T> = LinkedBlockingDeque<T>(n)

                while (true) {
                    select<Unit> {
                        newSubscribers.onReceive { newSub ->
                            subscribers += newSub
                            newSub.invokeOnClose { subscribers -= newSub }
                            // Replay to new subscribers.
                            buffer.forEach { newSub.send(it) }
                        }
                        source.onReceive { value ->
                            // Evict items if buffer is full.
                            if (buffer.size > n) buffer.removeFirst()
                            buffer.addLast(value)
                            subscribers.forEach { it.send(value) }
                        }
                    }
                }
            }
        }.also { connection ->
            this@ReplayStream.connection = connection
            connection.invokeOnCompletion { this@ReplayStream.connection = null }
        }
    }
}
