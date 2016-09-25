package com.beyondeye.reduks

@SafeVarargs
fun <S> combineReducers(vararg reducers: IReducer<S>): IReducer<S> {
    return Reducer<S> {state: S, action: Any ->
        reducers.fold(state, { state, reducer -> reducer.reduce(state, action) })
//        var state = state
//        for (reducer in reducers) {
//            state = reducer.reduce(state, action)
//        }
//        state
    }
}