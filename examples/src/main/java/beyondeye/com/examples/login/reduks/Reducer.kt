package beyondeye.com.examples.login.reduks

import com.beyondeye.reduks.Reducer

/**
 * Created by daely on 8/14/2016.
 */
val reducer = Reducer<ActivityState> { state, action ->
    when {
        action is LoginAction -> when (action) {
            is LoginAction.PasswordUpdated -> state.copy(password = action.pw)
            is LoginAction.EmailUpdated -> state.copy(email = action.email, emailConfirmed = false)
            is LoginAction.EmailConfirmed -> state.copy(emailConfirmed = true)
        }
        else -> state
    }
}
