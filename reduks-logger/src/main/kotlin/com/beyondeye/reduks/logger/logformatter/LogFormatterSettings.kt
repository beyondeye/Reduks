package com.beyondeye.reduks.logger.logformatter

data class LogFormatterSettings(
        var methodCount:Int = 2,
        var isShowThreadInfo:Boolean = false,
        var isShowCallStack:Boolean = false,
        var methodOffset:Int = 0,
        var isLogEnabled: Boolean = true,
        /**
         * if true, write all log message to a buffer, instead of using the android log
         */
        var isLogToString: Boolean = false,
        var isBorderEnabled: Boolean = true
        ) {

    internal var logAdapter: LogAdapter? = null

    fun showThreadInfo(showThreadInfo: Boolean?): LogFormatterSettings {
        this.isShowThreadInfo = showThreadInfo!!
        return this
    }

    fun showCallStack(showCallStack: Boolean?): LogFormatterSettings {
        this.isShowCallStack = showCallStack!!
        return this
    }

    fun methodCount(methodCount: Int): LogFormatterSettings {
        this.methodCount = if(methodCount>=0) methodCount else 0
        return this
    }

    fun borderEnabled(borderEnabled: Boolean): LogFormatterSettings {
        this.isBorderEnabled = borderEnabled
        return this
    }

    fun logEnabled(logEnabled: Boolean): LogFormatterSettings {
        this.isLogEnabled = logEnabled
        return this
    }

    fun logToString(logToString: Boolean): LogFormatterSettings {
        this.isLogToString = logToString
        return this
    }

    fun methodOffset(offset: Int): LogFormatterSettings {
        this.methodOffset = offset
        return this
    }

    fun logAdapter(logAdapter: LogAdapter): LogFormatterSettings {
        this.logAdapter = logAdapter
        return this
    }

    fun getLogAdapter(): LogAdapter {
        if (logAdapter == null) {
            logAdapter = if (isLogToString) StringBufferLogAdapter() else AndroidLogAdapter()
        }
        return logAdapter!!
    }

    fun reset() {
        methodCount = 2
        methodOffset = 0
        isShowThreadInfo = false
        isShowCallStack = false
        isLogEnabled = true
        isLogToString = false
        isBorderEnabled = true
    }
}
