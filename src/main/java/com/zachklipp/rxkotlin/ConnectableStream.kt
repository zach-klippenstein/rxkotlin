package com.zachklipp.rxkotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * A stream that will only subscribe upstream once, no matter how many downstream
 * subscribers there are.
 */
interface ConnectableStream<T : Any> : Stream<T> {
    /**
     * Connect upstream. The returned [Job] must be canceled to unsubscribe upstream.
     *
     * The resulting connection Job is a child of the current coroutine scope.
     *
     * If the stream is already connected, calling this method does nothing and returns the
     * existing Job.
     */
    fun CoroutineScope.connect(): Job
}
