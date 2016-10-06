import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.combineReducers

class Action
{
    class IncrA
    class IncrB
}
data class State(val a:Int=0,val b:Int=0)
val reducerA=ReducerFn<State>{ state,action-> when(action) {
    is Action.IncrA -> state.copy(a=state.a+1)
    else -> state
}}
val reducerB=ReducerFn<State>{ state,action-> when(action) {
    is Action.IncrB -> state.copy(b=state.b+1)
    else -> state
}}

val reducerAB=ReducerFn<State>{ state,action-> when(action) {
    is Action.IncrA -> state.copy(a=state.a*2)
    is Action.IncrB -> state.copy(b=state.b*2)
    else -> state
}}
val reducercombined= combineReducers(reducerA, reducerB, reducerAB)
