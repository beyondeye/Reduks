package com.beyondeye.reduks

import com.beyondeye.reduks.modules.MultiStore
import com.beyondeye.reduks.modules.ReduksContext
import com.beyondeye.reduks.modules.subStore_


/**
 * see [Store.subStore]
 * note that undefined (null) [subctx] is no more allowed as argument
 */
inline fun<reified SB:Any> Reduks<*>.subStore(subctx: ReduksContext):Store<SB>?  =store.subStore(subctx)
/**
 * see [Store.subState]
 * note that undefined (null) [subctx] is no more allowed as argument
 */
inline fun<reified SB:Any> Reduks<*>.subState(subctx: ReduksContext):SB?  =store.subState(subctx)
/**
 * see [Store.subDispatcher]
 * if [isWeakRef] true then get a keep a weak reference of the dispatcher instead of a normal reference
 * note that undefined (null) [subctx] is no more allowed as argument
 */
inline fun<reified SB:Any> Reduks<*>.subDispatcher(subctx: ReduksContext,isWeakRef:Boolean=true):DispatcherFn?  =store.subDispatcher<SB>(subctx,isWeakRef)



/**
 * try to retrieve a substore data: if input param subctx is null then
 * then use as context the  default substore ReduksContext for the specified state type SB
 * see [MultiStore.subStore_]
 * Note that undefined (null) [subctx] is no more allowed as argument
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
inline fun<reified SB:Any> Store<*>.subStore(subctx: ReduksContext):Store<SB>?  =
    if(this is MultiStore)  subStore_<SB>(subctx) else null

/**
 * try to retrieve a substore state: if input param subctx is null then
 * then use as context the  default substore ReduksContext for the specified state type SB
 * see [MultiStore.subStore_]
 * Note that undefined (null) [subctx] is no more allowed as argument
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
inline fun<reified SB:Any> Store<*>.subState(subctx: ReduksContext):SB?  =
    if(this is MultiStore)  subStore_<SB>(subctx)?.state else null


/**
 * try to retrieve a substore dispatcher: if input param subctx is null then
 * then use as context the  default substore ReduksContext for the specified state type SB
 * see [MultiStore.subStore_]
 * if [isWeakRef] true then get a keep a weak reference of the dispatcher instead of a normal reference
 * Note that undefined (null) [subctx] is no more allowed as argument
 * @return null if either the a substore with the specified context was not found or it was found but it has a
 * different substate type than required
 */
inline fun<reified SB:Any> Store<*>.subDispatcher(subctx: ReduksContext,isWeakRef: Boolean):DispatcherFn?  =
    if(this is MultiStore)  subStore_<SB>(subctx)?.getDispatcherFn(isWeakRef) else null

