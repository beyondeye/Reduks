package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.Middleware
import com.beyondeye.reduks.NextDispatcher
import com.beyondeye.reduks.Store
import nl.komponents.kovenant.task
import nl.komponents.kovenant.Promise


sealed class AsyncAction(val type:String) {
    class Started<PayloadType :Any>(type:String, promise: Promise<PayloadType, Throwable>): AsyncAction(type) {
        val promise: Promise<PayloadType, Throwable> = promise
        constructor(type:String,body: () -> PayloadType):this(type, task { body() })
        fun asCompleted() = Completed(type, promise.get())
        fun asFailed() = Failed(type, promise.getError())
        /**
         * block until we get back the result from the promise
         */
        fun resolve(): AsyncAction {
            val res: AsyncAction
            try {
                res= Completed(type, promise.get())
            } catch (e:Exception) {
                res= Failed(type, e)
            }
            return res
        }
    }
    class Completed(type:String, val payload: Any): AsyncAction(type)
    class Failed(type:String, val error:Throwable): AsyncAction(type)
    fun onStarted(body: () -> Unit): AsyncAction {
        if(this is AsyncAction.Started<*>)
            body()
        return this
    }
    inline fun <reified PayloadType> onCompleted(body: (value: PayloadType) -> Unit): AsyncAction {
        if(this is AsyncAction.Completed)
            body(this.payload as PayloadType)
        return this
    }
    fun onFailed(body: (error: Throwable) -> Unit): AsyncAction {
        if(this is AsyncAction.Failed)
            body(this.error)
        return this
    }
    companion object {
        fun ofType(type: String, action: Any):AsyncAction? {
            if(action !is AsyncAction) return null
            if (action.type!=type) return null
            return action
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
    override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any):Any {
        if(action is AsyncAction.Started<*>) {
            //queue some async actions when the promise resolves
            action.promise
                    .success { store.dispatch(action.asCompleted()) }
                    .fail { store.dispatch(action.asFailed()) }
            //do not pass back the unwrapped promise as result of the middleware like is done for example in https://github.com/acdlite/redux-promise
            //because not very useful for chaining .since we need anyway to cast back to the right type, just return the AsyncAction.Started<A> itself
        }
        return next(action)
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