package com.beyondeye.reduks

/**
 * single method interface, mainly used because kotlin does not support yet type alias for function types
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#middleware
 */
interface Middleware<S> {
    fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any
}
