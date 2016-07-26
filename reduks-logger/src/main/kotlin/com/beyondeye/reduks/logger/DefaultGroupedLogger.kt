package com.beyondeye.reduks.logger

import com.beyondeye.reduks.logger.logformatter.LogFormatter


/**
 * logger that support grouping, i.e. common indenting of multiple subsequent lines in a similar way to javascript console:
 * see https://developer.mozilla.org/en/docs/Web/API/console (Using groups in console)
 * Created by daely on 7/25/2016.
 */
internal class DefaultGroupedLogger(tag:String): GroupedLogger {
    val logFormatter=LogFormatter(tag)
    override fun group(s: String,logLevel:Int) {
        logFormatter.groupStart()
        log(s,logLevel)
    }

    override fun groupCollapsed(s: String,logLevel:Int) {
        logFormatter.groupCollapsedStart()
        log(s,logLevel)
    }

    override fun groupEnd() {
        logFormatter.groupEnd()
    }

    override fun log(s: String, logLevel: Int) {
        logFormatter.log(logLevel,null,s,null)
    }

    override fun json(objName:String,s: String, logLevel: Int) {
        logFormatter.json(logLevel,objName,s)
    }
}