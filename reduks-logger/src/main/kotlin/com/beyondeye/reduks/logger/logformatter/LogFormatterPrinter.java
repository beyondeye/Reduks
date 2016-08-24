package com.beyondeye.reduks.logger.logformatter;

import com.beyondeye.reduks.logger.LogLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class LogFormatterPrinter {

    private static final String DEFAULT_TAG = "RDKS";


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
    private static final String DOUBLE_DIVIDER_CHAR = "═";
    private static final String SINGLE_DIVIDER_CHAR = "─";
    private String TOP_BORDER;
    private String BOTTOM_BORDER;
    private String MIDDLE_BORDER;

    /**
     * single level indent blanks
     */
    private static final String SINGLE_INDENT_BLANKS = "  ";

    /**
     * tag is used for the Log, the name is a little different
     * in order to differentiate the logs easily with the filter
     */
    private String tag;


    private int localMethodCount;
    private String indentBlanks = "";
    private int collapsed = 0;
    private int grouped = 0;
    private int groupHeaderLogLevel=-1;
    private String groupHeaderTagSuffix=null;
    private StringBuilder collapsedMsgBuffer =new StringBuilder();

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
        initDividerString();
    }

    private void initDividerString() {
        int dl=settings.borderDividerLength;
        StringBuilder sb=new StringBuilder();
        //TOP_BORDER
        sb.append(TOP_LEFT_CORNER);
        for(int i=0;i<dl;++i) sb.append(DOUBLE_DIVIDER_CHAR);
        TOP_BORDER=sb.toString();
        sb.setLength(0);

        //BOTTOM_BORDER
        sb.append(BOTTOM_LEFT_CORNER);
        for(int i=0;i<dl;++i) sb.append(DOUBLE_DIVIDER_CHAR);
        BOTTOM_BORDER=sb.toString();
        sb.setLength(0);

        //MIDDLE BORDER
        sb.append(MIDDLE_CORNER);
        for(int i=0;i<dl;++i) sb.append(SINGLE_DIVIDER_CHAR);
        MIDDLE_BORDER=sb.toString();
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
        ++grouped;
    }
    public void groupCollapsedStart() {
        groupStart();
        ++collapsed;
    }
    public void increaseIndent() {
        indentBlanks += SINGLE_INDENT_BLANKS;
    }
    private void decreaseIndent() {
        int newlength = indentBlanks.length() - SINGLE_INDENT_BLANKS.length();
        if (newlength >= 0) {
            indentBlanks = indentBlanks.substring(0, newlength);
        }
    }

    public void groupEnd() {
        //remove one level of collapse
        if (collapsed >0) { //closed a collapsed group
            if(--collapsed ==0) { //closed all collapse level: empty buffer
                flushCollapsedBuffer(); //CALL BEFORE flushGroup()!
            }
        }
        //remove one level of group
        if (grouped >0) { //closed a collapsed group
            if(--grouped ==0) { //closed all collapse level: empty buffer
                flushGroup();
            }
        }
        decreaseIndent();
    }



    public boolean isInGroup() {
        return grouped >0;
    }

    public boolean isCollapsed() {
        return collapsed >0;
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
        String formattedJson=getPrettyPrintedJson(objName,json);
        if(formattedJson==null) {
            e(jsonWithObjName(objName,"<Invalid Json>"));
        }
        else if (formattedJson.length()==0) {
            d(jsonWithObjName(objName,"<Empty json content>"));
        }
        else {
            log(formattedJson, logLevel, null);
        }
    }
    public String getPrettyPrintedJson(String objName,String json) {
        if (Helper.isEmpty(json)) {
            return "";
        }
        try {
            json = json.trim();
            if(isCollapsed()) { //no json pretty printing if collapsed
                return jsonWithObjName(objName,json);
            }
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                return jsonWithObjName(objName,jsonObject.toString(JSON_INDENT));
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                return jsonWithObjName(objName,jsonArray.toString(JSON_INDENT));
            }
        } catch (JSONException e) {
            return  null;
        }
        return null;
    }
    //TODO use stringbuilder here?
    String jsonWithObjName(String objName, String json) {
        if(objName!=null&&objName.length()>0)
            return objName+"="+json;
        else return json;
    }

    public synchronized void log(String message,int loglevel, String tagSuffix) {
        if (!settings.isLogEnabled) {
            return;
        }
        boolean isGroupHeader=isInGroup()&& groupHeaderLogLevel<0;
        if(isGroupHeader) {
            groupHeaderTagSuffix =tagSuffix;
            groupHeaderLogLevel=loglevel;
        }
        if(!isInGroup()||isGroupHeader) {
            logTopBorder(loglevel, tagSuffix);
            logHeaderContent(loglevel, tagSuffix, getMethodCount(), settings.isShowThreadInfo, settings.isShowCallStack);
        } else {
            if(!isCollapsed()) logDivider(loglevel,tagSuffix);

        }

        if(isCollapsed()) {
            collapsedMsgBuffer.append(message);
        } else {
            logMessageBodyChunked(loglevel, tagSuffix, message);
        }
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

    private void flushCollapsedBuffer() {
        logMessageBodyChunked(groupHeaderLogLevel, groupHeaderTagSuffix, collapsedMsgBuffer.toString());
        collapsedMsgBuffer.setLength(0); //empty buffer

    }
    private void flushGroup() {
        logBottomBorder(groupHeaderLogLevel, groupHeaderTagSuffix);
        groupHeaderLogLevel=-1;
        groupHeaderTagSuffix=null;
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
        if (!settings.isBorderEnabled) return;
        logChunk(logLevel, tagSuffix, TOP_BORDER);
    }
    private String HorizontalDoubleLine() {
        return settings.isBorderEnabled ? HORIZONTAL_DOUBLE_LINE_STR : "";
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

        int stackOffset = getStackOffset(trace) + settings.methodOffset;

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
        if (!settings.isBorderEnabled) return;
        logChunk(logLevel, tagSuffix, BOTTOM_BORDER);
    }

    private void logDivider(int logLevel, String tagSuffix) {
        if (!settings.isBorderEnabled) return;
        logChunk(logLevel, tagSuffix, MIDDLE_BORDER);
    }

    private void logContent(int logLevel, String tagSuffix, String chunk) {
//        String[] lines = chunk.split(LINE_SEPARATOR_CHAR);
        String[] lines = chunk.split("\n");
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
            return this.tag + "-" + suffixTag + indentBlanks;
        }
        return this.tag;
    }


    private int getMethodCount() {
        int count = localMethodCount;
        int result = settings.methodCount;
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
