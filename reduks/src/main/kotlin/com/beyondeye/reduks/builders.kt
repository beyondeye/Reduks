package com.beyondeye.reduks

import com.beyondeye.reduks.*

/**
 * builder methods for making the code more clear when defining reduks objects
 * Created by daely on 8/31/2016.
 */

/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#middleware
 */
class MiddlewareImpl<S>(val middlewareFn:(store: Store<S>, nextDispatcher:  (Any)->Any, action:Any)->Any):IMiddleware<S> {
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any =middlewareFn(store, nextDispatcher,action)
}

/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#reducer
 */
class ReducerImpl<S>(val reducerFn:(state:S,action:Any)->S) :IReducer<S>{
    override fun reduce(state: S, action: Any): S= reducerFn(state,action)
}

/**
 * get a store creator and return a new enhanced one
 * see https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store-enhancer
 * Created by daely on 8/23/2016.
 */
class StoreEnhancerImpl<S>(val storeEnhancerFn:(next: StoreCreator<S>)-> StoreCreator<S>):IStoreEnhancer<S> {
    override fun enhance(next: StoreCreator<S>): StoreCreator<S> = storeEnhancerFn(next)
}

class StoreSubscriberImpl<S>(val subscriberFn: ()->Unit) :IStoreSubscriber<S>{
    override fun onStateChange(){subscriberFn()}
}

class StoreSubscriberBuilderImpl<S>(val storeSubscriberBuilderFn:(store: Store<S>)-> IStoreSubscriber<S>):IStoreSubscriberBuilder<S> {
    override fun build(store: Store<S>): IStoreSubscriber<S> = storeSubscriberBuilderFn(store)
}

class ThunkImpl<S>(val thunkFn:(dispatcher: (Any)->Any, state: S)->Any) : IThunk<S> {
    override fun execute(dispatcher:  (Any)->Any, state: S): Any = thunkFn(dispatcher,state)
}


fun <S> Middleware(middlewareFn:(store: Store<S>, nextDispatcher:  (Any)->Any, action:Any)->Any) = MiddlewareImpl(middlewareFn)
fun <S> Reducer( reducerFn:(state:S,action:Any)->S) = ReducerImpl(reducerFn)
fun <S> StoreEnhancer(storeEnhancerFn:(next: StoreCreator<S>)-> StoreCreator<S>)= StoreEnhancerImpl(storeEnhancerFn)
fun <S> StoreSubscriber(subscriberFn: ()->Unit)= StoreSubscriberImpl<S>(subscriberFn)
fun <S> StoreSubscriberBuilder(storeSubscriberBuilderFn:(store: Store<S>)-> IStoreSubscriber<S>) = StoreSubscriberBuilderImpl(storeSubscriberBuilderFn)
fun <S> Thunk( thunkFn:(dispatcher:  (Any)->Any, state: S)->Any) = ThunkImpl(thunkFn)

data class TestState(val a:Int, val b:Int)
val s1= StoreSubscriber<TestState>{ -> }
