[![Kotlin 1.2.10](https://img.shields.io/badge/Kotlin-1.2.10-blue.svg)](http://kotlinlang.org)
[![](https://jitpack.io/v/beyondeye/kjsonpatch.svg)](https://jitpack.io/#beyondeye/kjsonpatch)
[![Build Status](https://travis-ci.org/beyondeye/kjsonpatch.svg?branch=master)](https://travis-ci.org/beyondeye/kjsonpatch)
# KJsonPatch: a JSonPatch implementation in kotlin
This is a port to Kotlin of [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) with
 - Jackson substituted with Gson
 - Guava dependency removed
 - code needed from dependency from Apache Commons Collections 4.1 incorporated

The main motivation is making this library more suitable to uses in Android applications

see the original [README](./README_zjsonpatch.md) for more informations

<a name="gradledeps"></a>
## dependencies for gradle
```groovy
// First, add JitPack to your repositories
repositories {
    ...
    maven { url "https://jitpack.io" }
}

dependencies {
        compile 'com.github.beyondeye:kjsonpatch:0.3.1'
}

```

## License
Copyright (c) 2017 Dario Elyasy
[Apache license 2.0](./LICENSE) 
