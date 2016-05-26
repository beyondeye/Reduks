package com.brianegan.bansa

@SafeVarargs
fun <S> combineReducers(vararg reducers: Reducer<S>): Reducer<S> {
    return Reducer<S> {state: S, action: Any ->
        reducers.fold(state, { state, reducer -> reducer.reduce(state, action) })
//        var state = state
//        for (reducer in reducers) {
//            state = reducer.reduce(state, action)
//        }
//        state
    }
}