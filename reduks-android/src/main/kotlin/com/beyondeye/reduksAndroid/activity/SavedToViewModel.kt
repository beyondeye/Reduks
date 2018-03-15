package com.beyondeye.reduksAndroid.activity



/**
 * make your reduksstate inherit from this class, if you want you reduks state to be retained on
 * configuration changes.
 * Note that we assume that there is a single Reduks State Class for each activity: we use the helper class
 * type of [ReduksStateViewModel] as a key for retrieving the state from ViewModelProvider:
 * see also [ActionRestoreState.restoreReduksState]
 * see https://developer.android.com/topic/libraries/architecture/viewmodel.html
 * basically a reference to the reduks state is saved in a retained fragment
 * Created by daely on 3/15/2018.
 */
interface SavedToViewModel

