package com.beyondeye.reduks

/**
 * single method interface, mainly used because kotlin does not support yet type alias for function types
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#reducer
 */
interface Reducer<S> {
    fun reduce(state: S, action: Any): S
}
