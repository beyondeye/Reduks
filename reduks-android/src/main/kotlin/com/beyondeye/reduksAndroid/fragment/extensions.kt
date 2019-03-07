package com.beyondeye.reduksAndroid.fragment

import androidx.fragment.app.Fragment
import com.beyondeye.reduks.*
import com.beyondeye.reduks.bus.*
import com.beyondeye.reduks.modules.MultiStore
import com.beyondeye.reduksAndroid.activity.ReduksActivity

//----------------------------
/**
 * helper extension method for building a selector for fragment [FragmentStatusData] for some specific fragment
 */
fun SelectorBuilder<StateWithFragmentStatusData>.withFragmentActiveStatus(fragmentTag: String) =
        withSingleField { fragmentStatus.getFragmentActiveStatus(fragmentTag)?: FragmentActiveStatus.fragmentStatusNotCreated }

/**
 * helper extension method for building a selector for fragment tag current at some specified position [positionTag]
 */
fun SelectorBuilder<StateWithFragmentStatusData>.withFragmentCurAtPos(positionTag: String) =
        withSingleField { fragmentStatus.getFragmentCurAtPos(positionTag) }

//----------------------------
fun StateWithFragmentStatusData.isFragmentActive(fragmentTag:String):Boolean {
    val activeStatus=fragmentStatus.getFragmentActiveStatus(fragmentTag) ?: FragmentActiveStatus.fragmentStatusNotCreated
    return activeStatus.isActive()
}

fun StateWithFragmentStatusData.isFragmentInactive(fragmentTag:String):Boolean {
    val activeStatus=fragmentStatus.getFragmentActiveStatus(fragmentTag) ?: FragmentActiveStatus.fragmentStatusNotCreated
    return activeStatus.isInactive()
}
fun StateWithFragmentStatusData.isFragmentNotCreated(fragmentTag:String):Boolean {
    val activeStatus=fragmentStatus.getFragmentActiveStatus(fragmentTag) ?: FragmentActiveStatus.fragmentStatusNotCreated
    return activeStatus.isNotCreated()
}

fun StateWithFragmentStatusData.fragmentCurAtPos(positionTag:String):String {
    return fragmentStatus.getFragmentCurAtPos(positionTag)
}
//---------------------
//extensions methods related to [FragmentStatusData]
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
fun  MultiStore.fragmentActiveStatus(fragmentTag:String):FragmentActiveStatus? {
    return subStoreWithFragmentStatus()?.fragmentActiveStatus(fragmentTag)
}
fun MultiStore.fragmentCurAtPos(positionTag: String):String? {
    return subStoreWithFragmentStatus()?.fragmentCurAtPos(positionTag)
}

fun  Store<out Any>.fragmentActiveStatus(fragmentTag:String):FragmentActiveStatus? {
    val multifs=(this as? MultiStore)?.fragmentActiveStatus(fragmentTag)
     if(multifs!=null) return multifs
    @Suppress("UNCHECKED_CAST")
     val st=this as? Store<out StateWithFragmentStatusData>
     if(st==null) return null
    return st.state.fragmentStatus.getFragmentActiveStatus(fragmentTag) ?: FragmentActiveStatus.fragmentStatusNotCreated
}

/**
 * return the current fragment tag for the [positionTag] as defined in [ReduksFragment.positionTag]
 */
fun  Store<out Any>.fragmentCurAtPos(positionTag:String):String {
    val multifp=(this as? MultiStore)?.fragmentCurAtPos(positionTag)
    if(multifp!=null) return multifp
    @Suppress("UNCHECKED_CAST")
    val st=this as? Store<out StateWithFragmentStatusData>
    if(st==null) return ""
    return st.state.fragmentStatus.getFragmentCurAtPos(positionTag)
}

fun  Reduks<out Any>.fragmentActiveStatus(fragmentTag:String):FragmentActiveStatus?=store.fragmentActiveStatus(fragmentTag)

/**
 * return the fragment tag for the [positionTag] as defined in [ReduksFragment.positionTag]
 */
fun  Reduks<out Any>.fragmentCurAtPos(positionTag:String):String?=store.fragmentCurAtPos(positionTag)

