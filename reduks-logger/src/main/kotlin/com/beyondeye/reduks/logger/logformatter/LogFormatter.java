package com.beyondeye.reduks.logger.logformatter;

/**
 * LogFormatter is a wrapper of {@link android.util.Log}
 * But more pretty, simple and powerful
 * adapted from https://github.com/orhanobut/logger
 */
public class LogFormatter {
//  public static final int DEBUG = 3;
//  public static final int ERROR = 6;
//  public static final int ASSERT = 7;
//  public static final int INFO = 4;
//  public static final int VERBOSE = 2;
//  public static final int WARN = 5;

  private LoggerPrinter printer = new LoggerPrinter();

  public LogFormatter(String tag) {
    printer = new LoggerPrinter();
    printer.init(tag);
  }
  public void resetSettings() {
    printer.resetSettings();
  }
  public LogFormatterSettings getSettings() { return printer.getSettings(); }

  public  LoggerPrinter t(String tag) {
    return printer.t(tag, printer.getSettings().getMethodCount());
  }

  public  LoggerPrinter t(int methodCount) {
    return printer.t(null, methodCount);
  }

  public  LoggerPrinter t(String tag, int methodCount) {
    return printer.t(tag, methodCount);
  }

  public  void log(int priority, String tag, String message, Throwable throwable) {
    printer.log(priority, tag, message, throwable);
  }

//  public static void d(String message, Object... args) {
//    printer.d(message, args);
//  }

//  public static void d(Object object) {
//    printer.d(object);
//  }

//  public static void e(String message, Object... args) {
//    printer.e(null, message, args);
//  }

//  public static void e(Throwable throwable, String message, Object... args) {
//    printer.e(throwable, message, args);
//  }

//  public static void i(String message, Object... args) {
//    printer.i(message, args);
//  }
//
//  public static void v(String message, Object... args) {
//    printer.v(message, args);
//  }
//
//  public static void w(String message, Object... args) {
//    printer.w(message, args);
//  }
//
//  public static void wtf(String message, Object... args) {
//    printer.wtf(message, args);
//  }

  /**
   * Formats the json content and print it
   *
   * @param json the json content
   */
  public  void json(String json) {
    printer.json(json);
  }



}
