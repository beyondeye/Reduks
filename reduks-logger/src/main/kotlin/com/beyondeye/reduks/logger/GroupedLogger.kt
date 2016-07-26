package com.beyondeye.reduks.logger

import com.beyondeye.reduks.logger.logformatter.LogFormatter


/**
 * logger that support grouping, i.e. common indenting of multiple subsequent lines in a similar way to javascript console:
 * see https://developer.mozilla.org/en/docs/Web/API/console (Using groups in console)
 * Created by daely on 7/25/2016.
 */
internal class GroupedLogger(tag: String, logToStringBuffer: Boolean) {
    val logFormatter = LogFormatter(tag,logToStringBuffer)
    /**
     * start a new group, increasing indent, until next call to [groupEnd]
     * and call [log] with the specified argument
     */
    fun group(s: String, logLevel: Int=LogLevel.INFO) {
        logFormatter.groupStart()
        log(s, logLevel)
    }

    /**
     * start a new collapsed group: all log lines are collapsed to a single line,, until next call to [groupEnd]
     * and call [log] with the specified argument
     */
    fun groupCollapsed(s: String, logLevel: Int=LogLevel.INFO) {
        logFormatter.groupCollapsedStart()
        log(s, logLevel)
    }

    fun groupEnd() {
        logFormatter.groupEnd()
    }

    fun log(s: String, logLevel: Int=LogLevel.INFO) {
        logFormatter.log(logLevel, null, s, null)
    }

    fun json(objName: String, s: String, logLevel: Int=LogLevel.INFO) {
        logFormatter.json(logLevel, objName, s)
    }

    fun getLogAsString(): String =logFormatter.getStringBuffer()
}