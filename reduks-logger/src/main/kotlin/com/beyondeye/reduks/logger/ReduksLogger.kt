package com.beyondeye.reduks.logger

import com.beyondeye.reduks.Middleware
import com.beyondeye.reduks.NextDispatcher
import com.beyondeye.reduks.Store
import com.beyondeye.zjsonpatch.JsonDiff
import com.google.gson.GsonBuilder

/**
 * Reduks Logger middleware
 * Created by daely on 7/21/2016.
 */
class ReduksLogger<S>(val options: ReduksLoggerConfig<S> = ReduksLoggerConfig()) : Middleware<S> {
    private val jsonDiffer= JsonDiff
    var gsonInstance = GsonBuilder().serializeNulls().disableHtmlEscaping().serializeSpecialFloatingPointValues().create()
    private val logger = DefaultGroupedLogger()
    private val logBuffer: MutableList<LogEntry<S>> = mutableListOf() //we need a logBuffer because of possible unhandled exceptions before we print the logEntry
    override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any): Any? {
        // Exit early if predicate function returns 'false'
        val prevState = store.state
        if (!options.filter(prevState, action)) return next.dispatch(action)
        val started = System.nanoTime()
        val logEntry = LogEntry<S>(started, options.stateTransformer(prevState), action)
        logBuffer.add(logEntry)

        var returnedValue: Any? = null
        if (options.logErrors) {
            try {
                returnedValue = next.dispatch(action)
            } catch (e: Exception) {
                logEntry.error = options.errorTransformer(e)
            }
        } else {
            returnedValue = next.dispatch(action)
        }
        logEntry.took = Math.round((System.nanoTime() - logEntry.started)/10.0)/100.0 //in ms rounded to max two decimals
        logEntry.nextState = options.stateTransformer(store.state)
        //check if diff is activated
        logEntry.diffActivated = if(options.logStateDiff&&options.logStateDiffFilter!=null) options.logStateDiffFilter.invoke(logEntry.nextState!!,action) else options.logStateDiff
        printBuffer(logBuffer)
        logBuffer.clear()

        if (logEntry.error != null) throw logEntry.error!!
        return returnedValue
    }

    private fun printBuffer(buffer: List<LogEntry<S>>) {
        buffer.forEachIndexed { key, curEntry ->
            var took = curEntry.took
            var nextState=curEntry.nextState
            val nextEntry: LogEntry<S>? = if(key<buffer.size) buffer[key+1] else null
            if(nextEntry!=null) {
                nextState=nextEntry.prevState
                took=Math.round((nextEntry.started-curEntry.started)/10.0)/100.0
            }

            //message
            val formattedAction=options.actionTransformer(curEntry.action)
            val isCollapsed=options.collapsed(nextState,curEntry.action)
//            val formattedTime=Helpers.formatTime(curEntry.startedTime)
            //val titleCSS = if(options.colors.title!=null) "color: ${options.colors.title(formattedAction)};" else null
//            val timestampstr=if(options.timestamp) formattedTime else ""
            val tookstr=took.toString().padStart(5) //took.toFixed(2)
            val durationstr=if(options.logActionDuration) "(in $tookstr ms)" else ""
            val actiontypestr=options.actionTypeExtractor(formattedAction)
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
            } catch (e:Exception) {
                logger.log(title)
            }

            val prevStateLevel = options.level(LogElement.PREVSTATE,formattedAction,curEntry.prevState,nextState,curEntry.error) //use reduced info?: action and prevState
            val actionLevel = options.level(LogElement.ACTION,formattedAction,curEntry.prevState,nextState,curEntry.error) ///use reduced info?: action
            val errorLevel = options.level(LogElement.ERROR,formattedAction,curEntry.prevState,nextState,curEntry.error) //use reduced info?: action, error, prevState
            val nextStateLevel = options.level(LogElement.NEXTSTATE,formattedAction,curEntry.prevState,nextState,curEntry.error) //use reduced info?: action nextState

            if (prevStateLevel!=null) {
//                if (colors.prevState) logger[prevStateLevel](`%c prev state`, `color: ${colors.prevState(prevState)}; font-weight: bold`, prevState);
//                else
                    logger.log("prev state"+ curEntry.prevState,prevStateLevel)
            }

            if (actionLevel!=null) {
//                if (colors.action) logger[actionLevel](`%c action`, `color: ${colors.action(formattedAction)}; font-weight: bold`, formattedAction);
//                else
                    logger.log("action "+ formattedAction.toString(),actionLevel)
            }

            if (curEntry.error!=null && errorLevel!=null) {
 //               if (colors.error) logger[errorLevel](`%c error`, `color: ${colors.error(error, prevState)}; font-weight: bold`, error);
 //               else
                    logger.log("error"+ curEntry.error.toString(),errorLevel)

            }

            if (nextStateLevel!=null) {
//                if (colors.nextState) logger[nextStateLevel](`%c next state`, `color: ${colors.nextState(nextState)}; font-weight: bold`, nextState);
//               else
                    logger.log("next state"+ nextState,nextStateLevel)
            }

            if (options.logStateDiff) {
                diffLogger(curEntry.prevState, nextState, isCollapsed)
            }

            try {
                logger.groupEnd()
            } catch (e:Exception) {
                logger.log("—— log end ——")
            }

        }
    }

    private fun diffLogger(prevState: S, nextState: S?, collapsed: Boolean):Int? {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}