package com.zachklipp.rxkotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

fun runTest(block: suspend CoroutineScope.() -> Unit) = runBlocking(block = block)
