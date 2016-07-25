package com.beyondeye.reduks.logger.logformatter;

public interface Printer {

  Printer t(String tag, int methodCount);

  Settings init(String tag);

  Settings getSettings();

  void groupStart();
  void groupEnd();

  void d(String message, Object... args);

  void d(Object object);

  void e(String message, Object... args);

  void e(Throwable throwable, String message, Object... args);

  void w(String message, Object... args);

  void i(String message, Object... args);

  void v(String message, Object... args);

  void wtf(String message, Object... args);

  void json(String json);

  void log(int priority, String tag, String message, Throwable throwable);
  //void log(int priority, Throwable throwable, String msg, Object... args);

  void resetSettings();

}
