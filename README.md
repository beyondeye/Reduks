[![Kotlin 1.3.31](https://img.shields.io/badge/Kotlin-1.3.31-blue.svg)](http://kotlinlang.org)
[![](https://jitpack.io/v/beyondeye/reduks.svg)](https://jitpack.io/#beyondeye/reduks)
[![Slack channel](https://img.shields.io/badge/Chat-Slack-green.svg)](https://kotlinlang.slack.com/messages/reduks/)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Reduks-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/4245)

# What is Reduks?
Reduks is a port of [redux.js](https://redux.js.org/) and several of the most popular libraries from its ecosystem 
to Kotlin. The purpose is to have a set of integrated libraries to make it a viable option to develop real world apps for Android.

# Only Android?
The focus of development until recently has been Android, but all Android-related features are kept in separated modules, with the core modules usable in any JVM context.
Actually Reduks core modules are almost pure Kotlin and in fact there is already a  [fork of Reduks](https://github.com/patjackson52/Reduks) that can be used with multiplatform projects.

In the future, as the multiplatform support in Kotlin matures, Reduks will be fully restructured as a multiplatform project

# Some notable features
- All basic standard ReduxJS components and interfaces:  Store, Store Creator, Store Subscriber, Middlewares, Store Enhancers
- common basic extensions, like Thunk and Promise middlewares, Standard Action
- port of [reselect library](https://github.com/reactjs/reselect>)
- coroutine based port of [reduks-saga.js](https://redux-saga.js.org/) 
- A powerful logger middleware based on a port of <https://github.com/evgenyrodionov/redux-logger>, with the text
  formatting engine based on a heavily customized version of <https://github.com/orhanobut/logger>. 
  The reduks logger middleware allows to print Json diff of subsequent reduks states  thanks to a port to Kotlin of <https://github.com/flipkart-incubator/zjsonpatch>, with Jackson substituted with Gson and guava dependency removed
- integration with a  [persistent collections library ](https://github.com/hrldcpr/pcollections) for better perfomance with complex states 
- Integration with an Event Bus implemented as Store enhancer
- Support for easily decomposing complex states in multiple reduks modules
## Android specific features
- integration with Android Activity and Fragment and their lifecycles, with automatic save and restore of reduks state 

# Browse the Docs!
For the full documentation see [here](https://beyondeye.gitbooks.io/reduks/content), or take a look at the [summary](SUMMARY.MD).

## License
[Apache License 2.0](LICENSE)
