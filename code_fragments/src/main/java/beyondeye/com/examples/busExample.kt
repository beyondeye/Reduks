package beyondeye.com.examples

import android.content.Context
import android.support.v4.app.Fragment
import com.beyondeye.reduks.*
import com.beyondeye.reduks.bus.*
import com.beyondeye.reduks.pcollections.PMap
import com.beyondeye.reduksAndroid.activity.ReduksActivity

/**
 * Created by daely on 10/6/2016.
 */

data class AState(val a:Int, val b:Int, override val busData: BusData = BusData.empty) :StateWithBusData {
    override fun copyWithNewBusData(newBusData: BusData): StateWithBusData = copy(busData=newBusData)
}
val initialState=AState(0,0)
class Action
{
    class SetA(val newA:Int)
    class SetB(val newB:Int)
}
val reducer = ReducerFn<AState> { state, action ->
    when (action) {
        is Action.SetA -> state.copy(a= action.newA)
        is Action.SetB -> state.copy(b= action.newB)
        else -> state
    }
}

class LoginFragmentResult(val username:String, val password:String)

fun test() {
    val creator= SimpleStore.Creator<AState>()
    val store = creator.create(reducer, initialState, BusStoreEnhancer())
    store.addBusDataHandler { lfr:LoginFragmentResult? ->
        if(lfr!=null) {
            print("login with username=${lfr.username} and password=${lfr.password} and ")
        }
    }

    store.postBusData(LoginFragmentResult(username = "Kotlin", password = "IsAwsome"))

}
fun Fragment.reduks() =
        if (activity is ReduksActivity<*>)
            (activity as ReduksActivity<out StateWithBusData>).reduks
        else null

class LoginFragment : Fragment() {
    fun onSubmitLogin() {
        reduks()?.postBusData(LoginFragmentResult("Kotlin","IsAwsome"))
    }
}

class LoginDataDisplayFragment : Fragment() {
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        reduks()?.addBusDataHandlerWithTag(tag) { lfr:LoginFragmentResult? ->
            if(lfr!=null) {
                print("login with username=${lfr.username} and password=${lfr.password} and ")
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        reduks()?.removeBusDataHandlersWithTag(tag) //remove all bus data handler attached to this fragment tag
    }
}
