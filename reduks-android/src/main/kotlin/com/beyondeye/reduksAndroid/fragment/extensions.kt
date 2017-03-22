package com.beyondeye.reduksAndroid.fragment

import android.support.v4.app.Fragment
import com.beyondeye.reduks.*
import com.beyondeye.reduks.bus.*
import com.beyondeye.reduks.modules.MultiStore
import com.beyondeye.reduksAndroid.activity.ReduksActivity

//----------------------------
/**
 * helper extension method for building a selector for fragment status for some specific fragment
 */
fun SelectorBuilder<StateWithFragmentStatusData>.withFragmentStatus(fragmentTag: String) =
        withSingleField { fragmentStatus[fragmentTag]?: FragmentStatus.fragmentStatusNotCreated }

//----------------------------
fun StateWithFragmentStatusData.isFragmentActive(fragmentTag:String):Boolean {
    val status=fragmentStatus[fragmentTag] ?: FragmentStatus.fragmentStatusNotCreated
    return status.isActive()
}

fun StateWithFragmentStatusData.isFragmentInactive(fragmentTag:String):Boolean {
    val status=fragmentStatus[fragmentTag] ?: FragmentStatus.fragmentStatusNotCreated
    return status.isInactive()
}
fun StateWithFragmentStatusData.isFragmentNotCreated(fragmentTag:String):Boolean {
    val status=fragmentStatus[fragmentTag] ?: FragmentStatus.fragmentStatusNotCreated
    return status.isNotCreated()
}
//--------------------
//some basic utility extension methods

/**
 * for multistore, use one of the substores as StateWithFragmentStatusData
 */
fun  MultiStore.subStoreWithFragmentStatus(): Store<out StateWithFragmentStatusData>?{
    @Suppress("UNCHECKED_CAST")
    for (st in storeMap.values) {
        val t=st as? Store<out StateWithFragmentStatusData>
        if(t!=null) return t
    }
    return null
}
//-------
fun  MultiStore.fragmentStatus(fragmentTag:String):FragmentStatus? {
    return subStoreWithFragmentStatus()?.fragmentStatus(fragmentTag)
}
fun  Store<out Any>.fragmentStatus(fragmentTag:String):FragmentStatus? {
    val multifs=(this as? MultiStore)?.fragmentStatus(fragmentTag)
     if(multifs!=null) return multifs
    @Suppress("UNCHECKED_CAST")
     val st=this as? Store<out StateWithFragmentStatusData>
     if(st==null) return null
    return st.state.fragmentStatus.get(fragmentTag) ?: FragmentStatus.fragmentStatusNotCreated
}
fun  Reduks<out Any>.fragmentStatus(fragmentTag:String):FragmentStatus?=store.fragmentStatus(fragmentTag)


//-----------
fun  MultiStore.setFragmentStatus(fragmentTag:String,fragmentStatus:FragmentStatus) {
    val s= subStoreWithFragmentStatus()
    s?.setFragmentStatus(fragmentTag,fragmentStatus)
}

fun  Store<out Any>.setFragmentStatus(fragmentTag:String,newStatus: FragmentStatus) {
    if(this is MultiStore)
        (this as MultiStore).setFragmentStatus(fragmentTag,newStatus)
    else
        dispatch(ActionSetFragmentStatus(fragmentTag,newStatus))
}
fun  Reduks<out Any>.setFragmentStatus( fragmentTag:String,newStatus: FragmentStatus) {
    store.setFragmentStatus(fragmentTag,newStatus)
}

//------------------
val Fragment.reduks: Reduks<out Any>? get() {
    val fsa=activity as? ReduksActivity<*>
    @Suppress("UNCHECKED_CAST")
    return (fsa?.reduks as? Reduks<out Any>)
}
/**
 * TO be called in Fragment OnViewCreated, for adding a reduks subscriber specific for this fragment
 */
fun <S>Fragment.reduksSubscribe(setFragmentStatusActive:Boolean,storeSubscriberBuilder:StoreSubscriberBuilder<S>) {
    if(setFragmentStatusActive) reduks?.setFragmentStatus(tag, FragmentStatus.fragmentStatusActive)
    @Suppress("UNCHECKED_CAST")
    val fsa=activity as? ReduksActivity<S>
    fsa?.reduks?.subscribe(tag,storeSubscriberBuilder)
}
/**
 * TO be called in Fragment OnViewCreated, for detaching the bus data handler/reduks subscriber associated to this fragment
 */
fun <S>Fragment.reduksSubscribe(setFragmentStatusActive:Boolean,storeSubscriber:StoreSubscriber<S>) {
    if(setFragmentStatusActive) reduks?.setFragmentStatus(tag, FragmentStatus.fragmentStatusActive)
    @Suppress("UNCHECKED_CAST")
    val fsa=activity as? ReduksActivity<S>
    @Suppress("UNCHECKED_CAST")
    fsa?.reduks?.subscribe(tag,storeSubscriber)
}

/**
 * TO be called in Fragment OnDestroyView
 */
fun Fragment.reduksUnsubscribe(setFragmentStatusInactive:Boolean) {
    if(setFragmentStatusInactive) reduks?.setFragmentStatus(tag, FragmentStatus.fragmentStatusInactive)
    reduks?.apply {
       unsubscribe(tag)
       removeBusDataHandlers(tag)
    }
}

inline fun <reified BusDataType:Any> ReduksActivity<out Any>.reduksAddBusDataHandler(noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? =
        reduks.AddBusDataHandler(reduks.ctx.toString(),true,null,fn)

/**
 * use fragment tag as subscription tag and clear busdata after handling
 * fragment store subscribers can be destroyed and then get notifications from old bus data so by default we
 * should clear bus data after handling it
 */
inline fun <reified BusDataType:Any> Fragment.reduksAddBusDataHandler(noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? =
        reduks?.AddBusDataHandler(tag,true,null,fn)

fun Fragment.reduksDispatch(action:Any) {
//    LogRdks("dispatch of $action").before
    reduks?.dispatch(action)
//    LogRdks("dispatch of $action").after
}

inline fun <reified BusDataType :Any> Fragment.reduksPostBusData(data: BusDataType, key:String?=null) {
    reduks?.postBusData(data,key)
}
inline fun <reified BusDataType :Any> Fragment.reduksClearBusData(key:String?=null) {
    reduks?.clearBusData<BusDataType>(key)
}