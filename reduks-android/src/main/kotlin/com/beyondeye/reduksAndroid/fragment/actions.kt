package com.beyondeye.reduksAndroid.fragment

/**
 * set the fragment active status
 */
class ActionSetFragmentActiveStatus(val fragmentTag: String, val newActiveStatus: FragmentActiveStatus)

/**
 * mark the fragment with the specified [newFragmentTag] as the current fragment in the UI at the
 * position specified by [positionTag]
 */
class ActionSetFragmentCurAtPos(val newFragmentTag: String, val positionTag:String)