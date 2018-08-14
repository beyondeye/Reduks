package com.beyondeye.reduks
/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store
 */
interface Store<S> {
    /**
     * the current state
     */
    val state: S
    /**
     * dispatch the action to the store and return it
     * An action can be of Any type
     */
    var dispatch: (action: Any) -> Any

    /**
     * return a subscription
     */
    fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription

    /**
     * replace current reducer with new one
     * note that MultiStore does not support this. Call replaceReducer on the component stores instead
     */
    fun replaceReducer(reducer: Reducer<S>)

}


/**
 * allow to use direct call on dispatcher function reference: dispatch(action) even if it is nullable
 * instead of having to write dispatch?.invoke(action)
 */
operator fun ((action:Any) -> Any)?.invoke(action:Any):Any? =
    if(this!=null) this.invoke(action) else null

/**
 * extension method for directly provide a lambda as argument for store subscribe
 */
fun <S> Store<S>.subscribe(lambda: () -> Unit) = this.subscribe(StoreSubscriberFn<S> { lambda() })

/**
 * extension method for directly subscribing using a store subscriber builder
 */
fun <S> Store<S>.subscribe(sb: StoreSubscriberBuilder<S>?) =if(sb!=null) this.subscribe(sb.build(this)) else null

/**
 * extension method for checking at compile time that we only dispatch objects derived from
 * base [Action] interface
 */
fun <S> Store<S>.dispatch_a(action: Action) = dispatch(action)

/**
 * extension method for checking at compile time that we only dispatch objects derived from
 * base [StandardAction] interface
 */
fun <S> Store<S>.dispatch_sa(action: StandardAction) = dispatch(action)


/**
 * extension method for obtained encapsulated [DispatcherFn] reference to the store [dispatch] method
 *  if [isWeakRef] true then get a [DispatcherFn_weakref] instance
 */
fun <S> Store<S>.getDispatcherFn(isWeakRef:Boolean=false)=DispatcherFn.instance(this.dispatch,isWeakRef)