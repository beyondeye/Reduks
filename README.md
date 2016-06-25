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
compile 'com.github.beyondeye.reduks:reduks-core:1.5.2'

//rx-java based state store
compile 'com.github.beyondeye.reduks:reduks-rx:1.5.2'

//kovenant based state store and Async Action Middleware
compile 'com.github.beyondeye.reduks:reduks-kovenant:1.5.2'

//dev tools
compile 'com.github.beyondeye.reduks:reduks-devtools:1.5.2'

```


## License

~~~
The MIT License (MIT)
Copyright (c) 2016 Dario Elyasy

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
~~~
