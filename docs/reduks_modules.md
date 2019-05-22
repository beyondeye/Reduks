## Handling complex reduks states with modules
They are part of support for reduks modules, it is possible with reduks to define a state as a combination of multiple substates, Each substate is associated with a "tag" for identifying it between other substates. this tag is the ReduksContext.
Each  substate has its associated reducer function. So when an action is dispatched to the parent reduks store, the parent reducer use the reduks context for deciding to which substate reducer function to dispatch the action.
Why this is useful? Lets say that you created some UI module, driven by a reduks state, lets say associated to a fragment. and want to reuse this UI module in another activity, or perhaps have multiple instances of the same UI module displayed. Reduks modules make it easy to do it.
ReduksContextTyped is simular to ReduksContext, but contains also information on the type of the Action, so that the master reducer, when deciding to which substate reducer to send the action can potentially take into consideration the action type not only the module "tag".
RedukcContextTyped is not currently in use, so it is possible that for the sake of simplicity I will remove it in the future
For an example of usage of ReduksContext you can look at tests, in here
https://github.com/beyondeye/Reduks/tree/3.x_kotlin_1_3/reduks-core-modules/src/test/kotlin/com/beyondeye/reduks/modules
Reduks modules are also useful for the sake of performance in updating the reduks state and notifying subscribers of changes in the state.
For very complex reduks state, updating substate, means creating a new substate and updating the reference to it in the main state. This is cheaper than creating a copy of the full main state. And also you can have subscribers to changes of only a single substate.