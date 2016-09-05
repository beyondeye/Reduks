package beyondeye.com.examples.login.reduks

/**
 * Created by daely on 8/14/2016.
 */
sealed class LoginAction {
    class EmailUpdated(val email:String) :LoginAction()
    class PasswordUpdated(val pw:String) :LoginAction()
    class EmailConfirmed :LoginAction()
}