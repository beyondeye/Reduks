package com.beyondeye.reduks.logger.logformatter

class LogFormatterSettings(
        @JvmField  var methodCount:Int = 2,
        @JvmField var borderDividerLength:Int=90,
        @JvmField var isShowThreadInfo:Boolean = false,
        @JvmField var isShowCallStack:Boolean = false,
        @JvmField var methodOffset:Int = 0,
        @JvmField var isLogEnabled: Boolean = true,
        /**
         * if true, write all log message to a buffer, instead of using the android log
         */
        @JvmField var isLogToString: Boolean = false,
        @JvmField var isBorderEnabled: Boolean = true
        ) {

    internal var logAdapter: LogAdapter? = null

    fun getLogAdapter(): LogAdapter {
        if (logAdapter == null) {
            logAdapter = if (isLogToString) StringBufferLogAdapter() else AndroidLogAdapter()
        }
        return logAdapter!!
    }

    fun reset() {
        methodCount = 2
        borderDividerLength =90
        methodOffset = 0
        isShowThreadInfo = false
        isShowCallStack = false
        isLogEnabled = true
        isLogToString = false
        isBorderEnabled = true
    }
}
