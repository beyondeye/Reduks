[![Kotlin 1.0](https://img.shields.io/badge/Kotlin-1.0.5-blue.svg)](http://kotlinlang.org)
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

 - integration with a  [persistent collections library ](https://github.com/hrldcpr/pcollections) for better perfomance with complex states
 
 - Integration with an Event Bus implemented as Store enhancer
 
##### Table of Contents 
- [Gradle dependencies](#gradledeps)
- [Introduction to Reduks](#reduks_intro)
- [The state](#the_state)
- [State change subscribers](#subscribers)
- [Actions and Reducers](#actions)
    - [Bettern actions with sealed classes](#sealedclasses)
    - [Even better actions with StandardAction](#standardaction)
- [Combining Reducers](#combinereducers)
- [No dispatch from reducers!](#dispatch_from_reducer)
- [Immutable Collections with Reduks](#pcollections)
- [Reduks bus: a communication channel between fragments](#reduksbus)
- [Credits from other open source libraries](#opensource)

<a name="gradledeps"></a>
## dependencies for gradle
```groovy
// First, add JitPack to your repositories
repositories {
    ...
    maven { url "https://jitpack.io" }
}

// main reduks package
compile 'com.github.beyondeye.reduks:reduks-core:2.0.0b11'

//rx-java based state store+ additional required dep for android support
compile 'com.github.beyondeye.reduks:reduks-rx:2.0.0b11'
compile 'com.github.beyondeye.reduks:reduks-android:2.0.0b11'

//kovenant based state store and Async Action Middleware
compile 'com.github.beyondeye.reduks:reduks-kovenant:2.0.0b11'
compile 'com.github.beyondeye.reduks:reduks-android:2.0.0b11'


//dev tools
compile 'com.github.beyondeye.reduks:reduks-devtools:2.0.0b11'

//immutable collections
compile 'com.github.beyondeye.reduks:reduks-pcollections:2.0.0b11'

//reduks bus
compile 'com.github.beyondeye.reduks:reduks-pcollections:2.0.0b11'
compile 'com.github.beyondeye.reduks:reduks-bus:2.0.0b11'

```


<a name="reduks_intro"></a>
# An introduction to Reduks
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

<a name="the_state"></a>
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

<a name="subscribers"></a>
###State Change Subscribers
Before we discuss how the state changes, let's see how we listen to those changes. 
Through the store method
```kotlin
 fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription
```
we register callbacks to be called each time the state is modified (i.e. some action is dispatched to the store).
```kotlin
val curLogInfo=LoginInfo("","")
val subscriber=StoreSubscriberFn<LoginActivityState> {
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
val subscriberBuilder = StoreSubscriberBuilderFn<ActivityState> { store ->
    val sel = SelectorBuilder<ActivityState>()
    val sel4LoginInfo=sel.withField { email } .withField { password }.compute { e, p -> LoginInfo(e,p)  }
    val sel4email=sel.withSingleField { email }
    StoreSubscriberFn {
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
 
 * We are creating a `StoreSubcriberBuilderFn` that a takes a `Store` argument and returns a `StoreSubscriber`. This is actual the recommended way to build a subscriber.
 The `StoreSubscriberBuilderFn` takes as argument the store instance, so that inside the subscriber we can get the newState and dispatch new actions to the store.
 * We are creating *selector objects*: their purpose is to automatically detect change in one or more state fields and lazily compute a function of these fields, passing
  its value to a lambda when the method `onChangeIn(newState)` is called.
  
As you can see the code now looks similar to code with Callbacks traditionally used for subscribing to asynchronous updates. 
Selectors can detect quite efficiently changes in the state, thanks to a technique called *memoization* that works because we have embraced immutable data structures for representing the application state
 
Look [here](./reduks/src/test/kotlin/com/beyondeye/reduks/ReselectTest.kt) for more examples on how to build selectors.

<a name="actions"></a>  
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
val reducer = ReducerFn<LoginActivityState> { state, action ->
    when(action) {
        is LoginAction.PasswordUpdated -> state.copy(password = action.pw)
        is LoginAction.EmailUpdated -> state.copy(email = action.email,emailConfirmed = false)
        is LoginAction.EmailConfirmed -> state.copy(emailConfirmed = true)
        else -> state
    }
}
```
Reducers must be pure functions, without side-effects except for updating the state. In particular in a reducer you cannot dispatch actions

<a name="sealedclasses"></a>  
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
val reducer = ReducerFn<ActivityState> { state, action ->
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
Note that the exhaustive check is activated only for [when expressions](https://kotlinlang.org/docs/reference/control-flow.html#when-expression), i.e. when we actually use the result of
the  ```when``` block, like in the code above.

<a name="standardaction"></a>  
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
val reducer2 = ReducerFn<LoginActivityState2> { s, a ->
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

<a name="combinereducers"></a>
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
val reducerA=ReducerFn<State>{ state,action-> when(action) {
    is Action.IncrA -> state.copy(a=state.a+1)
    else -> state
}}
val reducerB=ReducerFn<State>{ state,action-> when(action) {
    is Action.IncrB -> state.copy(b=state.b+1)
    else -> state
}}

val reducerAB=ReducerFn<State>{ state,action-> when(action) {
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

<a name="dispatch_from_reducer"></a>
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

<a name="pcollections"></a>
####Immutable (Persistent) Collections with Reduks
A critical component, from a performance point of view, when defining complex Reduks states are so called _persistent collections_ , that is collections
that when modified always create a copy of the original collection, with efficient data sharing mechanims between multiple versions of the modified collections.
Unfortunately there are not yet persistent collection in kotlin standard library (there is [a proposal](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/proposal.md)).
But there are several implementations of persistent collections for the JVM. Some notable ones
- [capsule](https://github.com/usethesource/capsule) from the author of the CHAMP state of the art algorithm.
- [Dexx](https://github.com/andrewoma/dexx): mainly a port of Scala collections to Kotlin.
- [Paguro](https://github.com/GlenKPeterson/Paguro): based on Clojure collections (formerly known as UncleJim).
- [PCollections](https://github.com/hrldcpr/pcollections).
For a discussion of performance of various implementations see [here](https://github.com/Kotlin/kotlinx.collections.immutable/issues/6).
Currently the `reduks-pcollections` module include a stripped down version of the pcollections library (only [Pmap](./reduks-pcollections/src/main/kotlin/com/beyondeye/reduks/pcollections/PMap.java) and [PStack](./reduks-pcollections/src/main/kotlin/com/beyondeye/reduks/pcollections/PStack.java)).
Although it is not the most efficient implementation, it is not too far behind for maps, it has low method count and play well with standard Java collections. It is used as the building block
for reduks bus store enhancer (see below)

<a name="reduksbus"></a>
####Reduks bus: a communication channel between fragments
The official method in Android for communicating results from a fragment to the parent activity or between fragments are [callback interfaces](http://developer.android.com/training/basics/fragments/communicating.html).
This design pattern is very problematic, as it is proven by the success of libraries like [Square Otto](https://github.com/square/otto) and [GreenRobot EventBus](https://github.com/greenrobot/EventBus).
Reduks architecture has actually severally things in common with  an event bus 

<img src="https://github.com/greenrobot/EventBus/blob/master/EventBus-Publish-Subscribe.png" width="500" height="187"/>

So why not implementing a kind of event bus on top of Reduks? This is what the [BusStoreEnhancer](./reduks-bus/src/main/kotlin/com/beyondeye/reduks/bus/BusStoreEnhancer.kt)  is for.
 It is not a real event bus, but it is perfectly fit for the purpose of communicating data between fragments (and more). Let's see how it is done. 
 Let's say for example that our state class is defined as
 
 ```kotlin
 data class State(val a:Int, val b:Int)
 ```
 with actions and reducers defined as follows
 ```kotlin
 class Action
 {
     class SetA(val newA:Int)
     class SetB(val newB:Int)
 }
 val reducer = ReducerFn<State> { state, action ->
     when (action) {
         is Action.SetA -> state.copy(a= action.newA)
         is Action.SetB -> state.copy(b= action.newB)
         else -> state
     }
 }
 ```
 
 In order to enable support for reduks bus,  your Reduks state class need to implement the [StateWithBusData interface](./reduks-bus/src/main/kotlin/com/beyondeye/reduks/bus/StateWithBusData.kt):
```kotlin
data class State(val a:Int, val b:Int, 
    override val busData: PMap<String, Any> = emptyBusData()) :StateWithBusData 
{
    override fun copyWithNewBusData(newBusData: PMap<String, Any>): StateWithBusData = copy(busData=newBusData)
}
```
Basically we add a ```busData``` field (that is a persistent map) and we define a method that Reduks will use to create a new state with an updated version
 of this ```busData``` (something similar of the standard copy() method for data classes, which is actually used for implementation in the example above).
The next change we need is the when we create our store. Now we need pass an instance of ```BusStoreEnhancer```:
```kotlin
 val initialState=State(0,0)
 val creator= SimpleStore.Creator<AState>()
 val store = creator.create(reducer, initialState, BusStoreEnhancer())
```
That's it! Now you can add a bus subscriber for some specific data type that is sent on the bus. For example for a subscriber that should receive updates for
```kotlin
class LoginFragmentResult(val username:String, val password:String)
```
you add a subscriber like this
```kotlin
store.addBusDataHandler { lfr:LoginFragmentResult? ->
    if(lfr!=null) {
        print("login with username=${lfr.username} and password=${lfr.password} and ")
    }
}
```
Simple! Note that the data received in the BusDataHandler must be always define as nullable. The explanation why in a moment (and how Reduks bus works under the hood).
But first let's see how we post some data on the bus:
```kotlin
 store.postBusData(LoginFragmentResult(username = "Kotlin", password = "IsAwsome"))
```
That's it. See more code examples [here](./reduks-bus/src/test/kotlin/com/beyondeye/reduks/bus/BusStoreEnhancerTest.kt). For the full Api see [here](./reduks-bus/src/main/kotlin/com/beyondeye/reduks/bus/api.kt)
#####Reduks bus under the hood
What's happening when we post some data on the bus? What we are doing is actualling dispatching the Action
```kotlin
class ActionSendBusData(val key: String, val newData: Any)
```
with the object class name as `key` (this is actually customizable) and object data as `newData`. This action is automatically intercepted 
by a reducer added by the `BusStoreEnhancer` and translated in a call to `copyWithNewBusData` for updating the map `busData` in the state with
the new value. The bus data handler that we added in the code above is actually a store subscriber that watch for changes (and only for changes) of
data in the `busData` map for the specific key equal to the object class name.
As you can see what we have implemented is not really an Event Bus, because we do not support a stream of data, we only support the two states: 

- some data present for the selected key
- no data present

the `no data present` state is triggered when we call
```kotlin
store.clearBusData<LoginFragmentResult>()
```
that will clear the bus data for the specified object type and trigger the bus data handler with a null value as input.
####Reduks bus in Android Fragments
Finally we can show the code for handling communication  between a Fragment and a parent activity, or another Fragment.
We assume that the parent activity implement the [ReduksActivity interface](./reduks-android/src/main/kotlin/com/beyondeye/reduksAndroid/activity/ReduksActivity.kt)
```kotlin
interface  ReduksActivity<S> {
       val reduks: Reduks<S>
   }
```
For posting data on the bus the fragment need to obtain a reference to the [`Reduks`](./reduks/src/main/kotlin/com/beyondeye/reduks/Reduks.kt) object of the parent activity. It can be easily done overriding the `onAttach()` method:
```kotlin
fun Context.bindReduksFromParentActivity(): Reduks<out StateWithBusData>? =
        if (this is ReduksActivity<*>) {
            this.reduks as? Reduks<out StateWithBusData>
        } else {
            throw RuntimeException(this.toString() + " must implement ReduksActivity<out StateWithBusData>")
        }

class LoginFragment : Fragment() {
    private var reduks: Reduks<out StateWithBusData>?=null
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        reduks=context?.bindReduksFromParentActivity()
    }

    override fun onDetach() {
        super.onDetach()
        reduks=null
    }
    fun onSubmitLogin() {
        reduks?.postBusData(LoginFragmentResult("Kotlin","IsAwsome"))
    }
}
```
And in another fragment (or in the parent activity) we can listen for data posted on the bus in this way 
```kotlin
class LoginDataDisplayFragment : Fragment() {
    private var reduks: Reduks<out StateWithBusData>?=null
    val busHandlers:MutableList<StoreSubscription> = mutableListOf()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        reduks=context?.bindReduksFromParentActivity()
        reduks?.addBusDataHandler { lfr:LoginFragmentResult? ->
            if(lfr!=null) {
                print("login with username=${lfr.username} and password=${lfr.password} and ")
            }
        }?.addToList(busHandlers)
    }

    override fun onDetach() {
        super.onDetach()
        reduks?.removeBusDataHandlers(busHandlers)
        reduks=null
    }
}
```

for the full source code of the example discussed see [here](./code_fragments/src/main/java/beyondeye/com/examples/busExample.kt) 
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

<a name="opensource"></a>
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
- https://github.com/hrldcpr/pcollections

## License

~~~
The MIT License (MIT)
Copyright (c) 2016 Dario Elyasy

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
~~~
