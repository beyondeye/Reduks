package com.beyondeye.reduks.logger

import com.beyondeye.reduks.Middleware
import com.beyondeye.reduks.NextDispatcher
import com.beyondeye.reduks.StateType
import com.beyondeye.reduks.Store
import com.beyondeye.zjsonpatch.JsonDiff
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

/**
 * Reduks Logger middleware
 * Created by daely on 7/21/2016.
 */
class ReduksLogger<S>(val config: ReduksLoggerConfig<S> = ReduksLoggerConfig()) : Middleware<S> {
    private val jsonDiffer = JsonDiff
    private val jsonParser = JsonParser()
    /**
     * gson instance used to serialize reduks State and Actions
     */
    var gsonInstance = GsonBuilder().serializeNulls().disableHtmlEscaping().serializeSpecialFloatingPointValues().create()
    private val stateType = StateType<S>()
    private val logger = GroupedLogger(config.reduksLoggerTag,config.logToStringBuffer)
    private val logBuffer: MutableList<LogEntry<S>> = mutableListOf() //we need a logBuffer because of possible unhandled exceptions before we print the logEntry
    override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any): Any? {
        // Exit early if predicate function returns 'false'
        val prevState = store.state
        if (!config.filter(prevState, action)) return next.dispatch(action)
        val started = System.nanoTime()
        val logEntry = LogEntry<S>(started, config.stateTransformer(prevState), action)
        logBuffer.add(logEntry)

        var returnedValue: Any? = null
        if (config.logErrors) {
            try {
                returnedValue = next.dispatch(action)
            } catch (e: Exception) {
                logEntry.error = config.errorTransformer(e)
            }
        } else {
            returnedValue = next.dispatch(action)
        }
        logEntry.took = Math.round((System.nanoTime() - logEntry.started) / 10.0) / 100.0 //in ms rounded to max two decimals
        logEntry.nextState = config.stateTransformer(store.state)
        //check if diff is activated
        logEntry.diffActivated = if (config.logStateDiff && config.logStateDiffFilter != null) config.logStateDiffFilter.invoke(logEntry.nextState!!, action) else config.logStateDiff
        printBuffer(logBuffer)
        logBuffer.clear()

        if (logEntry.error != null) throw logEntry.error!!
        return returnedValue
    }

    private fun printBuffer(buffer: List<LogEntry<S>>) {
        buffer.forEachIndexed { key, curEntry ->
            var took = curEntry.took
            var nextState = curEntry.nextState
            val nextEntry: LogEntry<S>? = if (key < buffer.size) buffer[key + 1] else null
            if (nextEntry != null) {
                nextState = nextEntry.prevState
                took = Math.round((nextEntry.started - curEntry.started) / 10.0) / 100.0
            }

            //message
            val formattedAction = config.actionTransformer(curEntry.action)
            val isCollapsed = config.collapsed(nextState, curEntry.action)
            val tookstr = took.toString().padStart(5) //took.toFixed(2)
            val durationstr = if (config.logActionDuration) "(in $tookstr ms)" else ""
            val actiontypestr = config.actionTypeExtractor(formattedAction)
            val title = "action @ $actiontypestr $durationstr "

            // Render
            try {
                if (isCollapsed) {
                    //  if (colors.title) logger.groupCollapsed("%c ${title}", titleCSS);
                    //  else
                    logger.groupCollapsed(title)
                } else {
                    //if (colors.title) logger.group("%c ${title}", titleCSS);
                    //else
                    logger.group(title)
                }
            } catch (e: Exception) {
                logger.log(title)
            }

            val prevStateLevel = config.level(LogElement.PREVSTATE, formattedAction, curEntry.prevState, nextState, curEntry.error) //use reduced info?: action and prevState
            val actionLevel = config.level(LogElement.ACTION, formattedAction, curEntry.prevState, nextState, curEntry.error) ///use reduced info?: action
            val errorLevel = config.level(LogElement.ERROR, formattedAction, curEntry.prevState, nextState, curEntry.error) //use reduced info?: action, error, prevState
            val nextStateLevel = config.level(LogElement.NEXTSTATE, formattedAction, curEntry.prevState, nextState, curEntry.error) //use reduced info?: action nextState

            val prevStateJson=stateToJson(curEntry.prevState)
            if (prevStateLevel != null) {
                logger.json("prev state", prevStateJson, prevStateLevel)
            }

            val formattedActionJson=actionToJson(formattedAction)
            if (actionLevel != null) {
                logger.json("action ",formattedActionJson, actionLevel)
            }

            if (curEntry.error != null && errorLevel != null) {
                logger.log("error" + curEntry.error.toString(), errorLevel)

            }

            val nextStateJson:String=stateToJson(nextState)
            if (nextStateLevel != null) {
                logger.json("next state", nextStateJson, nextStateLevel)
            }

            if (config.logStateDiff) {
                diffLogger(prevStateJson, nextStateJson, isCollapsed)
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

    private fun diffLogger(prevStateJsonStr: String, nextStateJsonStr: String, collapsed: Boolean) {
        val prevStateJson=jsonParser.parse(prevStateJsonStr)
        val nextStateJson=jsonParser.parse(nextStateJsonStr)
        val stateDiff=jsonDiffer.asJson(prevStateJson,nextStateJson)
        if (collapsed) {
            logger.groupCollapsed("diff")
        } else {
            logger.group("diff")
        }

        if (stateDiff.size()>2) {
            logger.json("",stateDiff.toString())
        } else {
            logger.log("—— no diff ——")
        }

        logger.groupEnd()
    }
}