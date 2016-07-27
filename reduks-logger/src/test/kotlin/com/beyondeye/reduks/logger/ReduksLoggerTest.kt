package com.beyondeye.reduks.logger

import android.os.Build
import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.SimpleStore
import com.beyondeye.reduks.logger.logformatter.LogFormatterSettings
import com.beyondeye.reduks.middlewares.applyMiddleware
import com.beyondeye.reduks_logger.BuildConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by daely on 7/27/2016.
 * use robolectric because LogFormatterPrinter depends from org.json in android sdk
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class ReduksLoggerTest {

    @Before
    fun setUp() {

    }
    data class TestState(val component1: String = "initial state",val component2: String = "initial state")
    data class TestAction(val type: String = "unknown")
    @Test
    fun test_nano2ms() {
        val start=0L
        val end =123456789L
        val diff=ReduksLogger.nano2ms(start,end)
        assertThat(diff).isEqualTo(123.46)
    }

    @Test
    fun test_logoutput_for_simple_action_changing_one_field_of_state() {
        val reducer = Reducer<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "reduce component1" -> state.copy(component1 = "reduced")
                    "reduce component2" -> state.copy(component2 = "reduced")
                    else -> state
                }
                else -> state
            }
        }

        val store = SimpleStore(TestState(), reducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)

        store.dispatch(TestAction(type = "reduce component1"))
        val logstr=loggerMiddleware.getLogAsString()

//        Assertions.assertThat(store.state.message).isEqualTo("reduced")
    }
}