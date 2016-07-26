package com.beyondeye.reduks.logger.logformatter;

/**
 * LogFormatter is a wrapper of {@link android.util.Log}
 * But more pretty, simple and powerful
 * adapted from https://github.com/orhanobut/logger
 * It also support log entry grouping i.e. common indenting of multiple subsequent lines in a similar way to javascript console:
 * see https://developer.mozilla.org/en/docs/Web/API/console (Using groups in console)
 */
public class LogFormatter {

  private final LogFormatterPrinter printer;

  public LogFormatter(String tag,LogFormatterSettings settings) {
    printer = new LogFormatterPrinter(settings);
    printer.setTag(tag);
  }
  public void resetSettings() {
    printer.resetSettings();
  }
  public LogFormatterSettings getSettings() { return printer.getSettings(); }


  public LogFormatterPrinter t(int methodCount) {
    return printer.t(methodCount);
  }

  public void groupStart() {
    printer.groupStart();
  }
  public void groupCollapsedStart() {
    printer.groupCollapsedStart();
  }
  public void groupEnd() {
    printer.groupEnd();
  }

  public  void log(int loglevel, String tagSuffix, String message, Throwable throwable) {
    printer.log(loglevel, tagSuffix, message, throwable);
  }

  /**
   * Formats the json content and print it
   *
   * @param json the json content
   */
  public  void json(int logLevel,String objName,String json) {
    printer.json(logLevel,objName,json);
  }


    public String getStringBuffer() {
        LogAdapter logAdapter= printer.getSettings().getLogAdapter();
        if(!(logAdapter instanceof StringBufferLogAdapter)) return "";
        return ((StringBufferLogAdapter)logAdapter).getBuffer();
    }
}
