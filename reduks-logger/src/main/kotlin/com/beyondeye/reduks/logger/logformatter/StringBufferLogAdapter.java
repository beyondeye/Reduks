package com.beyondeye.reduks.logger.logformatter;

/**
 * LogAdapter implementation that write log message to a string buffer
 * Created by daely on 7/26/2016.
 */
public class StringBufferLogAdapter  implements LogAdapter {
    private static final String LINE_SEPARATOR_CHAR =System.getProperty("line.separator");

    StringBuilder sb=new StringBuilder();
    public void reset() {
        sb.setLength(0);
    }
    public String getBuffer() {
        return sb.toString();
    }
    private void log( char prefixchar,String tag,String message) {
        sb.append(prefixchar);
        sb.append("/");
        sb.append(tag);
        sb.append(':');
        sb.append(message);
        sb.append(LINE_SEPARATOR_CHAR);
    }
    @Override
    public void d(String tag, String message) {
        log('D',tag,message);
    }


    @Override
    public void e(String tag, String message) {
        log('E',tag,message);
    }

    @Override
    public void w(String tag, String message) {
        log('W',tag,message);
    }

    @Override
    public void i(String tag, String message) {
        log('I',tag,message);
    }

    @Override
    public void v(String tag, String message) {
        log('V',tag,message);
    }

    @Override
    public void wtf(String tag, String message) {
        log('A',tag,message);
    }
}
