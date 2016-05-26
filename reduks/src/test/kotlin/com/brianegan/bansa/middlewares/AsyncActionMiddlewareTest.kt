package com.brianegan.bansa.middlewares

import com.brianegan.bansa.SimpleStore
import com.brianegan.bansa.Reducer
import com.brianegan.bansa.StoreSubscriber
import org.assertj.core.api.Assertions
import org.junit.Test

/**
 * Created by Dario on 3/22/2016.
 */
class AsyncActionMiddlewareTest {
    class IncrementCounterAction;
    data class TestState(val counter:Int=0,val lastAsyncActionMessage: String = "none",val lastAsyncActionError: String? =null, val lastAsyncActionResult:Int?=null)
    val actionDifficultTag = "A very difficult mathematical problem"
    val actionDifficultError ="Sometimes difficult problems cannot be solved"
    val reducer = Reducer<TestState> { state, action ->
        var res: TestState? = null
        AsyncAction.ofType(actionDifficultTag, action)
                ?.onCompleted<Int> { payload ->
                    res = TestState(
                            lastAsyncActionMessage = actionDifficultTag,
                            lastAsyncActionError = null,
                            lastAsyncActionResult = payload
                    )

                }?.onFailed { error ->
                    res=TestState(
                            lastAsyncActionMessage = actionDifficultTag,
                             lastAsyncActionError = error.message,
                             lastAsyncActionResult = null)
        }
        if(action is IncrementCounterAction) res=state.copy(counter = state.counter + 1)
        res ?: state
    }
    @Test
    fun `test an async action for a very difficult and computation heavy operation`() {
        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(AsyncActionMiddleWare())

        //subscribe before dispatch!!
        store.subscribe (StoreSubscriber {
            with (store.state) {
                if (lastAsyncActionMessage != "none") {
                    Assertions.assertThat(lastAsyncActionMessage).isEqualTo(actionDifficultTag)
                    Assertions.assertThat(lastAsyncActionError).isNull()
                    Assertions.assertThat(lastAsyncActionResult).isEqualTo(2 + 2)
                }
            }
        }) //on state change
        val asyncAction = AsyncAction.Started(actionDifficultTag) { 2 + 2 }
        store.dispatch(asyncAction)
    }

    @Test
    fun `test an async action for a very difficult and computation heavy operation that fails`() {

        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(AsyncActionMiddleWare())

        //subscribe before dispatch!
        store.subscribe (
                StoreSubscriber {
                    with(store.state) {
                        if (lastAsyncActionMessage != "none") {
                            Assertions.assertThat(store.state.lastAsyncActionMessage).isEqualTo(actionDifficultTag)
                            Assertions.assertThat(store.state.lastAsyncActionError).isEqualTo(actionDifficultError)
                            Assertions.assertThat(store.state.lastAsyncActionResult).isNull()
                        }
                    }
                }
        )
        val asyncAction = AsyncAction.Started(actionDifficultTag) {
            throw Exception(actionDifficultError)
        }
        store.dispatch(asyncAction)


    }
    @Test
    fun `test that normal actions pass through the middleware`() {

        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(AsyncActionMiddleWare())


        //subscribe before dispatch!!
        store.subscribe (
                StoreSubscriber {
                    //on state change
                    Assertions.assertThat(store.state.counter).isEqualTo(1);
                    Assertions.assertThat(store.state.lastAsyncActionMessage).isEqualTo("none")
                    Assertions.assertThat(store.state.lastAsyncActionError).isNull()
                    Assertions.assertThat(store.state.lastAsyncActionResult).isNull()
                })
        store.dispatch(IncrementCounterAction())

    }
}

