package com.beyondeye.reduks

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/31/2016.
 */

/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#middleware
 */
class MiddlewareImpl<S>(val middlewareFn:(store: Store<S>, next: NextDispatcher, action:Any)->Any) {
    fun dispatch(store: Store<S>, next: NextDispatcher, action: Any): Any =middlewareFn(store,next,action)
}
class NextDispatcherImpl(val nextDispatcherFn:(action:Any)->Any) {
    fun dispatch(action: Any): Any = nextDispatcherFn(action)
}
/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#reducer
 */
class ReducerImpl<S>(val reducerFn:(state:S,action:Any)->S) {
    fun reduce(state: S, action: Any): S= reducerFn(state,action)
}

/**
 * get a store creator and return a new enhanced one
 * see https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store-enhancer
 * Created by daely on 8/23/2016.
 */
class StoreEnhancerImpl<S>(val storeEnhancerFn:(next: StoreCreator<S>)-> StoreCreator<S>) {
    fun enhance(next: StoreCreator<S>): StoreCreator<S> = storeEnhancerFn(next)
}

class StoreSubscriberImpl<S>(val subscriberFn: (newState:S)->Unit) {
    fun onStateChange(state: S){subscriberFn(state)}
}

class StoreSubscriberBuilderImpl<S>(val storeSubscriberBuilderFn:(store: Store<S>)-> StoreSubscriber<S>) {
    fun build(store: Store<S>): StoreSubscriber<S> = storeSubscriberBuilderFn(store)
}

class ThunkImpl<S>(val thunkFn:(dispatcher: NextDispatcher, state: S)->Any) : Action {
    fun execute(dispatcher: NextDispatcher, state: S): Any = thunkFn(dispatcher,state)
}

fun <S> _Middleware(middlewareFn:(store: Store<S>, next: NextDispatcher, action:Any)->Any) = MiddlewareImpl(middlewareFn)
fun <S> _NextDispatcher(nextDispatcherFn:(action:Any)->Any) = NextDispatcherImpl(nextDispatcherFn)
fun <S> _Reducer( reducerFn:(state:S,action:Any)->S) = ReducerImpl(reducerFn)
fun <S> _StoreEnhancer(storeEnhancerFn:(next: StoreCreator<S>)-> StoreCreator<S>)= StoreEnhancerImpl(storeEnhancerFn)
fun <S> _StoreSubcriber(subscriberFn: (newState:S)->Unit)= StoreSubscriberImpl(subscriberFn)
fun <S> _StoreSubscriberBuilder(storeSubscriberBuilderFn:(store: Store<S>)-> StoreSubscriber<S>) = StoreSubscriberBuilderImpl(storeSubscriberBuilderFn)
fun <S> _Thunk( thunkFn:(dispatcher: NextDispatcher, state: S)->Any) = ThunkImpl(thunkFn)

data class TestState(val a:Int, val b:Int)
val s1= _StoreSubcriber<TestState>{ it-> }
