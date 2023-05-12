package com.beyondeye.reduks.logger

import com.beyondeye.kjsonpatch.JsonDiff
import com.beyondeye.reduks.*
import com.beyondeye.reduks.logger.logformatter.LogFormatter
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlin.math.roundToInt

/**
 * Reduks Logger middleware
 * Created by daely on 7/21/2016.
 */
class ReduksLogger<S>(val config: ReduksLoggerConfig<S> = ReduksLoggerConfig()) : Middleware<S> {
    private val jsonDiffer = JsonDiff
    /**
     * gson instance used to serialize reduks State and Actions
     */
    var gsonInstance = GsonBuilder().serializeNulls().disableHtmlEscaping().serializeSpecialFloatingPointValues().create()
    private val stateType = StateType<S>()
    private val logger = LogFormatter(config.reduksLoggerTag,config.formatterSettings)
    private val logBuffer: MutableList<LogEntry<S>> = mutableListOf() //we need a logBuffer because of possible unhandled exceptions before we print the logEntry
    fun getLogAsString():String = logger.getLogAsString()
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any {
        // Exit early if predicate function returns 'false'
        val prevState = store.state
        if (!config.filter(prevState, action)) return nextDispatcher(action)
        val started = System.nanoTime()
        val logEntry = LogEntry(started, config.stateTransformer(prevState), action)
        logBuffer.add(logEntry)

        var returnedValue: Any? = null
        if (config.logErrors) {
            try {
                returnedValue = nextDispatcher(action)
            } catch (e: Exception) {
                logEntry.error = config.errorTransformer(e)
            }
        } else {
            returnedValue = nextDispatcher(action)
        }
        logEntry.took = nano2ms(logEntry.started,System.nanoTime())
        logEntry.nextState = config.stateTransformer(store.state)
        //check if diff is activated
        logEntry.diffActivated = if (config.logStateDiff && config.logStateDiffFilter != null) config.logStateDiffFilter.invoke(logEntry.nextState!!, action) else config.logStateDiff
        printBuffer(logBuffer)
        logBuffer.clear()

        if (logEntry.error != null) throw logEntry.error!!
        return returnedValue ?:object :Action{} //no action to return, return empty action
    }
    private fun printBuffer(buffer: List<LogEntry<S>>) {
        buffer.forEachIndexed { entryIdx, curEntry ->
            var took = curEntry.took
            var nextState = curEntry.nextState
            val nextEntry: LogEntry<S>? = if (entryIdx < buffer.size-1) buffer[entryIdx + 1] else null
            if (nextEntry != null) { //handle the case where we had an exception and buffer length>1
                nextState = nextEntry.prevState
                took = nano2ms(curEntry.started,nextEntry.started)
            }

            //message
            val formattedAction = config.actionTransformer(curEntry.action)
            val isCollapsed = config.collapsed(nextState, curEntry.action)
            val tookstr = took.toString().padStart(7) //took.toFixed(2)
            val durationstr = if (config.logActionDuration) "(in $tookstr ms)" else ""
            val actiontypestr = config.actionTypeExtractor(formattedAction)
            val title = "action: $actiontypestr $durationstr = "

            // Render
            try {
                if (isCollapsed) {
                    logger.groupCollapsed(title)
                } else {
                    logger.group(title)
                }
            } catch (e: Exception) {
                logger.log(title)
            }

            val actionLevel = config.actionLevel(LogElement.ACTION, formattedAction, curEntry.prevState, nextState, curEntry.error) ///use reduced info?: action
            if (actionLevel != null) {
                logger.json("",actionToJson(formattedAction), actionLevel)
            }

            val prevStateLevel = config.prevStateLevel(LogElement.PREVSTATE, formattedAction, curEntry.prevState, nextState, curEntry.error) //use reduced info?: action and prevState
            val prevStateJson=stateToJson(curEntry.prevState)
            if (prevStateLevel != null) {
                logger.json("prev state", prevStateJson, prevStateLevel)
            }

            val errorLevel = config.errorLevel(LogElement.ERROR, formattedAction, curEntry.prevState, nextState, curEntry.error) //use reduced info?: action, error, prevState
            if (curEntry.error != null && errorLevel != null) {
                logger.log("error" , errorLevel,null, curEntry.error)
            }

            val nextStateLevel = config.nextStateLevel(LogElement.NEXTSTATE, formattedAction, curEntry.prevState, nextState, curEntry.error) //use reduced info?: action nextState
            val nextStateJson:String=stateToJson(nextState)
            if (nextStateLevel != null) {
                logger.json("next state", nextStateJson, nextStateLevel)
            }

            val diffStateLevel=LogLevel.INFO
            if (config.logStateDiff) {
                diffLogger(prevStateJson, nextStateJson, isCollapsed,diffStateLevel)
            }

            try {
                logger.groupEnd()
            } catch (e: Exception) {
                logger.log("—— log end ——")
            }

        }
    }

    private fun  actionToJson(a: Any): String {
        return gsonInstance.toJson(a)
    }

    private fun stateToJson(s: S?):String {
        if(s==null) return ""
        return gsonInstance.toJson(s, stateType.type)
    }

    private fun diffLogger(prevStateJsonStr: String, nextStateJsonStr: String, collapsed: Boolean, diffStateLogLevel: Int) {
        val prevStateJson=JsonParser.parseString(prevStateJsonStr)
        val nextStateJson=JsonParser.parseString(nextStateJsonStr)
        val stateDiff_ = jsonDiffer.asJson(prevStateJson, nextStateJson)
        val stateDiff: JsonElement =
                if (stateDiff_.size() == 1) //single element diff: print it, not the array
                {
                    stateDiff_.get(0)
                } else {
                    stateDiff_
                }
        if (collapsed) {
            logger.groupCollapsed("state diff$stateDiff", diffStateLogLevel)
        } else {
            val stateDiffPretty=logger.getPrettyPrintedJson("state diff",stateDiff.toString())
            logger.group(stateDiffPretty, diffStateLogLevel)
        }

//        if (stateDiff.size()>2) {
//            logger.json("",stateDiff.toString(),diffStateLogLevel)
//        } else {
//            logger.log("—— no diff ——", diffStateLogLevel)
//        }

        logger.groupEnd()
    }
    companion object {
        //TODO move to Helper class
        fun nano2ms(start:Long,end:Long):Double = ((end - start) / 10000.0).roundToInt() / 100.0 //in ms rounded to max two decimals
    }
}