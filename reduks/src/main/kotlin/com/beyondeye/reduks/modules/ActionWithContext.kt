package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Action

/**
 * Action object with attached ActionContext: this wrapped action is used when combining multiple Reduks modules
 * Created by daely on 7/31/2016.
 */
class ActionWithContext(val action: Any, val context: ReduksContext):Action