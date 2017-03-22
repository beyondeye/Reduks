package com.beyondeye.reduksAndroid.fragment

import com.beyondeye.reduks.pcollections.HashTreePMap
import com.beyondeye.reduks.pcollections.PMap

class FragmentStatus(val status:Int){
    fun isActive()=(status and CODE_FRAGMENTSTATUS_ACTIVE)!=0
    fun isInactive()=(status and CODE_FRAGMENTSTATUS_INACTIVE)!=0
    fun isNotCreated()=(status and CODE_FRAGMENTSTATUS_NOT_CREATED)!=0
    override fun toString(): String = when {
        isNotCreated() -> "not_created"
        isActive() -> "active"
        isInactive() -> "inactive"
        else -> "???"
    }

    companion object {
        val CODE_FRAGMENTSTATUS_NOT_CREATED =1 shl 0
        val CODE_FRAGMENTSTATUS_ACTIVE =1 shl 1
        val CODE_FRAGMENTSTATUS_INACTIVE =1 shl 2
        val fragmentStatusNotCreated=FragmentStatus(CODE_FRAGMENTSTATUS_NOT_CREATED)
        val fragmentStatusActive=FragmentStatus(CODE_FRAGMENTSTATUS_ACTIVE)
        val fragmentStatusInactive =FragmentStatus(CODE_FRAGMENTSTATUS_INACTIVE)
    }
}
class FragmentStatusData(val fsdata: PMap<String,FragmentStatus>){
    companion object {
        val empty:FragmentStatusData=FragmentStatusData(HashTreePMap.empty())
    }

    fun  plus(fragmentTag: String, newFragmentStatus: FragmentStatus) = FragmentStatusData(fsdata.plus(fragmentTag,newFragmentStatus))
    operator fun  get(fragmentTag: String): FragmentStatus? {return fsdata.get(fragmentTag)}

}
/**
 * base interface for reduks State class that can handle fragment status
 */
interface StateWithFragmentStatusData {
    /**
     * a persistent (immutable) map that contains a key for each fragment tag: simply override it
     * in Reduks state class implementation with
     * override val fragmentStatus:PMap<String,Int> = FragmentStatusData.empty
     */
    val fragmentStatus: FragmentStatusData
    /**
     * a method that returns a copy of the state, with the fragmentStatus map substituted with the one
     * in [newFragmentStatus].If your State class is defined as a data class, simply implement this as
     *     override fun copyWithNewFragmentStatus(newFragmentStatus: BusData) = copy(fragmentStatus=newFragmentStatus)
     */
    fun copyWithNewFragmentStatus(newFragmentStatus: FragmentStatusData): StateWithFragmentStatusData
}


