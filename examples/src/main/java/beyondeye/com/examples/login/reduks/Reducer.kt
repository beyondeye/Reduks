package beyondeye.com.examples.login.reduks

import com.beyondeye.reduks.Reducer

/**
 * Created by daely on 8/14/2016.
 */
val reducer = Reducer<ActivityState> { state, action ->
    when(action) {
        is Action.PasswordUpdated -> state.copy(password = action.pw)
        is Action.EmailUpdated -> state.copy(email = action.email,emailConfirmed = false)
        is Action.EmailConfirmed -> state.copy(emailConfirmed = true)
        else -> state
    }
}
