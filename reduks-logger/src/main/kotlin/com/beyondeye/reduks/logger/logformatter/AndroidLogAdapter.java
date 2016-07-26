package com.beyondeye.reduks.logger.logformatter;

import android.util.Log;

class AndroidLogAdapter implements LogAdapter {
  @Override public void d(String tag, String message) {
    Log.d(tag, message);
  }

  @Override public void e(String tag, String message) {
    Log.e(tag, message);
  }

  @Override public void w(String tag, String message) {
    Log.w(tag, message);
  }

  @Override public void i(String tag, String message) {
    Log.i(tag, message);
  }

  @Override public void v(String tag, String message) {
    Log.v(tag, message);
  }

  @Override public void wtf(String tag, String message) {
    Log.wtf(tag, message);
  }

  /**
   * Android's max limit for a log entry is ~4076 bytes,
   * so 4000 bytes is used as chunk size since default charset
   * is UTF-8
   */
  @Override
  public int max_message_size() {
    return 4000;
  }
}
