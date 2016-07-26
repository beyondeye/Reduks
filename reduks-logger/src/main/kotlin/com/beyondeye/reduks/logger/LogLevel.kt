package com.beyondeye.reduks.logger

/**
 * standard android log levels translated to integer constants in order to pass log level as parameter
 * Created by daely on 7/25/2016.
 */
object LogLevel {
    @JvmField val v=0
    @JvmField val VERBOSE=v
    @JvmField val d=1
    @JvmField val DEBUG=d
    @JvmField val i=2
    @JvmField val INFO=i
    @JvmField val w=3
    @JvmField val WARN=w
    @JvmField val e=4
    @JvmField val ERROR=e
    @JvmField val wtf=5
    @JvmField val ASSERT=wtf
}