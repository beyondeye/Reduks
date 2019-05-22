
# Reduks modules in brief
Here is a bird-eye view of Reduks modules and features
## reduks-core
Reduks core implements all basic standard ReduxJS components and interfaces:
- Store, Store Creator,  Store Subscriber
- Middlewares, Store Enhancers
- Thunk  Middleware, Standard Action

reduks-core also contains a basic implementation of the Store interface, called SimpleStore, whose behavior is the most similar to the Store in the original ReduxJS implementation
In addition to all standard ReduxJS stuff,reduks-core  also contains several extensions
- Store Subscriber Builder and reselect library: this is one most important components of reduks
- Basic support for Reduks Modules (reduks substates): reduks modules is the reduks way to support modular UI components

dependencies for gradle for reduks-core
```groovy
// First, add JitPack to your repositories
repositories {
    ...
    maven { url "https://jitpack.io" }
}

// main reduks package
compile 'com.github.beyondeye.reduks:reduks-core:<Reduks_Version>'
```

Some notable features:

 - RxJava+RxAndroid support
 
 - Promise middleware based on kovenant promises (<http://kovenant.komponents.nl/>)
 
 - Thunk middleware
 
 - port of reselect library(<https://github.com/reactjs/reselect>)
 
 - A powerful logger middleware based on a port of <https://github.com/evgenyrodionov/redux-logger>, with the text
  formatting engine based on a heavily customized version of <https://github.com/orhanobut/logger>. 
  The reduks logger middleware allows to print Json diff of subsequent reduks states  thanks to a port to Kotlin of <https://github.com/flipkart-incubator/zjsonpatch>, with Jackson substituted with Gson and guava dependency removed

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
- [Reduks on Android](#reduks_on_android)
    - [Activities](#reduks_activities)
    - [Fragments](#reduks_fragments)
    - [Saving and Restoring Reduks state on Device Configuration Changes](reduks_onsaveinstancestate)
    - [Reduks bus: a communication channel between fragments](#reduksbus)
- [Credits from other open source libraries](#opensource)


<a name="gradledeps"></a>


#### Better Actions with Kotlin sealed classes
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
#### Even Better Actions with Reduks StandardAction
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
#### Combining Reducers
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
#### If I feel like I want to dispatch from my Reducer what is the correct thing to do instead?
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
### Reduks Modules
TODO
#### Combining Reduks modules
TODO

<a name="pcollections"></a>
#### Immutable (Persistent) Collections with Reduks
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
for [reduks bus](#reduksbus)
<a name="reduks_on_android"></a>
## Reduks on Android
<a name="reduks_activities"></a>
### Activities
<a name="reduks_fragments"></a>
### Fragments
<a name="reduks_onsaveinstancestate"></a>
### Saving and Restoring Reduks state on Device Configuration Changes

<a name="reduksbus"></a>
#### Reduks bus: a communication channel between fragments
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
    override val busData: BusData = BusData.empty) :StateWithBusData
{
    override fun copyWithNewBusData(newBusData: BusData): StateWithBusData = copy(busData=newBusData)
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
That's it. See more code examples [here](./reduks-bus/src/test/kotlin/com/beyondeye/reduks/bus/BusStoreEnhancerTest.kt). For the full Api see [here](./reduks-bus/src/main/kotlin/com/beyondeye/reduks/bus/busApi.kt)
##### Reduks bus under the hood
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
#### Reduks bus in Android Fragments
Finally we can show the code for handling communication  between a Fragment and a parent activity, or another Fragment.
We assume that the parent activity implement the [ReduksActivity interface](./reduks-android/src/main/kotlin/com/beyondeye/reduksAndroid/activity/ReduksActivity.kt)
```kotlin
interface  ReduksActivity<S> {
       val reduks: Reduks<S>
   }
```
For posting data on the bus the fragment need to obtain a reference to the [`Reduks`](./reduks/src/main/kotlin/com/beyondeye/reduks/Reduks.kt) object of the parent activity.
 You can get it easily from the parent activity for example by defining the following extension property in the fragment
```kotlin
    fun Fragment.reduks() =
            if (activity is ReduksActivity<*>)
                (activity as ReduksActivity<out StateWithBusData>).reduks
            else null
```
and then we can use it
```
class LoginFragment : Fragment() {
    fun onSubmitLogin() {
        reduks()?.postBusData(LoginFragmentResult("Kotlin","IsAwsome"))
    }
}
```
And in another fragment (or in the parent activity) we can listen for data posted on the bus in this way 
```kotlin
class LoginDataDisplayFragment : Fragment() {
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        reduks()?.addBusDataHandler(tag) { lfr:LoginFragmentResult? ->
            if(lfr!=null) {
                print("login with username=${lfr.username} and password=${lfr.password} and ")
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        reduks()?.removeBusDataHandlersWithTag(tag) //remove all bus data handler attached to this fragment tag
    }
}
```
Notices that we are using the Fragment tag (assuming it is defined) for automatically keeping track of all registered bus data handlers and removing them when the Fragment is detached from the activity
for the full source code of the example discussed see [here](./code_fragments/src/main/java/beyondeye/com/examples/busExample.kt) 

### Middlewares
TODO
#### Thunk Middleware
TODO
#### Promise Middleware
TODO
#### Logger Middleware
TODO
### Types of Reduks Stores
TODO
#### Simple Store
TODO
#### RxJava based Store
TODO
#### Promise based (Kovenant) Store
TODO
### Store Enhancers
TODO
#### Reduks DevTools
TODO

<a name="opensource"></a>
## Open source library included/modified or that inspired Reduks
- <http://redux.js.org/>
- <https://github.com/brianegan/bansa>. (Reduks was actually started as a fork of Bansa)
- <https://github.com/reactjs/reselect>
- <https://github.com/evgenyrodionov/redux-logger>
- <https://github.com/orhanobut/logger>
- <https://github.com/flipkart-incubator/zjsonpatch>
- <https://github.com/acdlite/redux-promise>
- <https://github.com/gaearon/redux-thunk>
- <https://github.com/acdlite/flux-standard-action>
- <https://github.com/hrldcpr/pcollections>
