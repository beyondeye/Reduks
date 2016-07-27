package com.beyondeye.reduks.logger.logformatter

class LogFormatterSettings {

    var methodCount = 2
        private set
    var isShowThreadInfo = false
        private set
    var isShowCallStack = false
        private set
    var methodOffset = 0
        private set
    internal var logAdapter: LogAdapter? = null

    var isLogEnabled: Boolean = true
        private set
    /**
     * if true, write all log message to a buffer, instead of using the android log
     */
    var isLogToString: Boolean = false
        private set
    private var borderEnabled: Boolean = true

    fun showThreadInfo(showThreadInfo: Boolean?): LogFormatterSettings {
        this.isShowThreadInfo = showThreadInfo!!
        return this
    }

    fun showCallStack(showCallStack: Boolean?): LogFormatterSettings {
        this.isShowCallStack = showCallStack!!
        return this
    }

    fun methodCount(methodCount: Int): LogFormatterSettings {
        var methodCount = methodCount
        if (methodCount < 0) {
            methodCount = 0
        }
        this.methodCount = methodCount
        return this
    }

    fun borderEnabled(borderEnabled: Boolean): LogFormatterSettings {
        this.borderEnabled = borderEnabled
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

    val isBorderEnabled: Boolean
        get() = borderEnabled

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
        borderEnabled = true
    }
}
