package com.zachklipp.rxkotlin.internal

import com.zachklipp.rxkotlin.Stream

internal abstract class NamedStreamOperator<T : Any> : Stream<T> {
    abstract val description: String
    override fun toString(): String = "${javaClass.simpleName}($description)@${hashCode()}"
}
