package com.beyondeye.reduks

/**
 * get a store creator and return a new enhanced one
 * see https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store-enhancer

 * single method interface, mainly used because kotlin does not support yet type alias for function types
 * Created by daely on 8/23/2016.
 */
interface IStoreEnhancer<S> {
    fun enhance(next: StoreCreator<S>): StoreCreator<S>
}
