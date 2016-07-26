package com.beyondeye.reduks.logger.logformatter

import com.beyondeye.reduks.logger.LogLevel

/**
 * LogFormatter is a wrapper of android.util.Log
 * But more pretty, simple and powerful
 * adapted from https://github.com/orhanobut/logger
 * It also support log entry grouping i.e. common indenting of multiple subsequent lines in a similar way to javascript console:
 * see https://developer.mozilla.org/en/docs/Web/API/console (Using groups in console)
 */
class LogFormatter(tag: String, settings: LogFormatterSettings) {

    private val printer: LogFormatterPrinter

    init {
        printer = LogFormatterPrinter(settings)
        printer.setTag(tag)
    }

    fun resetSettings() {
        printer.resetSettings()
    }

    val settings: LogFormatterSettings
        get() = printer.settings


    fun setLocalMethodCount(methodCount: Int) {
         printer.selLocalMethodCount(methodCount)
    }

    /**
     * start a new group, increasing indent, until next call to [groupEnd]
     * and call [log] with the specified argument
     */
    fun group(message: String, logLevel: Int= LogLevel.INFO,tagSuffix: String?=null) {
        printer.groupStart()
        printer.log(message,logLevel, tagSuffix, null)
    }

    /**
     * start a new collapsed group: all log lines are collapsed to a single line,, until next call to [groupEnd]
     * and call [log] with the specified argument
     */
    fun groupCollapsed(message: String, logLevel: Int=LogLevel.INFO,tagSuffix: String?=null) {
        printer.groupCollapsedStart()
        printer.log(message,logLevel, tagSuffix, null)
    }

    fun groupEnd() {
        printer.groupEnd()
    }
    fun log(message: String?, logLevel: Int=LogLevel.INFO,tagSuffix: String?=null,throwable: Throwable?=null) {
        printer.log(message,logLevel, tagSuffix, throwable)
    }

    /**
     * Formats the json content and print it

     * @param jsonMessage the json content
     */
    fun json(objName: String, jsonMessage: String, logLevel: Int=LogLevel.INFO,tagSuffix: String?=null) {
        printer.json(objName, jsonMessage,logLevel, tagSuffix)
    }


    fun getLogAsString(): String  {
        val logAdapter = printer.settings.logAdapter
        if (logAdapter !is StringBufferLogAdapter) return ""
        return logAdapter.buffer
    }

}
