package com.beyondeye.reduks

import com.beyondeye.reduks.modules.MultiStore
import com.beyondeye.reduks.modules.ReduksContext
import com.beyondeye.reduks.modules.subStore_


/**
 * see [Store.subStore]
 */
inline fun<reified SB:Any> Reduks<*>.subStore(subctx: ReduksContext?=null):Store<SB>?  =store.subStore(subctx)
/**
 * see [Store.subState]
 */
inline fun<reified SB:Any> Reduks<*>.subState(subctx: ReduksContext?=null):SB?  =store.subState(subctx)
/**
 * see [Store.subDispatcher]
 */
inline fun<reified SB:Any> Reduks<*>.subDispatcher(subctx: ReduksContext?=null):((Any)->Any)?  =store.subDispatcher<SB>(subctx)



/**
 * try to retrieve a substore data: if input param subctx is null then
 * then use as context the  default substore ReduksContext for the specified state type SB
 * see [MultiStore.subStore_]
 * WARNING: when using the default context these methods use reflection
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
inline fun<reified SB:Any> Store<*>.subStore(subctx: ReduksContext?=null):Store<SB>?  =
    if(this is MultiStore)  subStore_<SB>(subctx) else null

/**
 * try to retrieve a substore state: if input param subctx is null then
 * then use as context the  default substore ReduksContext for the specified state type SB
 * see [MultiStore.subStore_]
 * WARNING: when using the default context these methods use reflection
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
inline fun<reified SB:Any> Store<*>.subState(subctx: ReduksContext?=null):SB?  =
    if(this is MultiStore)  subStore_<SB>(subctx)?.state else null


/**
 * try to retrieve a substore dispatcher: if input param subctx is null then
 * then use as context the  default substore ReduksContext for the specified state type SB
 * see [MultiStore.subStore_]
 * WARNING: when using the default context these methods use reflection
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
inline fun<reified SB:Any> Store<*>.subDispatcher(subctx: ReduksContext?=null):((Any)->Any)?  =
    if(this is MultiStore)  subStore_<SB>(subctx)?.dispatch else null

