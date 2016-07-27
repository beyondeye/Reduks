package com.beyondeye.reduks.logger.logformatter;

import com.beyondeye.reduks.logger.LogLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class LogFormatterPrinter {

    private static final String DEFAULT_TAG = "PRETTYLOGGER";


    /**
     * It is used for json pretty print
     */
    private static final int JSON_INDENT = 2;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 3;


    private static final String LINE_SEPARATOR_CHAR =System.getProperty("line.separator");
    /**
     * Drawing toolbox
     */
    private static final char TOP_LEFT_CORNER = '╔';
    private static final char BOTTOM_LEFT_CORNER = '╚';
    private static final char MIDDLE_CORNER = '╟';
    private static final char HORIZONTAL_DOUBLE_LINE = '║';
    private static final String HORIZONTAL_DOUBLE_LINE_STR = HORIZONTAL_DOUBLE_LINE+" ";
    private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
    private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;

    /**
     * single level group blanks
     */
    private static final String SINGLE_GROUP_BLANKS = "  ";

    /**
     * tag is used for the Log, the name is a little different
     * in order to differentiate the logs easily with the filter
     */
    private String tag;

    /**
     * Localize single tag and method count and groupBlanks for each thread
     */
//    private final ThreadLocal<String> localTag = new ThreadLocal<String>();
    private int localMethodCount;
    private String groupBlanks = "";
    private int  collapsedLevel = 0;
    private int groupedLevel = 0;

    /**
     * It is used to determine log settings such as method count, thread info visibility
     */
    private final LogFormatterSettings settings;

    public LogFormatterPrinter(LogFormatterSettings settings) {
        if(settings!=null)
            this.settings=settings;
        else
            this.settings=new LogFormatterSettings();
        setTag(DEFAULT_TAG);
    }

    /**
     * It is used to change the tag
     *
     * @param tag is the given string which will be used in LogFormatter
     */
    public void setTag(String tag) {
        if (tag == null) {
            throw new NullPointerException("tag may not be null");
        }
        if (tag.trim().length() == 0) {
            throw new IllegalStateException("tag may not be empty");
        }
        this.tag = tag;
    }

    public LogFormatterSettings getSettings() {
        return settings;
    }

    public void groupStart() {
        groupBlanks += SINGLE_GROUP_BLANKS;
        ++groupedLevel;
    }
    
    public void groupEnd() {
        int newlength = groupBlanks.length() - SINGLE_GROUP_BLANKS.length();
        if (newlength >= 0) {
            groupBlanks= groupBlanks.substring(0, newlength);
        }
        //remove one level of collapse
        if (collapsedLevel>0) { //closed a collapsed group
            if(--collapsedLevel==0) { //closed all collapse level: empty buffer
                flushCollapsedBuffer();
            }
        }
        //remove one level of group
        if (groupedLevel>0) { //closed a collapsed group
            if(--groupedLevel==0) { //closed all collapse level: empty buffer
                flushGroup();
            }
        }

    }


    public boolean isInGroup() {
        return groupedLevel>0;
    }

    public boolean isCollapsed() {
        return collapsedLevel>0;
    }
    public void groupCollapsedStart() {
        groupStart();
        ++collapsedLevel;
    }


    public void selLocalMethodCount(int methodCount) {
        localMethodCount=methodCount;
    }

//    @Override
//    public void d(Object object) {
//        String message;
//        if (object.getClass().isArray()) {
//            message = Arrays.deepToString((Object[]) object);
//        } else {
//            message = object.toString();
//        }
//        log(DEBUG, null, message);
//    }

    private void d(String message) {
        log(message,LogLevel.DEBUG, null);
    }

    private void e(String message) {
        log(message,LogLevel.ERROR, null);
    }

    /**
     * Formats the json content and print it
     *
     * @param json the json content
     */
    public void json(String objName,String json,int logLevel,String tagSuffix) {
        if (Helper.isEmpty(json)) {
            d("Empty/Null json content");
            return;
        }
        try {
            json = json.trim();
            if(isCollapsed()) { //no json pretty printing if collapsed
                log(jsonWithObjName(objName,json),logLevel,null);
                return;
            }
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                String formatted_json = jsonObject.toString(JSON_INDENT);
                log(jsonWithObjName(objName,formatted_json),logLevel,null);
                return;
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                String formatted_json = jsonArray.toString(JSON_INDENT);
                log(jsonWithObjName(objName,formatted_json),logLevel,null);
                return;
            }
            e("Invalid Json");
        } catch (JSONException e) {
            e("Invalid Json");
        }
    }
    //TODO use stringbuilder here?
    String jsonWithObjName(String objName, String json) {
        return objName+"="+json;
    }

    public synchronized void log(String message,int loglevel, String tagSuffix) {
        if (!settings.isLogEnabled()) {
            return;
        }
        if (isCollapsed()) log_collapsed(message,loglevel,tagSuffix);

        int methodCount = getMethodCount();
        logTopBorder(loglevel, tagSuffix);

        if(!isInGroup()) { //don't log header content if in group, log header only for group header, but log logDivider
            logHeaderContent(loglevel, tagSuffix, methodCount,  settings.isShowThreadInfo(),settings.isShowCallStack());
        } else {
            logDivider(loglevel,tagSuffix);
        }


        logMessageBodyChunked(loglevel, tagSuffix, message);
        if(!isInGroup()) {
            logBottomBorder(loglevel, tagSuffix);
        }
    }

    public String addFormattedThrowableToMessage(String message, Throwable throwable) {
        if (throwable != null && message != null) {
            message += " : " + Helper.getStackTraceString(throwable);
        }
        if (throwable != null && message == null) {
            message = Helper.getStackTraceString(throwable);
        }
        return message;
    }

    private String msgBufferTagSuffix ="";
    private int    msgBufferLogLevel=-1;
    private StringBuilder msgbuffer=new StringBuilder();
    //TODO remove synchronized from here and put on reduks_logger printBuffer
    private void log_collapsed(String message,int loglevel, String tagSuffix) {
        boolean isFirstLineInCollapsedGroup=msgbuffer.length()==0;
        int methodCount = getMethodCount();
        if(isFirstLineInCollapsedGroup) {
            msgBufferTagSuffix =tagSuffix;
            msgBufferLogLevel=loglevel;
            logTopBorder(loglevel, tagSuffix);
            logHeaderContent(loglevel, tagSuffix, methodCount, settings.isShowThreadInfo(), settings.isShowCallStack());
        }
        msgbuffer.append(message);
    }
    private void flushCollapsedBuffer() {
        logMessageBodyChunked(msgBufferLogLevel, msgBufferTagSuffix, msgbuffer.toString());
        logBottomBorder(msgBufferLogLevel, msgBufferTagSuffix);
        msgbuffer.setLength(0); //empty buffer
        msgBufferTagSuffix="";
        msgBufferLogLevel=-1;
    }
    private void flushGroup() {
        //TODO use loglevel and tagsuffix from group header
        int groupLogLevel=LogLevel.INFO;
        String groupTagSuffix=null;
        logBottomBorder(groupLogLevel, groupTagSuffix);
    }

    private void logMessageBodyChunked(int loglevel, String tagSuffix, String message) {
        //get bytes of message with system's default charset (which is UTF-8 for Android)
        byte[] bytes = message.getBytes();
        int length = bytes.length;
        int CHUNK_SIZE=settings.getLogAdapter().max_message_size();
        if (length <= CHUNK_SIZE) {
            logContent(loglevel, tagSuffix, message);
            return;
        }

        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int count = Math.min(length - i, CHUNK_SIZE);
            //create a new String with system's default charset (which is UTF-8 for Android)
            logContent(loglevel, tagSuffix, new String(bytes, i, count));
        }

    }

    public void resetSettings() {
        settings.reset();
    }


    private void logTopBorder(int logLevel, String tagSuffix) {
        if (!settings.isBorderEnabled()) return;
        logChunk(logLevel, tagSuffix, TOP_BORDER);
    }
    private String HorizontalDoubleLine() {
        return settings.isBorderEnabled() ? HORIZONTAL_DOUBLE_LINE_STR : "";
    }
    @SuppressWarnings("StringBufferReplaceableByString")
    private void logHeaderContent(int logLevel, String tagSuffix, int methodCount,boolean isShowThreadInfo,boolean isShowCallStack) {
        if(!(isShowCallStack||isShowThreadInfo)) return; //return early if nothing to show
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (isShowThreadInfo) {
            logChunk(logLevel, tagSuffix, HorizontalDoubleLine() + "Thread: " + Thread.currentThread().getName());
            logDivider(logLevel, tagSuffix);
        }
        if(!isShowCallStack) return;
        String level = "";

        int stackOffset = getStackOffset(trace) + settings.getMethodOffset();

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCount + stackOffset > trace.length) {
            methodCount = trace.length - stackOffset - 1;
        }

        for (int i = methodCount; i > 0; i--) {
            int stackIndex = i + stackOffset;
            if (stackIndex >= trace.length) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("║ ")
                    .append(level)
                    .append(getSimpleClassName(trace[stackIndex].getClassName()))
                    .append(".")
                    .append(trace[stackIndex].getMethodName())
                    .append(" ")
                    .append(" (")
                    .append(trace[stackIndex].getFileName())
                    .append(":")
                    .append(trace[stackIndex].getLineNumber())
                    .append(")");
            level += "   ";
            logChunk(logLevel, tagSuffix, builder.toString());
        }
        if (methodCount > 0) {
            logDivider(logLevel, tagSuffix);
        }
    }

    private void logBottomBorder(int logLevel, String tagSuffix) {
        if (!settings.isBorderEnabled()) return;
        logChunk(logLevel, tagSuffix, BOTTOM_BORDER);
    }

    private void logDivider(int logLevel, String tagSuffix) {
        if (!settings.isBorderEnabled()) return;
        logChunk(logLevel, tagSuffix, MIDDLE_BORDER);
    }

    private void logContent(int logLevel, String tagSuffix, String chunk) {
        String[] lines = chunk.split(LINE_SEPARATOR_CHAR);
        for (String line : lines) {
            logChunk(logLevel, tagSuffix, HorizontalDoubleLine() + line);
        }
    }


    private void logChunk(int logLevel, String tagSuffix, String chunk) {
        String finalTag = formatTag(tagSuffix);
        switch (logLevel) {
            case LogLevel.ERROR:
                settings.getLogAdapter().e(finalTag, chunk);
                break;
            case LogLevel.INFO:
                settings.getLogAdapter().i(finalTag, chunk);
                break;
            case LogLevel.VERBOSE:
                settings.getLogAdapter().v(finalTag, chunk);
                break;
            case LogLevel.WARN:
                settings.getLogAdapter().w(finalTag, chunk);
                break;
            case LogLevel.ASSERT:
                settings.getLogAdapter().wtf(finalTag, chunk);
                break;
            case LogLevel.DEBUG:
                // Fall through, log debug by default
            default:
                settings.getLogAdapter().d(finalTag, chunk);
                break;
        }
    }

    private String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    private String formatTag(String suffixTag) {
        if (!Helper.isEmpty(suffixTag) && !Helper.equals(this.tag, suffixTag)) {
            return this.tag + "-" + suffixTag + groupBlanks;
        }
        return this.tag;
    }


    private int getMethodCount() {
        int count = localMethodCount;
        int result = settings.getMethodCount();
        if (count >=0) {
            localMethodCount=-1;
            result = count;
        }
        if (result < 0) {
            throw new IllegalStateException("methodCount cannot be negative");
        }
        return result;
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(LogFormatterPrinter.class.getName()) && !name.equals(LogFormatter.class.getName())) {
                return --i;
            }
        }
        return -1;
    }

}
