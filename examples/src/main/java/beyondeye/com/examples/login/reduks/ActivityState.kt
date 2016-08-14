package beyondeye.com.examples.login.reduks

/**
 * Created by daely on 8/14/2016.
 */
class ActivityState(val email: String, val password: String, val emailConfirmed: Boolean) {
    fun copy(email: String? = null, password: String? = null, emailConfirmed: Boolean?=null) =
            ActivityState(email ?: this.email, password ?: this.password, emailConfirmed ?: this.emailConfirmed)
}