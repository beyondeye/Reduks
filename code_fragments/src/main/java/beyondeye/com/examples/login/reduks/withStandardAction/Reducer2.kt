package beyondeye.com.examples.login.reduks.withStandardAction

import com.beyondeye.reduks.ReducerFn

/**
 * Created by daely on 8/14/2016.
 */
val reducer2 = ReducerFn<ActivityState2> { s, a ->
    when {
        a is LoginAction2 -> when (a) {
            is LoginAction2.PasswordUpdated ->
                s.copy(password = a.payload,serverContactError = false)
            is LoginAction2.EmailUpdated ->
                s.copy(email = a.payload, emailConfirmed = false,serverContactError = false)
            is LoginAction2.EmailConfirmed ->
                if(a.error)
                    s.copy(serverContactError = true)
                else
                    s.copy(emailConfirmed = a.payload)
            else -> s
        }
        else -> s
    }
}
