## dependencies for gradle
Assuming that you have defined the version of reduks you want to use in your ```gradle.properties```
files like this:
```groovy
reduks_version=3.3.4
```
and have added jitpack to your repositories
```groovy
repositories {
    maven { url "https://jitpack.io" }
}
```




```groovy
// main reduks package
compile 'com.github.beyondeye.reduks:reduks-core:$reduks_version'

//rx-java based state store+ additional required dep for android support
compile 'com.github.beyondeye.reduks:reduks-rx:$reduks_version'
compile 'com.github.beyondeye.reduks:reduks-android:$reduks_version'

//kovenant based state store and Async Action Middleware
compile 'com.github.beyondeye.reduks:reduks-kovenant:$reduks_version'
compile 'com.github.beyondeye.reduks:reduks-android:$reduks_version'


//dev tools
compile 'com.github.beyondeye.reduks:reduks-devtools:$reduks_version'

//immutable collections
compile 'com.github.beyondeye.reduks:reduks-pcollections:$reduks_version'

//reduks bus
compile 'com.github.beyondeye.reduks:reduks-pcollections:$reduks_version'
compile 'com.github.beyondeye.reduks:reduks-bus:$reduks_version'

```
