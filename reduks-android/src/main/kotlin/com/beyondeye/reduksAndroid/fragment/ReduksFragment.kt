package com.beyondeye.reduksAndroid.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.beyondeye.reduks.Store
import com.beyondeye.reduks.StoreSubscriber
import com.beyondeye.reduks.dispatch
import com.beyondeye.reduksAndroid.activity.ReduksActivity

/**
 * base class for fragments associated to a [ReduksActivity], for
 * simplifying integration with reduks
 */
abstract class ReduksFragment<S: StateWithFragmentStatusData>:Fragment() {

    /**
     * if defined, then when the frament is shown, it will be marked in the reduks state as shown in
     * that position: see [FragmentStatusData]
     */
    abstract val positionTag:String
    /**
     * automatically unsubscribe from reduks state updates when the fragment view is destroyed
     */
    override fun onDestroyView() {
        reduksUnsubscribe()
        super.onDestroyView()
    }

    /**
     * this property is defined as "open" so that you can override it in a specific fragment and
     * cast the activity to the actual activity that control this fragment in case you need access  specific
     * fields or methods in the parent activity. This is not a recommended practice but sometimes it is needed
     */
    @Suppress("UNCHECKED_CAST")
    open val reduksActivity: ReduksActivity<S> = activity as ReduksActivity<S>

    val curReduksState: S
        get() = reduksActivity.reduks.store.state

    /**
     * automatically subscribe to reduks state updates once the fragment is created
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        @Suppress("UNCHECKED_CAST")
        (reduks?.store)?.let { reduksSubscribe(getFragmentStoreSubscriber(it as Store<S>)) }
}
    /**
     * define the reduks store subscriber to be activated when the fragment view is activated
     */
    abstract fun getFragmentStoreSubscriber(store: Store<S>): StoreSubscriber<S>


    /**
     * return true if the fragment is visible, by checking reduks state
     * use this method in fragment subscriber to decide the need to react to some state changes or not
     * when inside the fragment store subscriber
     * the default implementation will check if the fragment is marked as current in [FragmentStatusData]
     * for its defined [positionTag]
     */
     open fun isFragmentVisible(curState:S):Boolean {
        return positionTag.isNotEmpty() && tag==reduks?.fragmentCurAtPos(positionTag)
    }
}

/**
 * mark this fragment as the current fragment shown at the position defined by [ReduksFragment.positionTag]
 */
fun <S: StateWithFragmentStatusData> ReduksFragment<S>.reduksSetFragmentVisible() {
    reduks?.setFragmentCurAtPos(tag,positionTag)
}
