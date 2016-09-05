package beyondeye.com.examples.login.reduks

import beyondeye.com.examples.LoginInfo
import com.beyondeye.reduks.SelectorBuilder
import com.beyondeye.reduks.StoreSubscriber
import com.beyondeye.reduks.StoreSubscriberBuilder

/**
 * Created by daely on 8/16/2016.
 */
val curLogInfo = LoginInfo("", "")
val subscriber1 = StoreSubscriberBuilder<ActivityState> { store ->
    StoreSubscriber {
        val newState=store.state
        val loginfo = LoginInfo(newState.email, newState.password)
        if (loginfo.email != curLogInfo.email || loginfo.password != curLogInfo.password) {
            //log info changed...send to server for verification

        }
    }
}
val subscriberBuilder = StoreSubscriberBuilder<ActivityState> { store ->
    val sel = SelectorBuilder<ActivityState>()
    val sel4LoginInfo=sel.withField { email } .withField { password }.compute { e, p -> LoginInfo(e,p)  }
    val sel4email=sel.withSingleField { email }
    StoreSubscriber {
        val newState=store.state
        sel4LoginInfo.onChangeIn(newState) { newLogInfo ->
            //log info changed...send to server for verification
            //...then we received notification that email was verified
            store.dispatch(LoginAction.EmailConfirmed())
        }
        sel4email.onChangeIn(newState) { newEmail ->
            //email changed : do something
        }

    }
}