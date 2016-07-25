package com.beyondeye.reduks.logger

//'console' API
internal interface GroupedLogger {
    fun group(s:String)
    fun groupCollapsed(s:String)
    fun groupEnd()
    fun  log(s: String,logLevel:Int=LogLevel.INFO)
    fun  json(s: String,logLevel:Int=LogLevel.INFO)
}