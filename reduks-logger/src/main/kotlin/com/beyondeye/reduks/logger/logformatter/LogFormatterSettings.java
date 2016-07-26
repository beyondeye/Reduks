package com.beyondeye.reduks.logger.logformatter;

public final class LogFormatterSettings {

  private int methodCount = 2;
  private boolean showThreadInfo = false;
  private boolean showCallStack = false;
  private int methodOffset = 0;
  private LogAdapter logAdapter;

  private Boolean logEnabled = true;
  /**
   * if true, write all log message to a buffer, instead of using the android log
   */
  private Boolean logToString=false;
  private Boolean borderEnabled =true;

  public LogFormatterSettings showThreadInfo(Boolean showThreadInfo) {
    this.showThreadInfo = showThreadInfo;
    return this;
  }
  public LogFormatterSettings showCallStack(Boolean showCallStack) {
    this.showCallStack=showCallStack;
    return this;
  }

  public LogFormatterSettings methodCount(int methodCount) {
    if (methodCount < 0) {
      methodCount = 0;
    }
    this.methodCount = methodCount;
    return this;
  }

  public LogFormatterSettings borderEnabled(Boolean borderEnabled) {
      this.borderEnabled = borderEnabled;
    return this;
  }

  public LogFormatterSettings logEnabled(Boolean logEnabled) {
    this.logEnabled = logEnabled;
    return this;
  }
  public LogFormatterSettings logToString(Boolean logToString) {
    this.logToString = logToString;
    return this;
  }

  public LogFormatterSettings methodOffset(int offset) {
    this.methodOffset = offset;
    return this;
  }

  public LogFormatterSettings logAdapter(LogAdapter logAdapter) {
    this.logAdapter = logAdapter;
    return this;
  }

  public int getMethodCount() {
    return methodCount;
  }

  public boolean isShowThreadInfo() {
    return showThreadInfo;
  }
  public boolean isShowCallStack() {
    return showCallStack;
  }

  public Boolean isLogEnabled() {
    return logEnabled;
  }
  public Boolean isLogToString() {
    return logToString;
  }

  public int getMethodOffset() {
    return methodOffset;
  }
  public boolean isBorderEnabled() { return borderEnabled; }

  public LogAdapter getLogAdapter() {
    if (logAdapter == null) {
      logAdapter = logToString ? new StringBufferLogAdapter() : new AndroidLogAdapter();
    }
    return logAdapter;
  }

  public void reset() {
    methodCount = 2;
    methodOffset = 0;
    showThreadInfo = false;
    showCallStack = false;
    logEnabled = true;
  }
}
