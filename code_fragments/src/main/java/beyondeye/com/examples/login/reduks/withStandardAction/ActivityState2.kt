package beyondeye.com.examples.login.reduks.withStandardAction

/**
 * Created by daely on 8/14/2016.
 */
data class ActivityState2(val email: String,
                          val password: String,
                          val emailConfirmed: Boolean,
                          val serverContactError:Boolean)