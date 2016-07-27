[![](https://jitpack.io/v/beyondeye/reduks.svg)](https://jitpack.io/#beyondeye/reduks)

# Reduks: a port of Reduxjs for Kotlin+Android

This library was started as a fork of https://github.com/brianegan/bansa: while bansa is focused on Java, Reduks is totally focused on making the best, fully battery included port of reduxjs for Kotlin+Android.
it currently has the following features that bansa misses.

 - RxJava+RxAndroid support
 
 - Promise middleware based on kovenant promises (http://kovenant.komponents.nl/)
 
 - Thunk middleware
 
 - Thread safe action dispatch from multiple threads, in my implementation of State store based on Kovenant promises
  (see https://github.com/beyondeye/Reduks/blob/master/reduks-kovenant/src/main/kotlin/com/beyondeye/reduks/KovenantStore.kt)
   see also this issue in original bansa repository: [Is it safe to call dispatch from multiple threads](https://github.com/brianegan/bansa/issues/24)

 - port of reselect library(https://github.com/reactjs/reselect)
 
 - A powerful logger middleware based on a port of https://github.com/evgenyrodionov/redux-logger, with the text
  formatting engine based on a heavily customized version of https://github.com/orhanobut/logger. 
  The reduks logger middleware allows to print Json diff of subsequent reduks states  thanks to a port to Kotlin of https://github.com/flipkart-incubator/zjsonpatch, with Jackson substituted with Gson and guava dependency removed

##Planned:
support for persisting store state on activity/fragment lifecycle events. Reduks modules composing utilities

## dependencies for gradle
```groovy
// First, add JitPack to your repositories
repositories {
    ...
    maven { url "https://jitpack.io" }
}

// main reduks package
compile 'com.github.beyondeye.reduks:reduks-core:1.6.0'

//rx-java based state store
compile 'com.github.beyondeye.reduks:reduks-rx:1.6.0'

//kovenant based state store and Async Action Middleware
compile 'com.github.beyondeye.reduks:reduks-kovenant:1.6.0'

//dependencies for using the logger
compile 'com.github.beyondeye.reduks:reduks-logger:1.6.0'
compile 'com.github.beyondeye.reduks:zjsonpatch:1.6.0'

//dev tools
compile 'com.github.beyondeye.reduks:reduks-devtools:1.5.2'

```

#An introduction to Reduks
Reduks (similarly to Reduxjs) is basically a simplified Reactive Functional Programming approach for implementing UI for Android
A very good source of material for understanding redux/reduks is [the official reduxjs docs](http://redux.js.org/), but I will try to describe here the main principles, and how they blend with Android and Kotlin

Reduks main components are:

* the __State__: it is basically the same as the Model in the  standard MVC programming paradigm
* State change __subscribers__: their purpose is similar to Controllers in  MVC 
* __Actions__ and __Reducers__: Reducers are (pure)functions that specify how the State change in response to events (Actions)
* __Middlewares__: additional pluggable layers (functions) on top of Reducers for implementing logic for responding to the stream of events (Actions) or even modify them before they reach the reducers that implement the State change logic.
 Middlewares (together with event change subscribers) have also the main purpose to allow implementing 'side effects', that are prohibited in reducers, that must be pure functions.

There is also an additional component that is called the __Store__ but it is basically nothing more that the implementation details of the "glue" used to connect all the other components
Its responsibilities are 

* Allows access to the current state
* Allows to send update events to the state  via `dispatch(action)`
* Registers and unregister state change listeners via `subscribe(listener)`

This is Reduks in brief. let us now discuss it more in detail

##the State
The State is the full information that uniquely identify the current state of the application (typically in Android, the state of the current Activity).

An important property of the State data is that it is __immutable__, or in other words it is prohibited to update the state directly. The only way to mutate the 
state is to send an action via the Store dispatch method, to be processed by the registered state reducers(more on this later), that will generate a new updated state. The old state must be never modified.

In Kotlin  you will typically implement the state as a Data Class will all fields defined as val's (immutable)

Example:

## License

~~~
The MIT License (MIT)
Copyright (c) 2016 Dario Elyasy

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
~~~
