package com.beyondeye.reduks

import com.beyondeye.reduks.modules.MultiStore
import com.beyondeye.reduks.modules.ReduksContext

/**
 * Redux module containing all Reduks components: store and its (main) subscriber
 * Created by daely on 6/8/2016.
 */
interface Reduks<State> {
    val ctx: ReduksContext
    val store: Store<State>
    /**
     * the main store subscriber or null if none is defined
     */
    val storeSubscriber: StoreSubscriber<State>?
    /**
     * the subscription for the main store subscriber or null if none is defined
     */
    val storeSubscription: StoreSubscription?
}


/**
 * a shortcut for getting the current state from the store object
 */
val <S>Reduks<S>.state:S get()=store.state

/**
 * a shortcurt for calling dispatch on the store object
 */
fun <S>Reduks<S>.dispatch(action:Any) = store.dispatch(action)


/**
 * try to retrieve a substore defined by its identifying context subctx and its type
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
fun<SB> Reduks<*>.subStore(subctx:ReduksContext):Store<SB>?  {
    val store_ =store //get a reference to the store (it can a be delegated property)
    @Suppress("UNCHECKED_CAST")
    if(store_ is MultiStore) return  store_.storeMap[subctx.toString()] as? Store<SB>
    return null
}

/**
 * try to retrieve a substore with default substore ReduksContext
 * WARNING: uses reflection
 */
inline fun<reified SB:Any> Reduks<*>.subStore():Store<SB>?  {
    val store_ =store //get a reference to the store (it can a be delegated property)
    @Suppress("UNCHECKED_CAST")
    if(store_ is MultiStore) return  store_.storeMap[ReduksContext.defaultModuleId<SB>()] as? Store<SB>
    return null
}

fun<SB> Reduks<*>.subState(subctx:ReduksContext):SB?  {
    val store_=store //get a reference to the store (it can a be delegated property)
    @Suppress("UNCHECKED_CAST")
    if(store_ is MultiStore) return  (store_.storeMap[subctx.toString()] as? Store<SB>)?.state
    return null
}

/**
 * try to retrieve a substore state with default substore ReduksContext
 * WARNING: uses reflection
 */
inline fun<reified SB:Any> Reduks<*>.subState():SB?  {
    val store_=store //get a reference to the store (it can a be delegated property)
    @Suppress("UNCHECKED_CAST")
    if(store_ is MultiStore) return  (store_.storeMap[ReduksContext.defaultModuleId<SB>()] as? Store<SB>)?.state
    return null
}
