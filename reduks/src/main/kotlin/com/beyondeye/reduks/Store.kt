package com.beyondeye.reduks

/**
 * see also https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store
 */
interface Store<S> {
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
 * extension method for directly provide a lambda as argument for store subscribe
 */
fun <S>Store<S>.subscribe(lambda:(S)->Unit) {
    this.subscribe(StoreSubscriber{newState-> lambda(newState)})
}
/**
 * extension method for checking at compile time that we only dispatch objects derived from
 * base [Action] interface
 */
fun <S>Store<S>.dispatch_a(action:Action)=dispatch(action)

/**
 * extension method for checking at compile time that we only dispatch objects derived from
 * base [StandardAction] interface
 */
fun <S>Store<S>.dispatch_sa(action:StandardAction)=dispatch(action)
