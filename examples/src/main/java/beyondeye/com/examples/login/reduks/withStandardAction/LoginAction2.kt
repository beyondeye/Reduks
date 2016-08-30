package beyondeye.com.examples.login.reduks.withStandardAction

import com.beyondeye.reduks.StandardAction

/**
 * Created by daely on 8/14/2016.
 */
sealed class LoginAction2(override val payload: Any?=null,
                          override val error:Boolean=false) : StandardAction {
    class EmailUpdated(override val payload:String) : LoginAction2()
    class PasswordUpdated(override val payload:String) : LoginAction2()
    class EmailConfirmed(override val payload: Boolean) : LoginAction2()
}