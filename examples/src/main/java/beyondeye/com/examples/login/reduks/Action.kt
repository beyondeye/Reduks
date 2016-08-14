package beyondeye.com.examples.login.reduks

/**
 * Created by daely on 8/14/2016.
 */
class Action {
    class EmailUpdated(val email:String)
    class PasswordUpdated(val pw:String)
    class EmailConfirmed
}