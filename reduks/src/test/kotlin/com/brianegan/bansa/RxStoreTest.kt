package com.brianegan.bansa

import com.brianegan.bansa.rx.RxStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import rx.observers.TestSubscriber

class RxStoreTest {
    data class MyState(val state: String = "initial state")
    data class MyAction(val type: String = "unknown")

    @Test
    fun `when an action is fired, the corresponding reducer should be called and update the state of the application`() {
        val reducer = Reducer<MyState> { state, action->
            if (action !is MyAction) state
            else
                when (action.type) {
                    "to reduce" -> MyState("reduced")
                    else -> state
                }
        }
        val store = RxStore(MyState(), reducer)

        store.dispatch(MyAction(type = "to reduce"))

        assertThat(store.state.state).isEqualTo("reduced")
    }

    @Test
    fun `when two reducers are combined, and a series of actions are fired, the correct reducer should be called`() {
        val helloReducer1 = "helloReducer1"
        val helloReducer2 = "helloReducer2"

        val reducer1 = Reducer<MyState>{ state, action ->
            if (action is MyAction)
                when (action.type) {
                    helloReducer1 -> MyState("oh hai")
                    else -> state
                }
            else state
        }

        val reducer2 = Reducer<MyState>{ state, action ->
            if (action is MyAction)
                when (action.type) {
                    helloReducer2 -> MyState("mark")
                    else -> state
                }
            else state
        }

        val store = RxStore(MyState(), combineReducers(reducer1, reducer2))

        store.dispatch(MyAction(type = helloReducer1))
        assertThat(store.state.state).isEqualTo("oh hai")
        store.dispatch(MyAction(type = helloReducer2))
        assertThat(store.state.state).isEqualTo("mark")
    }

    @Test
    fun `subscribers should be notified when the state changes`() {
        val store = RxStore(MyState(), Reducer<MyState>{ state, action -> MyState() })
        val subscriber1 = TestSubscriber.create<MyState>()
        val subscriber2 = TestSubscriber.create<MyState>()

        store.subscribe(subscriber1)
        store.subscribe(subscriber2)

        store.dispatch(MyAction())

        assertThat(subscriber1.onNextEvents.size).isGreaterThan(0)
        assertThat(subscriber2.onNextEvents.size).isGreaterThan(0)
    }

    @Test
    fun `the store should not notify unsubscribed objects`() {
        val store = RxStore(MyState(), Reducer<MyState>{ state, action -> MyState() })
        val subscriber1 = TestSubscriber.create<MyState>()
        val subscriber2 = TestSubscriber.create<MyState>()

        store.subscribe(subscriber1)
        val subscription = store.subscribe(subscriber2)
        subscription.unsubscribe()

        store.dispatch(MyAction())

        assertThat(subscriber1.onNextEvents.size).isGreaterThan(0)
        assertThat(subscriber2.onNextEvents.size).isEqualTo(0)
    }
}

