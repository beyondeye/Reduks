package com.beyondeye.reduksAndroid.activity

import android.util.Log
import com.beyondeye.reduks.Reduks
import com.beyondeye.reduks.StoreCreator

/**
 * base interface for KovenantReduksActivity and RxReduksActivity
 * Created by daely on 10/5/2016.
 */
interface  ReduksActivity<S> {
    val reduks: Reduks<S>
    /**
     * get the base store creator to use (even for a different state type argument)
     */
    fun <T> storeCreator():StoreCreator<T>

    /**
     * function that create the reduks module that should control this activity
     * If your activity also inherit from SingleModuleReduksActivity, then you can simply
     * define this function as
     * override fun initReduks(storeCreator:StoreCreator<S>) = initReduksSingleModule(storeCreator)
     */
    fun initReduks(): Reduks<S>
    companion object {
        const val reduksTag="*REDUKS*"
        val defaultReduksInternalLogger:(String)->Unit = {msg -> Log.e(reduksTag,msg)}
    }
}