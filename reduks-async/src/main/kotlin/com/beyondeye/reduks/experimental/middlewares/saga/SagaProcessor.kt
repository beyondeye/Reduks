package com.beyondeye.reduks.experimental.middlewares.saga

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

class SagaYeldSingle<S:Any>(private val sagaProcessor: SagaProcessor<S>){
    suspend infix fun put(value:Any) {
        yieldSingle(SagaProcessor.Put(value))
    }


    suspend infix fun <B> takeEvery(process: Saga2<S>.(B) -> Any?)
    {
        yieldSingle( SagaProcessor.TakeEvery<S,B>(process))
    }
    //-----------------------------------------------
    suspend fun yieldSingle(value: Any) {
        sagaProcessor.inChannel.send(value)
    }
    suspend fun yieldBackSingle(): Any {
        return sagaProcessor.outChannel.receive()
    }
}
suspend inline fun <reified B> SagaYeldSingle<*>.take():B {
    yieldSingle(SagaProcessor.Take<B>(B::class.java))
    return yieldBackSingle() as B
}
class SagaYeldAll<S:Any>(private val sagaProcessor: SagaProcessor<S>){
    private suspend  fun yieldAll(inputChannel: ReceiveChannel<Any>) {
        for (a in inputChannel) {
            sagaProcessor.inChannel.send(a)
        }
    }
}

class Saga2<S:Any>(sagaProcessor: SagaProcessor<S>) {
    val yieldSingle= SagaYeldSingle(sagaProcessor)
    val yieldAll= SagaYeldAll(sagaProcessor)
}


class SagaProcessor<S:Any>(
        private val dispatcherActor: SendChannel<Any>
)
{
    class Put(val value:Any)
    class Take<B>(val type:Class<B>)
    class TakeEvery<S:Any,B>(val process: Saga2<S>.(B) -> Any?)
    class SagaFinished(val sm: SagaMiddleWare2<*>, val sagaName: String)

    /**
     * channel for communication between Saga and Saga processor
     */
    val inChannel : Channel<Any> = Channel()
    val outChannel : Channel<Any> = Channel()
    /**
     * channel where processor receive actions dispatched to the store
     */
    suspend fun start(inputActions: ReceiveChannel<Any>) {
        for(a in inChannel) {
            when(a) {
                is Put ->
                    dispatcherActor.send(a.value)
                is Take<*> -> {
//                    launch { //launch aynchronously, to avoid dead locks?
                        for(ia in inputActions) {
                            if(ia::class.java==a.type) {
                                outChannel.send(ia)
                                break
                            }
                        }
//                    }

                }
                is SagaFinished -> {
                    a.sm.sagaProcessorFinished(a.sagaName)
                    return
                }
                else ->  {
                    print("unsupported saga operation: ${a::class.java}")
                }
            }
        }
    }

    fun stop() {
        inChannel.close()
        outChannel.close()
    }
}