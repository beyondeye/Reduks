package com.beyondeye.reduksAndroid.activity

import com.beyondeye.reduks.Reduks

/**
 * base interface for KovenantReduksActivity and RxReduksActivity
 * Created by daely on 10/5/2016.
 */
interface  ReduksActivity<S> {
    val reduks: Reduks<S>
}