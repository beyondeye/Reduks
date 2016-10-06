package com.beyondeye.reduks.bus

/**
 * send data with specific key on the bus
 */
class ActionSendBusData(val key: String, val newData: Any)

/**
 * clear bus data with the specified key
 */
class ActionClearBusData(val key: String)