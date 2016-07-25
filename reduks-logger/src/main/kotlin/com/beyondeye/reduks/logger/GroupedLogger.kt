package com.beyondeye.reduks.logger

//'console' API
internal interface GroupedLogger {
    /**
     * start a new group, increasing indent, until next call to [groupEnd]
     * and call [log] with the specified argument
     */
    fun group(s:String,logLevel:Int=LogLevel.INFO)
    /**
     * start a new collapsed group: all log lines are collapsed to a single line,, until next call to [groupEnd]
     * and call [log] with the specified argument
     */
    fun groupCollapsed(s:String,logLevel:Int=LogLevel.INFO)
    fun groupEnd()
    fun  log(s: String,logLevel:Int=LogLevel.INFO)
    fun  json(objName:String,s: String,logLevel:Int=LogLevel.INFO)
}