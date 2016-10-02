package com.beyondeye.reduks.logger

import android.os.Build
import com.beyondeye.reduks.ReducerFn
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
@Config(constants = BuildConfig::class,sdk = intArrayOf(Build.VERSION_CODES.M))
class ReduksLoggerTest {

    @Before
    fun setUp() {

    }
    data class TestState(val component1: String = "initial state",val component2: String = "initial state")
    data class TestAction(val type: String = "unknown")
    val testReducer = ReducerFn<TestState> { state, action ->
        when (action) {
            is TestAction -> when (action.type) {
                "reduce component1" -> state.copy(component1 = "reduced")
                "reduce component2" -> state.copy(component2 = "reduced")
                else -> state
            }
            else -> state
        }
    }
    @Test
    fun test_nano2ms() {
        val start=0L
        val end =123456789L
        val diff=ReduksLogger.nano2ms(start,end)
        assertThat(diff).isEqualTo(123.46)
    }

    @Test
    fun test_simple_action_changing_one_field_of_state() {
        //-----GIVEN
        val store = SimpleStore(TestState(), testReducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true,borderDividerLength = 20))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)
        //-----WHEN
        store.dispatch(TestAction(type = "reduce component1"))
        //-----THEN
        val logstr=loggerMiddleware.getLogAsString()
        assertTextEqualToFileContent(logstr, "simple_action_changing_one_field_of_state")
    }
    @Test
    fun test_simple_action_changing_one_field_state_diff() {
        //-----GIVEN
        val store = SimpleStore(TestState(), testReducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(
                logStateDiff = true,
                reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true,borderDividerLength = 20))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)
        //-----WHEN
        store.dispatch(TestAction(type = "reduce component1"))
        //-----THEN
        val logstr=loggerMiddleware.getLogAsString()
        assertTextEqualToFileContent(logstr, "simple_action_changing_one_field_state_diff")
    }
    @Test
    fun test_simple_action_changing_one_field_of_state_collapsed() {
        //-----GIVEN
        val store = SimpleStore(TestState(), testReducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(
                collapsed = { state,action-> true },
                reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true,borderDividerLength = 20))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)
        //-----WHEN
        store.dispatch(TestAction(type = "reduce component1"))
        //-----THEN
        val logstr=loggerMiddleware.getLogAsString()
        assertTextEqualToFileContent(logstr, "simple_action_changing_one_field_of_state_collapsed")
    }
    @Test
    fun test_simple_action_changing_one_field_state_diff_collapsed() {
        //-----GIVEN
        val store = SimpleStore(TestState(), testReducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(
                collapsed = { state,action-> true },
                logStateDiff = true,
                reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true,borderDividerLength = 20))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)
        //-----WHEN
        store.dispatch(TestAction(type = "reduce component1"))
        //-----THEN
        val logstr=loggerMiddleware.getLogAsString()
        assertTextEqualToFileContent(logstr, "simple_action_changing_one_field_state_diff_collapsed")
    }
    @Test
    fun test_simple_action_changing_one_field_of_state_collapsed_noborder() {
        //-----GIVEN
        val store = SimpleStore(TestState(), testReducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(
                collapsed = { state,action-> true },
                reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true,isBorderEnabled = false))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)
        //-----WHEN
        store.dispatch(TestAction(type = "reduce component1"))
        //-----THEN
        val logstr=loggerMiddleware.getLogAsString()
        assertTextEqualToFileContent(logstr, "simple_action_changing_one_field_of_state_collapsed_noborder")
    }
    @Test
    fun test_simple_action_changing_one_field_state_diff_collapsed_noborder() {
        //-----GIVEN
        val store = SimpleStore(TestState(), testReducer)
        val loggerConfig=ReduksLoggerConfig<TestState>(
                collapsed = { state,action-> true },
                logStateDiff = true,
                reduksLoggerTag = "_RDKS_",formatterSettings = LogFormatterSettings(isLogToString = true,isBorderEnabled = false))
        val loggerMiddleware=ReduksLogger<TestState>(loggerConfig)
        store.applyMiddleware(loggerMiddleware)
        //-----WHEN
        store.dispatch(TestAction(type = "reduce component1"))
        //-----THEN
        val logstr=loggerMiddleware.getLogAsString()
        assertTextEqualToFileContent(logstr, "simple_action_changing_one_field_state_diff_collapsed_noborder")
    }

    private fun assertTextEqualToFileContent(text: String, expectedTextFilename: String) {
        val logstr = text.filterNot { it == '\r' }
        val expected = getTextFromTestDataTextFile(expectedTextFilename)
        assertThat(logstr).isEqualTo(expected)
    }

    private fun getTextFromTestDataTextFile(fileName: String): String {
        val path = "/testdata/$fileName.txt"
        val resourceAsStream = ReduksLoggerTest::class.java.getResourceAsStream(path)
        return IOUtils.toString(resourceAsStream, "UTF-8").filterNot { it=='\r' || it=='\uFEFF' }  //clean up text read, from spurious chars
    }
}