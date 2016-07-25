package com.beyondeye.reduks.logger.logformatter;

public final class Settings {

  private int methodCount = 2;
  private boolean showThreadInfo = true;
  private int methodOffset = 0;
  private LogAdapter logAdapter;

  /**
   * Determines to how logs will be printed
   */
  private Boolean logEnabled = true;

  public Settings hideThreadInfo() {
    showThreadInfo = false;
    return this;
  }

  public Settings methodCount(int methodCount) {
    if (methodCount < 0) {
      methodCount = 0;
    }
    this.methodCount = methodCount;
    return this;
  }

  public Settings logEnabled(Boolean logEnabled) {
    this.logEnabled = logEnabled;
    return this;
  }

  public Settings methodOffset(int offset) {
    this.methodOffset = offset;
    return this;
  }

  public Settings logAdapter(LogAdapter logAdapter) {
    this.logAdapter = logAdapter;
    return this;
  }

  public int getMethodCount() {
    return methodCount;
  }

  public boolean isShowThreadInfo() {
    return showThreadInfo;
  }

  public Boolean getLogEnabled() {
    return logEnabled;
  }

  public int getMethodOffset() {
    return methodOffset;
  }

  public LogAdapter getLogAdapter() {
    if (logAdapter == null) {
      logAdapter = new AndroidLogAdapter();
    }
    return logAdapter;
  }

  public void reset() {
    methodCount = 2;
    methodOffset = 0;
    showThreadInfo = true;
    logEnabled = true;
  }
}
