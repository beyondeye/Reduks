package com.beyondeye.reduks.logger

/**
 *
 * Created by daely on 7/21/2016.
 */
data class ReduksLoggerConfig<S>(
        /**
         *  activate log only if filter function returns true
         */
        val filter:(prevState:S,action:Any)->Boolean={state,action -> true},
        /**
         * Show diff between states.
         */
        val logStateDiff:Boolean = false,
        /**
         * Filter function for showing states diff.
         */
        val logStateDiffFilter:((nextState:S,action:Any)->Boolean)?=null,
        /**
         * Print the duration of each action?
         */
        val logActionDuration:Boolean=false,
        /**
         * Should the logger catch, log, and re-throw errors?
         */
        val logErrors:Boolean=true,
        /**
         * define log level as a function of log element and current log entry data
         */
        val level:(logElement:Int,Action:Any,prevState:S,nextState:S?,error:Throwable?)->Int?={le,a,ps,ns,e-> LogLevel.DEBUG },
        /**
         *  Returns `true` if current log entry should be collapsed
         */
        val collapsed:(nextState:S?,action:Any)->Boolean={state,action -> false}, //
        /**
         * Transform state before processing it with reduks logger. By default no transformation
         */
        val stateTransformer:(S)->S={s->s},
        /**
         * Transform action before processing it with reduks logger. By default no transformation
         */
        val actionTransformer:(Any)->Any={a->a},
        /**
         * extract action type as string: by default return action class name
         */
        val actionTypeExtractor:(Any)->String = {a->a.javaClass.simpleName},
        /**
         *  Transform error before print. By default no transformation
         */
        val errorTransformer:(Throwable)->Throwable = {e->e},

        /**
         * main tag to use for logger output
         */
        val reduksLoggerTag:String="REDUKS",
        /**
         * if true, write all log message to a buffer, instead of using the android log
         */
        val logToStringBuffer: Boolean=false
)