//-----------
fun  MultiStore.setFragmentActiveStatus(fragmentTag:String, fragmentStatus:FragmentActiveStatus) {
    val s= subStoreWithFragmentStatus()
    s?.setFragmentActiveStatus(fragmentTag,fragmentStatus)
}
//-----------
fun  MultiStore.setFragmentCurAtPos(newFragmentTag:String,positionTag:String) {
    val s= subStoreWithFragmentStatus()
    s?.setFragmentCurAtPos(newFragmentTag,positionTag)
}

fun  Store<out Any>.setFragmentActiveStatus(fragmentTag:String, newStatus: FragmentActiveStatus) {
    if(this is MultiStore)
        (this as MultiStore).setFragmentActiveStatus(fragmentTag,newStatus)
    else
        dispatch(ActionSetFragmentActiveStatus(fragmentTag,newStatus))
}

fun  Store<out Any>.setFragmentCurAtPos(newFragmentTag:String,positionTag:String) {
    if(this is MultiStore)
        (this as MultiStore).setFragmentCurAtPos(newFragmentTag,positionTag)
    else
        dispatch(ActionSetFragmentCurAtPos(newFragmentTag,positionTag))
}


fun requireFragmentTagNotNull(fragmentTag: String?) {
    if (fragmentTag == null)
        throw IllegalArgumentException("Android Fragment Tag Is Not Set")
}

fun  Reduks<out Any>.setFragmentActiveStatus(fragmentTag:String?, newStatus: FragmentActiveStatus) {
    requireFragmentTagNotNull(fragmentTag)
    store.setFragmentActiveStatus(fragmentTag!!,newStatus)
}

fun  Reduks<out Any>.setFragmentCurAtPos( newfragmentTag:String?,positionTag: String) {
    requireFragmentTagNotNull(newfragmentTag)
    store.setFragmentCurAtPos(newfragmentTag!!,positionTag)
}
//--------------------
//some general utility extension methods

val Fragment.reduks: Reduks<out Any>? get() {
    val fsa=activity as? ReduksActivity<*>
    @Suppress("UNCHECKED_CAST")
    return (fsa?.reduks as? Reduks<out Any>)
}
/**
 * TO be called in Fragment OnViewCreated, for adding a reduks subscriber specific for this fragment
 * the fragment status is set to Active
 */
fun <S>Fragment.reduksSubscribe(storeSubscriberBuilder:StoreSubscriberBuilder<S>) {
    reduks?.setFragmentActiveStatus(tag, FragmentActiveStatus.fragmentStatusActive)
    @Suppress("UNCHECKED_CAST")
    val fsa=activity as? ReduksActivity<S>

    requireFragmentTagNotNull(tag)
    fsa?.reduks?.subscribe(tag!!,storeSubscriberBuilder)
}
/**
 * TO be called in Fragment OnViewCreated, for adding a reduks subscriber specific for this fragment
 * the fragment status is set to Active
 */
fun <S>Fragment.reduksSubscribe(storeSubscriber:StoreSubscriber<S>) {
    reduks?.setFragmentActiveStatus(tag, FragmentActiveStatus.fragmentStatusActive)
    @Suppress("UNCHECKED_CAST")
    val fsa=activity as? ReduksActivity<S>
    @Suppress("UNCHECKED_CAST")
    requireFragmentTagNotNull(tag)
    fsa?.reduks?.subscribe(tag!!,storeSubscriber)
}

/**
 * TO be called in Fragment OnDestroyView, for detaching the bus data handler/reduks subscriber associated to this fragment
 * the fragment status is set to Inactive
 */
fun Fragment.reduksUnsubscribe() {
    reduks?.setFragmentActiveStatus(tag, FragmentActiveStatus.fragmentStatusInactive)
    requireFragmentTagNotNull(tag)
    reduks?.apply {
       unsubscribe(tag!!)
       removeBusDataHandlers(tag!!)
    }
}

inline fun <reified BusDataType:Any> ReduksActivity<out Any>.reduksAddBusDataHandler(noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? =
        reduks.AddBusDataHandler(reduks.ctx.moduleId,true,null,fn)

/**
 * use fragment tag as subscription tag and clear busdata after handling
 * fragment store subscribers can be destroyed and then get notifications from old bus data so by default we
 * should clear bus data after handling it
 */
inline fun <reified BusDataType:Any> Fragment.reduksAddBusDataHandler(noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? {
    requireFragmentTagNotNull(tag)
    return reduks?.AddBusDataHandler(tag!!,true,null,fn)
}

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