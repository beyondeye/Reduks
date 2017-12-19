package com.beyondeye.reduks.experimental.middlewares.saga

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit

class SagaYeldSingle<S:Any>(private val sagaProcessor: SagaProcessor<S>){
    suspend infix fun put(value:Any) {
        _yieldSingle(OpCode.Put(value))
    }
    suspend infix fun delay(timeMsecs: Long):Any {
        return _yieldSingle(OpCode.Delay(timeMsecs))
    }


    suspend infix fun <B> takeEvery(process: Saga2<S>.(B) -> Any?)
    {
        _yieldSingle( OpCode.TakeEvery<S,B>(process))
    }
    //-----------------------------------------------
    suspend fun _yieldSingle(opcode: OpCode):Any {
        sagaProcessor.inChannel.send(opcode)
        if(opcode !is OpCode.OpCodeWithResult) return Unit
        return sagaProcessor.outChannel.receive()
    }
}
suspend inline fun <reified B> SagaYeldSingle<*>.take():B {
    return _yieldSingle(OpCode.Take<B>(B::class.java)) as B
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
sealed class OpCode {
    open class OpCodeWithResult:OpCode()
    class Delay(val time: Long,val unit: TimeUnit = TimeUnit.MILLISECONDS):OpCodeWithResult()
    class Put(val value:Any): OpCode()
    class Take<B>(val type:Class<B>): OpCodeWithResult()
    class TakeEvery<S:Any,B>(val process: Saga2<S>.(B) -> Any?): OpCode()
    class SagaFinished(val sm: SagaMiddleWare2<*>, val sagaName: String): OpCode()
}

class SagaProcessor<S:Any>(
        private val dispatcherActor: SendChannel<Any>
)
{


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
                is OpCode.Delay -> {
                    delay(a.time,a.unit)
                    outChannel.send(Unit)
                }
                is OpCode.Put ->
                    dispatcherActor.send(a.value)
                is OpCode.Take<*> -> {
//                    launch { //launch aynchronously, to avoid dead locks?
                        for(ia in inputActions) {
                            if(ia::class.java==a.type) {
                                outChannel.send(ia)
                                break
                            }
                        }
//                    }
                }
                is OpCode.SagaFinished -> {
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