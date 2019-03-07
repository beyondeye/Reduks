package com.beyondeye.reduksAndroid.fragment

import androidx.fragment.app.Fragment
import com.beyondeye.reduks.pcollections.HashTreePMap
import com.beyondeye.reduks.pcollections.PMap

class FragmentActiveStatus(val mode:Int){
    fun isActive()=(mode and CODE_FRAGMENT_ACTIVESTATUS_ACTIVE)!=0
    fun isInactive()=(mode and CODE_FRAGMENT_ACTIVESTATUS_INACTIVE)!=0
    fun isNotCreated()=(mode and CODE_FRAGMENT_ACTIVESTATUS_NOT_CREATED)!=0
    override fun toString(): String = when {
        isNotCreated() -> "not_created"
        isActive() -> "active"
        isInactive() -> "inactive"
        else -> "???"
    }

    companion object {
        val CODE_FRAGMENT_ACTIVESTATUS_NOT_CREATED =1 shl 0
        val CODE_FRAGMENT_ACTIVESTATUS_ACTIVE =1 shl 1
        val CODE_FRAGMENT_ACTIVESTATUS_INACTIVE =1 shl 2
        val fragmentStatusNotCreated=FragmentActiveStatus(CODE_FRAGMENT_ACTIVESTATUS_NOT_CREATED)
        val fragmentStatusActive=FragmentActiveStatus(CODE_FRAGMENT_ACTIVESTATUS_ACTIVE)
        val fragmentStatusInactive =FragmentActiveStatus(CODE_FRAGMENT_ACTIVESTATUS_INACTIVE)
    }
}
class FragmentStatusData(val fsdata: PMap<String,FragmentActiveStatus>, val fpdata:PMap<String,String>){
    companion object {
        val empty:FragmentStatusData=FragmentStatusData(HashTreePMap.empty(),HashTreePMap.empty())
    }

    fun  withUpdatedFragmentActiveStatus(fragmentTag: String, newFragmentActiveStatus: FragmentActiveStatus) = FragmentStatusData(fsdata.plus(fragmentTag,newFragmentActiveStatus),fpdata)
    fun  withUpdatedFragmentCurAtPos(fragmentTag: String, positionTag:String) = FragmentStatusData(fsdata,fpdata.plus(positionTag,fragmentTag))

    fun getFragmentActiveStatus(fragmentTag: String): FragmentActiveStatus? {
        return fsdata.get(fragmentTag)
    }

    /**
     * return fragment tag  (see [Fragment.getTag]) for fragment currently at position [positionTag]
     * or empty string if no active fragment at specified position
     */
    fun getFragmentCurAtPos(positionTag:String):String {
        return fpdata[positionTag] ?:""
    }
}
/**
 * base interface for reduks State class that can handle fragment mode
 */
interface StateWithFragmentStatusData {
    /**
     * a persistent (immutable) map that contains a key for each fragment tag: simply override it
     * in Reduks state class implementation with
     * override val fragmentActiveStatus:PMap<String,Int> = FragmentStatusData.empty
     */
    val fragmentStatus: FragmentStatusData
    /**
     * a method that returns a copy of the state, with the fragmentActiveStatus map substituted with the one
     * in [newFragmentStatus].If your State class is defined as a data class, simply implement this as
     *     override fun copyWithNewFragmentStatus(newFragmentStatus: BusData) = copy(fragmentActiveStatus=newFragmentStatus)
     */
    fun copyWithNewFragmentStatus(newFragmentStatus: FragmentStatusData): StateWithFragmentStatusData
}


