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

data class AState(val a:Int, val b:Int, override val busData: PMap<String, Any> = emptyBusData()) :StateWithBusData {
    override fun copyWithNewBusData(newBusData: PMap<String, Any>): StateWithBusData = copy(busData=newBusData)
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

fun Context.bindReduksFromParentActivity(): Reduks<out StateWithBusData>? =
        if (this is ReduksActivity<*>) {
            this.reduks as? Reduks<out StateWithBusData>
        } else {
            throw RuntimeException(this.toString() + " must implement ReduksActivity<out StateWithBusData>")
        }

class LoginFragment : Fragment() {
    private var reduks: Reduks<out StateWithBusData>?=null
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        reduks=context?.bindReduksFromParentActivity()
    }

    override fun onDetach() {
        super.onDetach()
        reduks=null
    }
    fun onSubmitLogin() {
        reduks?.postBusData(LoginFragmentResult("Kotlin","IsAwsome"))
    }
}

class LoginDataDisplayFragment : Fragment() {
    private var reduks: Reduks<out StateWithBusData>?=null
    val busHandlers:MutableList<StoreSubscription> = mutableListOf()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        reduks=context?.bindReduksFromParentActivity()
        reduks?.addBusDataHandler { lfr:LoginFragmentResult? ->
            if(lfr!=null) {
                print("login with username=${lfr.username} and password=${lfr.password} and ")
            }
        }?.addToList(busHandlers)
    }

    override fun onDetach() {
        super.onDetach()
        reduks?.removeBusDataHandlers(busHandlers)
        reduks=null
    }
}
