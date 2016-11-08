package com.beyondeye.reduks

import com.beyondeye.reduks.*

/**
 * builder methods for making the code more clear when defining reduks objects
 * Created by daely on 8/31/2016.
 */

/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#middleware
 */
class MiddlewareImpl<S>(val middlewareFn:(store: Store<S>, nextDispatcher:  (Any)->Any, action:Any)->Any): Middleware<S> {
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any =middlewareFn(store, nextDispatcher,action)
}

/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#reducer
 */
class ReducerImpl<S>(val reducerFn:(state:S,action:Any)->S) : Reducer<S>{
    override fun reduce(state: S, action: Any): S= reducerFn(state,action)
}

/**
 * get a store creator and return a new enhanced one
 * see https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store-enhancer
 * Created by daely on 8/23/2016.
 */
class StoreEnhancerImpl<S>(val storeEnhancerFn:(next: StoreCreator<S>)-> StoreCreator<S>): StoreEnhancer<S> {
    override fun enhance(next: StoreCreator<S>): StoreCreator<S> = storeEnhancerFn(next)
}

class StoreSubscriberImpl<S>(val subscriberFn: ()->Unit) : StoreSubscriber<S>{
    override fun onStateChange(){subscriberFn()}
}

class StoreSubscriberBuilderImpl<S>(val storeSubscriberBuilderFn:(store: Store<S>)-> StoreSubscriber<S>): StoreSubscriberBuilder<S> {
    override fun build(store: Store<S>): StoreSubscriber<S> = storeSubscriberBuilderFn(store)
}
class StoreSubscriberBuilderImpl2<S>(val storeSubscriberBuilderFn2:(store: Store<S>,selector:SelectorBuilder<S>)-> StoreSubscriber<S>): StoreSubscriberBuilder<S> {
    val selector=SelectorBuilder<S>()
    override fun build(store: Store<S>): StoreSubscriber<S> = storeSubscriberBuilderFn2(store,selector)
}

class ThunkImpl<S>(val thunkFn:(dispatcher: (Any)->Any, state: S)->Any) : Thunk<S> {
    override fun execute(dispatcher:  (Any)->Any, state: S): Any = thunkFn(dispatcher,state)
}


fun <S> MiddlewareFn(middlewareFn:(store: Store<S>, nextDispatcher:  (Any)->Any, action:Any)->Any) = MiddlewareImpl(middlewareFn)
fun <S> ReducerFn(reducerFn:(state:S, action:Any)->S) = ReducerImpl(reducerFn)
fun <S> StoreEnhancerFn(storeEnhancerFn:(next: StoreCreator<S>)-> StoreCreator<S>)= StoreEnhancerImpl(storeEnhancerFn)
fun <S> StoreSubscriberFn(subscriberFn: ()->Unit)= StoreSubscriberImpl<S>(subscriberFn)
fun <S> StoreSubscriberBuilderFn(storeSubscriberBuilderFn:(store: Store<S>)-> StoreSubscriber<S>) = StoreSubscriberBuilderImpl(storeSubscriberBuilderFn)
fun <S> StoreSubscriberBuilderFn(storeSubscriberBuilderFn2:(store: Store<S>,selector:SelectorBuilder<S>)-> StoreSubscriber<S>) = StoreSubscriberBuilderImpl2(storeSubscriberBuilderFn2)

fun <S> ThunkFn(thunkFn:(dispatcher:  (Any)->Any, state: S)->Any) = ThunkImpl(thunkFn)
