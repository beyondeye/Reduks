[![Kotlin 1.0](https://img.shields.io/badge/Kotlin-1.0.3-blue.svg)](http://kotlinlang.org)
[![](https://jitpack.io/v/beyondeye/reduks.svg)](https://jitpack.io/#beyondeye/reduks)
[![Build Status](https://travis-ci.org/beyondeye/Reduks.svg?branch=master)](https://travis-ci.org/beyondeye/Reduks)
[![Slack channel](https://img.shields.io/badge/Chat-Slack-green.svg)](https://kotlinlang.slack.com/messages/reduks/)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Reduks-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4245)
# Reduks: a port of Reduxjs for Kotlin+Android

Some notable features:

 - RxJava+RxAndroid support
 
 - Promise middleware based on kovenant promises (http://kovenant.komponents.nl/)
 
 - Thunk middleware
 
 - port of reselect library(https://github.com/reactjs/reselect)
 
 - A powerful logger middleware based on a port of https://github.com/evgenyrodionov/redux-logger, with the text
  formatting engine based on a heavily customized version of https://github.com/orhanobut/logger. 
  The reduks logger middleware allows to print Json diff of subsequent reduks states  thanks to a port to Kotlin of https://github.com/flipkart-incubator/zjsonpatch, with Jackson substituted with Gson and guava dependency removed

 - Support for easily combining multiple reduks modules
 
 - support for saving/restoring store state on activity onSaveInstanceState/onRestoreInstanceState

 - support for [Store Enhancers](https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store-enhancer) (a generalization of middlewares),
  with helper functions to transform middlewares to store enhancers and combine them with other enhancers.

## dependencies for gradle
```groovy
// First, add JitPack to your repositories
repositories {
    ...
    maven { url "https://jitpack.io" }
}

// main reduks package
compile 'com.github.beyondeye.reduks:reduks-core:2.0.0b4'

//rx-java based state store+ additional required dep for android support
compile 'com.github.beyondeye.reduks:reduks-rx:2.0.0b3'
compile 'com.github.beyondeye.reduks:reduks-android:2.0.0b4'

//kovenant based state store and Async Action Middleware
compile 'com.github.beyondeye.reduks:reduks-kovenant:2.0.0b4'
compile 'com.github.beyondeye.reduks:reduks-android:2.0.0b4'


//dev tools
compile 'com.github.beyondeye.reduks:reduks-devtools:2.0.0b4'
```


#An introduction to Reduks
Reduks (similarly to Reduxjs) is basically a simplified Reactive Functional Programming approach for implementing UI for Android

A very good source of material for understanding redux/reduks are [the official reduxjs docs](http://redux.js.org/), but I will try to describe here the main principles, and how they blend with Android and Kotlin

Reduks main components are:

* the __State__: it is basically the same as the Model in the  standard MVC programming paradigm
* State change __subscribers__: their purpose is similar to Controllers in  MVC 
* __Actions__ and __Reducers__: Reducers are (pure)functions that specify how the State change in response to a stream of events (Actions)
* __Middlewares__: additional pluggable layers (functions) on top of Reducers for implementing logic for responding to the stream of events (Actions) or even modify them before they reach the reducers that implement the State change logic.
 Middlewares (together with event change subscribers) have also the main purpose to allow implementing 'side effects', that are prohibited in reducers, that must be pure functions.

There is also an additional component that is called the __Store__ but it is basically nothing more than the implementation details of the "glue" used to connect all the other components.

Its responsibilities are 

* Allows access to the current state
* Allows to send update events to the state  via `dispatch(action)`
* Registers and unregister state change listeners via `subscribe(listener)`

The implementation details of the Store  and their variations can be quite important, but for understanding Reduks, we can start by focusing first on the other components

This is Reduks in brief. let us now discuss it more in detail

##The State
The state is the set of data that uniquely identify the current state of the application. 
Typically in Android, this is the state of the current Activity.

An important requirement for the data inside the state object  is that it is required to be __immutable__, or in other words it is prohibited to update the state directly.

The only way to mutate the state is to send an action via the store dispatch method, to be processed by the registered state reducers(more on this later), that will generate a new updated state. 

The old state must be never modified.

In Kotlin  we will typically implement the state as a data class with all fields defined as val's (immutable)

Example:
```kotlin
data class LoginActivityState(val email:String, val password:String, val emailConfirmed:Boolean)
```
Why using a data class? Because it makes it easier to implement reducers, thanks to the autogenerated `copy()` method.

But if you don't want to use data classes  you can easily implement the `copy()` method like this:
```kotlin
fun copy(email: String?=null, password:String?=null, emailConfirmed:Boolean?=null) =
     LoginActivityState(email ?: this.email,  password ?: this.password,  emailConfirmed ?: this.emailConfirmed)
```
###State Change Subscribers
Before we discuss how the state changes, let's see how we listen to those changes. 
Through the store method
```kotlin
 fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription
```
we register callbacks to be called each time the state is modified (i.e. some action is dispatched to the store).
```kotlin
val curLogInfo=LoginInfo("","")
val subscriber=StoreSubscriber<LoginActivityState> {
    val newState=store.state
    val loginfo=LoginInfo(newState.email,newState.password)
    if(loginfo.email!= curLogInfo.email||loginfo.password!= curLogInfo.password) {
        //log info changed...do something
        curLogInfo= loginfo
    }
}
```

You should have noticed that in the subscriber, in order to get the value of the newState we need to reference our store instance. You shoud always get a reference to the new state at
 the beginning of the subscriber code and then avoid referencing ```store.state``` directly, otherwise you could end up using different values for ```newState```

Notice that we **cannot** subscribe for changes of some **specific field** of the activity state, but only of the **whole** state.
 
 At first this seems strange. But now we will show how using some advanced features of Reduks, we can turn this into an advantage.
 The idea behind Reduks is that all that happens in the application is put into a single stream of events so that debugging and testing the application behavior is much easier.
 
 Being a single stream of events we can apply functional programming ideas to application state changes that also make the behaviour of the application more easy to reason about and allows us to avoid bugs.
 
Reduks allows all this but also working with state changes in a way very similar to traditional callbacks. This is enabled by **Reduks selectors**: instead of writing
 the subscriber as above we can write the following code:
```kotlin
val subscriberBuilder = StoreSubscriberBuilder<ActivityState> { store ->
    val sel = SelectorBuilder<ActivityState>()
    val sel4LoginInfo=sel.withField { email } .withField { password }.compute { e, p -> LoginInfo(e,p)  }
    val sel4email=sel.withSingleField { email }
    StoreSubscriber {
        val newState=store.state
        sel4LoginInfo.onChangeIn(newState) { newLogInfo ->
            //log info changed...send to server for verification
            //...then we received notification that email was verified
            store.dispatch(Action.EmailConfirmed())
        }
        sel4email.onChangeIn(newState) { newEmail ->
            //email changed : do something
        }

    }
}
```
There are a few things to note in this new version of our sample subscriber:
 
 * We are creating a `StoreSubcriberBuilder` that a takes a `Store` argument and returns a `StoreSubscriber`. This is actual the recommended way to build a subscriber.
 The `StoreSubscriberBuilder` takes as argument the store instance, so that inside the subscriber we can get the newState and dispatch new actions to the store.
 * We are creating *selector objects*: their purpose is to automatically detect change in one or more state fields and lazily compute a function of these fields, passing
  its value to a lambda when the method `onChangeIn(newState)` is called.
  
As you can see the code now looks similar to code with Callbacks traditionally used for subscribing to asynchronous updates. 
Selectors can detect quite efficiently changes in the state, thanks to a technique called *memoization* that works because we have embraced immutable data structures for representing the application state
 
Look [here](./reduks/src/test/kotlin/com/beyondeye/reduks/ReselectTest.kt) for more examples on how to build selectors.
  
###Actions and Reducers
As we mentioned above, whenever we want to change the state of the application we need to send(dispatch) an *Action*  object, that will be processed by the *Reducers*,
that are pure functions that take as input the action and the current state and outputs a new modified state.
An action object can be literally any object. For example we can define the following actions
```kotlin
class LoginAction {
    class EmailUpdated(val email:String)
    class PasswordUpdated(val pw:String)
    class EmailConfirmed
}
```
####Reducers
a sample [Reducer](./reduks/src/main/java/com/beyondeye/reduks/Reducer.java) can be the following 
```kotlin
val reducer = Reducer<LoginActivityState> { state, action ->
    when(action) {
        is LoginAction.PasswordUpdated -> state.copy(password = action.pw)
        is LoginAction.EmailUpdated -> state.copy(email = action.email,emailConfirmed = false)
        is LoginAction.EmailConfirmed -> state.copy(emailConfirmed = true)
        else -> state
    }
}
```
Notice that `Reducer` is actually a JAVA SAM interface, and we are leveraging a Kotlin feature that allows to automatically convert a lambda to a SAM interface
Reducers must be pure functions, without side-effects except for updating the state. In particular in a reducer you cannot dispatch actions
####Better Actions with Kotlin sealed classes
You may have noticed a potential source of bugs in our previous reducer code. There is a risk that we simply forget to enumerate all action types in the ```when``` expression.

We can catch this type of errors at compile time thanks to [ Kotlin sealed classes](https://kotlinlang.org/docs/reference/classes.html#sealed-classes).
So we will rewrite our actions as
```kotlin
sealed class LoginAction {
    class EmailUpdated(val email:String) :LoginAction()
    class PasswordUpdated(val pw:String) :LoginAction()
    class EmailConfirmed :LoginAction()
}
```
and our reducer as
```kotlin
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
```

The compiler will give us an error if we forget to list one of ```LoginAction``` subtypes in the ```when``` expression above. Also we don't need the ```else``` case anymore (in the more internal ```when```)
####Even Better Actions with Reduks StandardAction
Reduks  [StandardAction](./reduks/src/main/kotlin/com/beyondeye/reduks/StandardAction.kt) is a base interface for actions that provide a standard way to define actions also for failed/async operations:
 
```kotlin
 interface StandardAction {
     val payload: Any?
     val error: Boolean
 }
```

 We can use this to rewrite our actions as
 
```kotlin
 sealed class LoginAction2(override val payload: Any?=null,
                           override val error:Boolean=false) : StandardAction {
     class EmailUpdated(override val payload:String) : LoginAction2()
     class PasswordUpdated(override val payload:String) : LoginAction2()
     class EmailConfirmed(override val payload: Boolean) : LoginAction2()
 }
```

Notice that we can redefine the type of the payload to the one required by each action type, without even using generics.

Also we redefine the state as
```kotlin
data class LoginActivityState2(val email: String,
                          val password: String,
                          val emailConfirmed: Boolean,
                          val serverContactError:Boolean)
```
And here is our new reducer that handle server errors
 
```kotlin
val reducer2 = Reducer<LoginActivityState2> { s, a ->
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
        }
        else -> s
    }
}
```

####Combining Reducers
When your application start getting complex, your reducer code will start getting difficult too manage.
To solve this problem, Reduks provide the method ```combineReducers``` that allows  splitting the definition of the reducer and even put each part of the definition in a different file. 

 ```combineReducers```  takes a list of reducers and return a reducer that apply each reducer in the list according to the order in the list.
For example:

```kotlin
class Action
{
    class IncrA
    class IncrB
}
data class State(val a:Int=0,val b:Int=0)
val reducerA=Reducer<State>{ state,action-> when(action) {
    is Action.IncrA -> state.copy(a=state.a+1)
    else -> state
}}
val reducerB=Reducer<State>{ state,action-> when(action) {
    is Action.IncrB -> state.copy(b=state.b+1)
    else -> state
}}

val reducerAB=Reducer<State>{ state,action-> when(action) {
    is Action.IncrA -> state.copy(a=state.a*2)
    is Action.IncrB -> state.copy(b=state.b*2)
    else -> state
}}
val reducercombined= combineReducers(reducerA, reducerB, reducerAB)
```
 
Then for action sequence 
```
IncrA, IncrB
``` 

starting from the initial state 

```kotlin
State(a=0,b=0)
```

the combined reducer will produce the finale state
```kotlin
State(a=2,b=2)
```

Note that this is different from how it works in   [reduxjs combineReducers](https://github.com/reactjs/redux/blob/master/docs/api/combineReducers.md). The original reduxjs concept has been
implemented and extended in Reduks Modules (see below)

####If I feel like I want to dispatch from my Reducer what is the correct thing to do instead?
This is one of the most typical things that confuse beginners.

For example let's say that in order to verify the email address at user registration we must

 * make some server API call (that can fail)
 * and then wait for some notification from the server that the user successfully confirmed the email address (or not).
  
So we can think of defining the following actions

* `class LoginApiCalled`
* `class LoginApiFailed`
* `class LoginEmailConfirmed`

It is natural to think, when receiving the action `LoginApiCalled` in the reducer, to add there the relevant logic for this action, namely checking if
   the call failed, and  if the email was confirmed.
    
Another common related  mistake it is to split the logic between multiple store subscribers, for example, in a subscriber that listen for loginApiCalled state changes
to add logic for treating api failed.

If you find yourself in this situation then you should defer dispatching an action when you actually have the result of the whole chain (so in our example dispatching only the action `LoginEmailConfirmed`).
At a later stage you can eventually also split the chain into multiple actions (so that you can update the UI at different stages of the user authentication process), 
but always keep the chain logic in the original place. We will discuss later the [Thunk middleware](./reduks/src/main/kotlin/com/beyondeye/reduks/middlewares/ThunkMiddleware.kt)
and [AsyncAction middleware](./reduks-kovenant/src/main/kotlin/com/beyondeye/reduks/middlewares/AsyncActionMiddleWare.kt) that will help you handle these chains of actions better
###Reduks Modules
TODO
####Combining Reduks modules
TODO
###Reduks Activity
TODO
####Reduks as a communication channel between fragments
TODO
####Persisting Reduks state and activity lifecycle
TODO
###Middlewares
TODO
####Thunk Middleware
TODO
####Promise Middleware
TODO
####Logger Middleware
TODO
###Types of Reduks Stores
TODO
####Simple Store
TODO
####RxJava based Store
TODO
####Promise based (Kovenant) Store
TODO
###Store Enhancers
TODO
####Reduks DevTools
TODO

## Open source library included/modified or that inspired Reduks
- http://redux.js.org/
- https://github.com/brianegan/bansa. (Reduks was actually started as a fork of Bansa)
- https://github.com/reactjs/reselect
- https://github.com/evgenyrodionov/redux-logger
- https://github.com/orhanobut/logger
- https://github.com/flipkart-incubator/zjsonpatch
- https://github.com/acdlite/redux-promise
- https://github.com/gaearon/redux-thunk
- https://github.com/acdlite/flux-standard-action

## License

~~~
The MIT License (MIT)
Copyright (c) 2016 Dario Elyasy

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
~~~
