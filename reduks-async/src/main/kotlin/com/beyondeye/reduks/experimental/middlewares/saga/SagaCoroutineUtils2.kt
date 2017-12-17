package com.beyondeye.reduks.experimental.middlewares.saga

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

/**
 * Created by daely on 12/16/2017.
 */
/**
 * Launches new coroutine to produce a stream of values by sending them to a channel
 * and returns a reference to the coroutine as a [ReceiveChannel]. This resulting
 * object can be used to [receive][ReceiveChannel.receive] elements produced by this coroutine.
 *
 * The scope of the coroutine contains [ProducerScope] interface, which implements
 * both [CoroutineScope] and [SendChannel], so that coroutine can invoke
 * [send][SendChannel.send] directly. The channel is [closed][SendChannel.close]
 * when the coroutine completes.
 * The running coroutine is cancelled when its receive channel is [cancelled][ReceiveChannel.cancel].
 *
 * The [context] for the new coroutine can be explicitly specified.
 * See [CoroutineDispatcher] for the standard context implementations that are provided by `kotlinx.coroutines`.
 * The [context][CoroutineScope.context] of the parent coroutine from its [scope][CoroutineScope] may be used,
 * in which case the [Job] of the resulting coroutine is a child of the job of the parent coroutine.
 * The parent job may be also explicitly specified using [parent] parameter.
 *
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [DefaultDispatcher] is used.
 *
 * Uncaught exceptions in this coroutine close the channel with this exception as a cause and
 * the resulting channel becomes _failed_, so that any attempt to receive from such a channel throws exception.
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for newly created coroutine.
 *
 * @param context context of the coroutine. The default value is [DefaultDispatcher].
 * @param outputChannel the output channel *DARIO* this is passed from outside
 * @param parent explicitly specifies the parent job, overrides job from the [context] (if any).*
 * @param block the coroutine code.
 */
public fun <E> produceToChannel(
        context: CoroutineContext = DefaultDispatcher,
        inputActions: Channel<E>,
        outputChannel:Channel<E>,
        parent: Job? = null,
        block: suspend SagaScope2<E>.() -> Unit
): ReceiveChannel<E> {
    val newContext = newCoroutineContext(context, parent)
    val coroutine = ProducerCoroutineCustom2(newContext, inputActions, outputChannel)
    coroutine.initParentJob(newContext[Job])
    block.startCoroutine(coroutine, coroutine)
    return coroutine
}

// * @param capacity capacity of the channel's buffer (no buffer by default).
//val channel = Channel<E>(capacity)


private class ProducerCoroutineCustom2<E>(parentContext: CoroutineContext, override val inputActions:Channel<E>, outputChannel: Channel<E>) :
        ChannelCoroutineCustom2<E>(parentContext, inputActions, outputChannel, active = true), ProducerScope<E>, SagaScope2<E>,ProducerJob<E> {
}

internal open class ChannelCoroutineCustom2<E>(
        parentContext: CoroutineContext,
        private val _inputChannel:Channel<E>,
        private val _channel: Channel<E>,
        active: Boolean
) : AbstractCoroutine<Unit>(parentContext, active), Channel<E> by _channel {
    val channel: Channel<E>
        get() = this

    override fun onCancellation(exceptionally: CompletedExceptionally?) {
        val cause = exceptionally?.cause
        if (!_channel.cancel(cause) && cause != null)
            handleCoroutineException(context, cause)
    }

    override fun cancel(cause: Throwable?): Boolean = super.cancel(cause)
}