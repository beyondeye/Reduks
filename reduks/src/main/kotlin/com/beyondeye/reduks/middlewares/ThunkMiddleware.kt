package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*

/**
 * a middleware that knows how to handle actions of type thunk
 * based on ideas from
 * https://github.com/gaearon/redux-thunk
 * and http://redux.js.org/docs/advanced/AsyncActions.html
 * and http://stackoverflow.com/questions/35411423/how-to-dispatch-a-redux-action-with-a-timeout/35415559#35415559
 * Created by daely on 5/17/2016.
 */

/**
 * Technically a thunk is an Action that is actually a function that return the actual action to be
 * dispatched to the State Store.
 * It has many use cases. See the tests for examples.
 * The ThunkFn middleware pass to the thunk as
 * arguments the Store main dispatcher and the current state. This allows for the use case of a thunk
 * that dispatch multiple conditional actions according to the current state.
 * This very useful for making it easy to implement state change sequence logic,
 * that would otherwise require  interactions between
 * action reducers and state change subscribers.
 * Another important use case is async actions: it is possible to manage all the multiple actions
 * and state changes associated with an async action (action started, action completed/failed) in
 * a single place, with a single thunk (see examples from tests)
 */

class ThunkMiddleware<S> : Middleware<S> {
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        if(action is Thunk<*>) {
            @Suppress("UNCHECKED_CAST")
            val a=(action as Thunk<S>).execute( { it-> store.dispatch(it)  } ,store.state)
            return nextDispatcher(a)
        }
        return  nextDispatcher(action)
    }

}
/*
//original javascript from
export default function thunkMiddleware({ dispatch, getState }) {
  return next => action => {
    if (typeof action === 'function') {
      return action(dispatch, getState);
    }

    return next(action);
  };
}
 */