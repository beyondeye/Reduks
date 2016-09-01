package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import org.junit.Test
import org.assertj.core.api.Assertions

/**
 * Created by daely on 5/22/2016.
 */
class ThunkMiddlewareTest {
    class SetLastMathResult(val result:Int)
    class IncrementCounter
    data class TestState1(val counter:Int=0, val lastMathResult:Int)

    val reducer1 = Reducer<TestState1> { state, action ->
        when (action) {
            is SetLastMathResult -> state.copy(lastMathResult = action.result)
            is IncrementCounter -> state.copy(counter = state.counter + 1)
            else -> state
        }
    }
    @Test
    fun testSimpleMathFunctionThunk() {
        val store = KovenantStore(TestState1(0, 0), reducer1,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.applyMiddleware(ThunkMiddleware<TestState1>())

        store.dispatch(Thunk<TestState1> {
            dispatcher, state ->
            SetLastMathResult(2 + 2) //return this action
        })
        Assertions.assertThat(store.state.lastMathResult).isEqualTo(2+2)

        //check that a regular action pass through the middleware
        store.dispatch(IncrementCounter())
        Assertions.assertThat(store.state.counter).isEqualTo(1)
    }

    //a simulated async action
    fun fetchSecretSauce(sauceName:String):String  {
        return "($sauceName)"
    }
    class MakeSandwitch(val forPerson:String,val sauceName:String)
    data class TestState2(val counter:Int=0, val forPerson:String="",val sauceName:String="")
    val reducer2 = Reducer<TestState2> { state, action ->
        when (action) {
            is MakeSandwitch -> state.copy(forPerson = action.forPerson, sauceName = action.sauceName)
            is IncrementCounter -> state.copy(counter = state.counter + 1)
            else -> state
        }
    }
    @Test
    fun testAsyncActionsThunk() {
        val store = KovenantStore(TestState2(), reducer2,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.applyMiddleware(ThunkMiddleware<TestState2>())
        val thunk= Thunk<TestState2> { dispatcher, state ->
            val promise: Promise<Any, Exception> = task {
                fetchSecretSauce("Tomato")
            }.then { sauce ->
                dispatcher.dispatch(
                        MakeSandwitch(forPerson = "John", sauceName = sauce))
            }
            promise
        }
        //subscribe before dispatch!!
        store.subscribe(StoreSubscriber {
            val s=store.state
            if (s.sauceName != "") {
                Assertions.assertThat(s.sauceName).isEqualTo("(Tomato)")
                Assertions.assertThat(s.forPerson).isEqualTo("John")
            }
        })
        //check that we can get back the promise from dispath
        val promise=store.dispatch(thunk) as Promise<Any,Throwable>
        val ms=promise.get() as MakeSandwitch
        Assertions.assertThat(ms.sauceName).isEqualTo("(Tomato)")
        Assertions.assertThat(ms.forPerson).isEqualTo("John")

    }
    }