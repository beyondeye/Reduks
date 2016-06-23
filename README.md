[![](https://jitpack.io/v/beyondeye/reduks.svg)](https://jitpack.io/#beyondeye/reduks)

# Reduks: a port of Reduxjs for Kotlin+Android

This library was started as a fork of https://github.com/brianegan/bansa. Thanks a lot to Brian Egan for his work on bansa
While bansa is focused on Java, Reduks is totally focused on making the best, fully battery included port of reduxjs for Kotlin+Android.
it currently has the following features that bansa misses.

 - RxJava+RxAndroid support
 
 - Promise middleware based on kovenant promises (http://kovenant.komponents.nl/)
 
 - Thunk middleware
 
 - Thread safe action dispatch from multiple threads, in my implementation of State store based on Kovenant promises
  (see https://github.com/beyondeye/Reduks/blob/master/reduks/src/main/kotlin/com/beyondeye/reduks/KovenantStore.kt)
   see also this issue in original bansa repository: [Is it safe to call dispatch from multiple threads](https://github.com/brianegan/bansa/issues/24)
 - port of reselect library(https://github.com/reactjs/reselect)

##Planned:
support for persisting store state on activity/fragment lifecycle events

## dependencies for gradle
```groovy
// First, add JitPack to your repositories
repositories {
    ...
    maven { url "https://jitpack.io" }
}

// main reduks package
compile 'com.github.beyondeye.reduks:reduks-core:v1.4.0'


//rx-java support
compile 'com.github.beyondeye.reduks:reduks-rx:v1.4.0'
```