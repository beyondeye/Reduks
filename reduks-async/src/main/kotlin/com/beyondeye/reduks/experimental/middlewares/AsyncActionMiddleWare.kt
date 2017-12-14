package com.beyondeye.reduks.experimental.middlewares

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async


sealed class AsyncAction(val payloadTypename:String): Action {
    class AsyncActionMatcher<PayloadType>(val action:AsyncAction) {
        fun onStarted(body: () -> Unit): AsyncActionMatcher<PayloadType>? {
            if(action is AsyncAction.Started<*>)
                body()
            return this
        }
        @Suppress("UNCHECKED_CAST")
        inline fun  onCompleted(body: (value: PayloadType) -> Unit):  AsyncActionMatcher<PayloadType> {
            if(action is AsyncAction.Completed<*>)
                body(action.payload as PayloadType)
            return this
        }
        fun onFailed(body: (error: Throwable) -> Unit):  AsyncActionMatcher<PayloadType> {
            if(action is AsyncAction.Failed)
                body(action.error)
            return this
        }
    }

    /**
     * it seems redundant to store both type name and define this class as template class
     * unfortunately this is required because of type erasure in java/kotlin generics
     */
    class Started<PayloadType :Any>(payloadTypename:String, val promise: Deferred<PayloadType>): AsyncAction(payloadTypename) {
        constructor(type:String,body: () -> PayloadType):this(type, async { body() })
        suspend fun asCompleted() = Completed(payloadTypename, promise.await())
        suspend fun asFailed() = Failed(payloadTypename, promise.getCompletionExceptionOrNull()!!)
        /**
         * block until we get back the result from the promise
         */
        suspend fun resolve(): AsyncAction {
            val res = try {
                Completed(payloadTypename, promise.await())
            } catch (e:Exception) {
                Failed(payloadTypename, e)
            }
            return res
        }
    }
    class Completed<PayloadType>(payloadTypename:String, val payload: PayloadType): AsyncAction(payloadTypename)
    class Failed(payloadTypename:String, val error:Throwable): AsyncAction(payloadTypename)
    companion object {
        inline fun <reified PayloadType:Any > withPayload(action: Any):AsyncActionMatcher<PayloadType>? {
            if(action !is AsyncAction) return null
            val expectedname=PayloadType::class.java.canonicalName //use Payload type name from java reflection to identify async action
            if (action.payloadTypename !=expectedname) return null
            return AsyncActionMatcher<PayloadType>(action)
        }
        inline fun <reified  PayloadType:Any> start( promise: Deferred<PayloadType>):AsyncAction {
            return Started<PayloadType>(PayloadType::class.java.canonicalName,promise)
        }
        inline fun <reified  PayloadType:Any> start(noinline  body: () -> PayloadType):AsyncAction {
            return Started<PayloadType>(PayloadType::class.java.canonicalName,body)
        }
    }
}


/**
 * a middleware that knows how to handle actions of type [AsyncAction]
 * based on ideas from
 * https://github.com/acdlite/redux-promise
 * https://github.com/acdlite/flux-standard-action
 * But do not return the unwrapped promise to the client (the external caller to store.dispatch),
 * ( because not very useful for chaining: we need to cast the result back to Promise, kotlin is not like javascript!!!)
 * Actually, because of this returning the action from the middleware is less useful than in javascript. TODO need to think about it
 *
 * Created by daely on 5/17/2016.
 */
class AsyncActionMiddleWare<S> : Middleware<S> {
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        if(action is AsyncAction.Started<*>) {
            //queue some async actions when the promise resolves
            async {
                try {
                    action.promise.await()
                    store.dispatch(action.asCompleted())
                } catch (e:Exception) {
                    store.dispatch(action.asFailed())
                }
            }
            //do not pass back the unwrapped promise as result of the middleware like is done for example in https://github.com/acdlite/redux-promise
            //because not very useful for chaining .since we need anyway to cast back to the right type, just return the AsyncAction.Started<A> itself
        }
        return nextDispatcher(action)
    }

}
/* ORIGINAL JAVASCRIPT CODE FROM https://github.com/acdlite/redux-promise
function isPromise(val) {
  return val && typeof val.then === 'function';
}

export default function promiseMiddleware({ dispatch }) {
  return next => action => {
    if (!isFSA(action)) {
      return isPromise(action)
        ? action.then(dispatch)
        : next(action);
    }

    return isPromise(action.payload)
      ? action.payload.then(
          result => dispatch({ ...action, payload: result }),
          error => dispatch({ ...action, payload: error, error: true })
        )
      : next(action);
  };
}
 */