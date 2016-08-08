package com.beyondeye.reduks.logger

internal  class LogEntry<S>(
        val started: Long,
        val prevState: S,
        val action: Any,
        var error: Throwable? = null, //error, nextState and took need to be var in case we don't log errors and unhandled exceptions before we can update it
        var nextState: S? = null,
        var took: Double = 0.0,
        var diffActivated:Boolean=false)