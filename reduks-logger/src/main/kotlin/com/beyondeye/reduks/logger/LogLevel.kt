package com.beyondeye.reduks.logger

/**
 * standard android log levels translated to integer constants in order to pass log level as parameter
 * Created by daely on 7/25/2016.
 */
object LogLevel {
    @JvmField val v=0
    @JvmField val VERBOSE=v
    @JvmField val i=1
    @JvmField val INFO=i
    @JvmField val d=2
    @JvmField val DEBUG=d
    @JvmField val w=3
    @JvmField val WARN=w
    @JvmField val wtf=5
    @JvmField val ASSERT=wtf
    @JvmField val e=6
    @JvmField val ERROR=e
}