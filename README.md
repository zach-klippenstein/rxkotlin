# RxKotlin

A proof-of-concept of a cold streams library for Kotlin using coroutines.

## Types

### Stream
_From the kdoc, might be out-of-date._

A cold stream. The stream begins producing values once it is subscribed to.
Since streams are cold, every subscriber will get its own set of values.
A `Stream` is basically just a factory for [ReceiveChannel]s.

Streams cannot contain `null`s.

#### Creating streams

There are a number of builder functions that create streams for you.
 - [stream] – the most flexible of the builders. Accepts a coroutine builder block just like `produce`.
   The block is executed once for every subscriber.
   Its coroutine scope is a [SendChannel], and every send will block until the subscriber is ready to receive.
 - [streamOf] – the simplest of the builders. Takes a vararg list of values and will immediately emit all of
   them to its subscribers.
 - [Iterable.asStream], [Sequence.asStream] – return streams that emit their source's contents.

#### Subscribing to streams

Subscription is represented as a [ReceiveChannel]. The channel must be [canceled][ReceiveChannel.cancel]
to unsubscribe. From a suspend function, you can subscribe with:
 - [Stream.openSubscription] – gives you the raw channel and leaves you to close it when you're done.
 - [Stream.subscribe] – you pass a block that accepts the channel, and the channel will automatically
   be closed for you when you're done.

#### Operating on and composing streams

Streams can be manipulated using a wide variety of operators defined as extension functions.
All the standard functional operators are there: `map`, `flatMap`, `reduce`, etc.
You can also define your own operators. Operators that accept functions accept suspend functions.

All built-in non-terminal operators can be given a description string to help with debugging.
[Stream.dumpChain] will return a list of operators from most-downstream to most-upstream, with their
descriptions. If you have an operator that is composed of a number of other operators, you can wrap
them up so they appear as a single operator using [Stream.compose].
