package com.beyondeye.reduks

private typealias  EqualityCheckFn = (a:Any,b:Any)->Boolean
/**
 * A rewrite for kotlin of https://github.com/reactjs/reselect library for redux (https://github.com/reactjs/redux)
 * see also "Computing Derived Data" in redux documentation http://redux.js.org/docs/recipes/ComputingDerivedData.html
 * Created by Dario Elyasy  on 3/18/2016.
 */
/**
 * equality check by reference
 */
val byRefEqualityCheck:EqualityCheckFn = { a: Any, b: Any -> a === b }

/**
 * equality check by value: for primitive type
 */
val byValEqualityCheck:EqualityCheckFn = { a: Any, b: Any -> a == b }

/**
 * a class for keeping a non null object reference even when actual reference is null
 * Needed because selectors do not work for nullable fields: if you have a field in the state that is
 * nullable T? the define instead as Opt<T> if you want selectors to work
 */
class Opt<T>(@JvmField val it:T?)

interface Memoizer<T> {
    fun memoize(state: Any, vararg inputs: SelectorInput<Any, Any>): T
}

// {a:Any,b:Any -> a===b}
fun <T> computationMemoizer(computeFn: (Array<out Any>) -> T) = object : Memoizer<T> {
    var lastArgs: Array<out Any>? = null
    var lastResult: T? = null
    override fun memoize(state:Any,vararg inputs: SelectorInput<Any,Any>): T {
        val nInputs=inputs.size
        val args=Array<Any>(nInputs) { inputs[it].invoke(state) }
        if(lastArgs!=null && lastArgs!!.size==inputs.size) {
            var bMatchedArgs=true
            for(i in 0 until nInputs) {
                if(!inputs[i].equalityCheck(args[i],lastArgs!![i])) {
                    bMatchedArgs=false
                    break
                }
            }
            if(bMatchedArgs) {
                return lastResult!!
            }
        }
        lastArgs = args
        lastResult = computeFn(args)
        return lastResult!!
    }
}



/**
 * specialization for the case of single input (a little bit faster)
 */
fun <T> singleInputMemoizer(func: (Array<out Any>) -> T)=object:Memoizer<T> {
    var lastArg:Any?=null
    var lastResult:T?=null
    override fun memoize(state: Any, vararg inputs: SelectorInput<Any, Any>): T {
        val input=inputs[0]
        val arg=input.invoke(state)
        if (lastArg != null &&
                input.equalityCheck(arg,lastArg!!)){
            return lastResult!!
        }
        lastArg = arg
        lastResult = func(arrayOf(arg))
        return lastResult!!
    }
}


interface SelectorInput<S, I> {
    operator fun invoke(state: S): I
    val equalityCheck:EqualityCheckFn
}

/**
 * a selector function is a function that map a field in state object to the input for the selector compute function
 */
class InputField<S, I>(val fn: S.() -> I,override val equalityCheck: EqualityCheckFn) : SelectorInput<S, I> {
    override operator fun invoke(state: S): I = state.fn()
}


/**
 * note: [Selector] inherit from [SelectorInput] because of support for composite selectors
 */
interface Selector<S, O> : SelectorInput<S, O> {
    val recomputations: Long
    fun isChanged(): Boolean
    /**
     * by calling this method, you will force the next call to [getIfChangedIn] to succeed,
     * as if the actual value of the selector was changed, but no actual recomputation is performed
     */
    fun signalChanged()
    fun resetChanged()
    fun getIfChangedIn(state: S): O? {
        val res = invoke(state)
        if (isChanged()) {
            resetChanged()
            return res
        }
        return null
    }

    fun onChangeIn(state: S, blockfn: (O) -> Unit) {
        getIfChangedIn(state)?.let(blockfn)
    }

    /**
     * same as regular [onChangeIn] but don't activate selector unless [condition] is true
     * Note that the selector is not run at all if [condition] is false. Therefore
     * the selector will be triggered by changes that happened and where ignored, as soon as
     * [condition] become true
     */
    fun onChangeIn(state: S, condition:Boolean,blockfn: (O) -> Unit) {
        if(condition) getIfChangedIn(state)?.let(blockfn)
    }
}

/**
 * same as [Selector.onChangeIn], but as extension function of state:
 * it checks if the specified selector value  is changed for the input state and if so, call [blockfn]
 * with the updated selector value
 */
fun <S,O> S.whenChangeOf(selector:Selector<S,O>,blockfn: (O) -> Unit) {
    selector.getIfChangedIn(this)?.let(blockfn)
}

/**
 * abstract base class for all selectors
 */
abstract class AbstractSelector<S, O> : Selector<S, O> {
    @JvmField protected var recomputationsLastChanged = 0L
    @JvmField protected var _recomputations = 0L
    override val recomputations: Long get() = _recomputations

    /**
     * see documentation to [Selector.signalChanged]
     */
    override fun signalChanged() {
        ++_recomputations
    }

    override fun isChanged(): Boolean = _recomputations != recomputationsLastChanged
    override fun resetChanged() {
        recomputationsLastChanged = _recomputations
    }


    protected abstract val computeAndCount: (i: Array<out Any>) -> O
    /**
     * 'lazy' because computeandcount is abstract. Cannot reference to it before it is initialized in concrete selectors
     * 'open' because we can provide a custom memoizer if needed
     */
    open val memoizer by lazy { computationMemoizer(computeAndCount) }

}

/**
 * this class is a base class for result of compute function. it derives from [AbstractSelector]
 * because a [ComputeResult] can be used as input when building a selector, using [SelectorBuilder.withSelector]
 */
abstract class ComputeResult<S:Any,O>:AbstractSelector<S,O>() {
    fun computeResultMemoizedByRef()=computeResultMemoized(byRefEqualityCheck)
    fun computeResultMemoizedByVal()=computeResultMemoized(byValEqualityCheck)
    /**
     * by default, a selector is considered changed if any of its inputs is changed, even though
     * the output calculated by compute is the same. If instead you want to trigger the selector
     * if some of the inputs is changed AND ALSO the output calculated is changed then use
     * this method, or [computeResultMemoizedByRef] or [computeResultMemoizedByVal] if you want to override the way
     * you check if the result of the compute is changed
     */
    fun computeResultMemoized(equalityCheckFn: EqualityCheckFn=this@ComputeResult.equalityCheck) = object : AbstractSelector<S, O>() {
        @Suppress("UNCHECKED_CAST")
        private val computeSelector=this@ComputeResult as SelectorInput<Any, Any>
        override val computeAndCount = fun(i: Array<out Any>):O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return i[0] as O
        }

        override operator fun invoke(state: S): O {
            return memoizer.memoize(state,computeSelector)
        }
        override val equalityCheck: EqualityCheckFn = equalityCheckFn
        override val memoizer: Memoizer<O> by lazy {
            object : Memoizer<O> { //a special memoizer for the computed result
                var lastCompute: O? = null
                override fun memoize(state: Any, vararg inputs: SelectorInput<Any, Any>): O {
                    val computeSelector = inputs[0]
                    val compute = computeSelector.invoke(state)
                    if (lastCompute != null &&
                            equalityCheck(compute, lastCompute!!)) {
                        return lastCompute!!
                    }
                    lastCompute = compute as O
                    ++_recomputations
                    return lastCompute!!
                }
            }
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP5<S:Any, I0 : Any, I1 : Any, I2 : Any, I3 : Any, I4 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                                                         @JvmField val si1: SelectorInput<S, I1>,
                                                                         @JvmField val si2: SelectorInput<S, I2>,
                                                                         @JvmField val si3: SelectorInput<S, I3>,
                                                                         @JvmField val si4: SelectorInput<S, I4>
) {
//    fun<O> computeByValue(computeFun: (I0, I1, I2, I3, I4) -> O)=compute(byValEqualityCheck,computeFun)
    fun<O> compute(equalityCheckForResult: EqualityCheckFn= byRefEqualityCheck,computeFun: (I0, I1, I2, I3, I4) -> O) = object : ComputeResult<S, O>() {
        override val equalityCheck: EqualityCheckFn
            get() = equalityCheckForResult
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1, i[2] as I2, i[3] as I3, i[4] as I4)
        }

        override operator fun invoke(state: S): O {
            @Suppress("UNCHECKED_CAST")
            return memoizer.memoize(
                   state,
                    si0 as SelectorInput<Any, Any>,
                    si1 as SelectorInput<Any, Any>,
                    si2 as SelectorInput<Any, Any>,
                    si3 as SelectorInput<Any, Any>,
                    si4 as SelectorInput<Any, Any>
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP4<S:Any, I0 : Any, I1 : Any, I2 : Any, I3 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                                               @JvmField val si1: SelectorInput<S, I1>,
                                                               @JvmField val si2: SelectorInput<S, I2>,
                                                               @JvmField val si3: SelectorInput<S, I3>
) {
    fun<I4 : Any> withField(fn: S.() -> I4) = SelectorForP5<S, I0, I1, I2, I3, I4>(si0, si1, si2, si3, InputField(fn, byRefEqualityCheck))
    fun<I4 : Any> withFieldByValue(fn: S.() -> I4) = SelectorForP5<S, I0, I1, I2, I3, I4>(si0, si1, si2, si3, InputField(fn, byValEqualityCheck))
    fun<I4 : Any> withSelector(si: SelectorInput<S, I4>) = SelectorForP5<S, I0, I1, I2, I3, I4>(si0, si1, si2, si3, si)
//    fun<O> computeByValue(computeFun: (I0, I1, I2, I3) -> O)=compute(byValEqualityCheck,computeFun)
    fun<O> compute(equalityCheckForResult: EqualityCheckFn= byRefEqualityCheck,computeFun: (I0, I1, I2, I3) -> O) = object : ComputeResult<S,O>() {
        override val equalityCheck: EqualityCheckFn
            get() = equalityCheckForResult
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1, i[2] as I2, i[3] as I3)
        }

        override operator fun invoke(state: S): O {
            @Suppress("UNCHECKED_CAST")
            return memoizer.memoize(
                    state,
                    si0 as SelectorInput<Any, Any>,
                    si1 as SelectorInput<Any, Any>,
                    si2 as SelectorInput<Any, Any>,
                    si3 as SelectorInput<Any, Any>
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP3<S:Any, I0 : Any, I1 : Any, I2 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                                     @JvmField val si1: SelectorInput<S, I1>,
                                                     @JvmField val si2: SelectorInput<S, I2>
) {
    fun<I3 : Any> withField(fn: S.() -> I3) = SelectorForP4<S, I0, I1, I2, I3>(si0, si1, si2, InputField(fn, byRefEqualityCheck))
    fun<I3 : Any> withFieldByValue(fn: S.() -> I3) = SelectorForP4<S, I0, I1, I2, I3>(si0, si1, si2, InputField(fn, byValEqualityCheck))
    fun<I3 : Any> withSelector(si: SelectorInput<S, I3>) = SelectorForP4<S, I0, I1, I2, I3>(si0, si1, si2, si)
//    fun<O> computeByValue(computeFun: (I0, I1, I2) -> O)=compute(byValEqualityCheck,computeFun)
    fun<O> compute(equalityCheckForResult: EqualityCheckFn= byRefEqualityCheck,computeFun: (I0, I1, I2) -> O) = object : ComputeResult<S, O>() {
        override val equalityCheck: EqualityCheckFn
            get() = equalityCheckForResult
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1, i[2] as I2)
        }

        override operator fun invoke(state: S): O {
            @Suppress("UNCHECKED_CAST")
            return memoizer.memoize(
                    state,
                    si0 as SelectorInput<Any, Any>,
                    si1 as SelectorInput<Any, Any>,
                    si2 as SelectorInput<Any, Any>
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP2<S:Any, I0 : Any, I1 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                           @JvmField val si1: SelectorInput<S, I1>) {
    fun<I2 : Any> withField(fn: S.() -> I2) = SelectorForP3<S, I0, I1, I2>(si0, si1, InputField(fn, byRefEqualityCheck))
    fun<I2 : Any> withFieldByValue(fn: S.() -> I2) = SelectorForP3<S, I0, I1, I2>(si0, si1, InputField(fn, byValEqualityCheck))
    fun<I2 : Any> withSelector(si: SelectorInput<S, I2>) = SelectorForP3<S, I0, I1, I2>(si0, si1, si)
//    fun<O> computeByValue(computeFun: (I0, I1) -> O)=compute(byValEqualityCheck,computeFun)
    fun<O> compute(equalityCheckForResult: EqualityCheckFn= byRefEqualityCheck,computeFun: (I0, I1) -> O) = object : ComputeResult<S, O>() {
        override val equalityCheck: EqualityCheckFn
            get() = equalityCheckForResult
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1)
        }

        override operator fun invoke(state: S): O {
            @Suppress("UNCHECKED_CAST")
            return memoizer.memoize(
                    state,
                    si0 as SelectorInput<Any, Any>,
                    si1 as SelectorInput<Any, Any>
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP1<S:Any, I0 : Any>(@JvmField val si0: SelectorInput<S, I0>) {
    fun<I1 : Any> withField(fn: S.() -> I1) = SelectorForP2<S, I0, I1>(si0, InputField(fn, byRefEqualityCheck))
    fun<I1 : Any> withFieldByValue(fn: S.() -> I1) = SelectorForP2<S, I0, I1>(si0, InputField(fn, byValEqualityCheck))
    fun<I1 : Any> withSelector(si: SelectorInput<S, I1>) = SelectorForP2<S, I0, I1>(si0, si)
//    fun<O> computeByValue(computeFun: (I0) -> O)=compute(byValEqualityCheck,computeFun)
    fun<O> compute(equalityCheckForResult: EqualityCheckFn= byRefEqualityCheck,computeFun: (I0) -> O) = object : ComputeResult<S, O>() {
        override val equalityCheck: EqualityCheckFn
            get() = equalityCheckForResult
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0)
        }

        override operator fun invoke(state: S): O {
            @Suppress("UNCHECKED_CAST")
            return memoizer.memoize(
                    state,
                    si0 as SelectorInput<Any, Any>
            )
        }
    }
}

/**
 * wrapper class for Selector factory methods , that basically is used only to capture
 * type information for the state parameter
 */
class SelectorBuilder<S:Any> {
    fun<I0 : Any> withField(fn: S.() -> I0) = SelectorForP1<S, I0>(InputField(fn, byRefEqualityCheck))
    fun<I0 : Any> withFieldByValue(fn: S.() -> I0) = SelectorForP1<S, I0>(InputField(fn, byValEqualityCheck))
    fun<I0 : Any> withSelector(si: SelectorInput<S, I0>) = SelectorForP1<S, I0>(si)

    /**
     * special single input selector that should be used when you just want to retrieve a single field:
     * Warning: Don't use this with primitive type fields, use [withSingleFieldByValue] instead!!!
     */
    fun <I : Any> withSingleField(fn: S.() -> I) = object : AbstractSelector<S, I>() {
        @Suppress("UNCHECKED_CAST")
        private val inputField=InputField(fn, byRefEqualityCheck) as SelectorInput<Any, Any>
        override val computeAndCount = fun(i: Array<out Any>): I {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return i[0] as I
        }

        override operator fun invoke(state: S): I {
            return memoizer.memoize(state,inputField)
        }
        override val equalityCheck: EqualityCheckFn
            get() = byRefEqualityCheck
        override val memoizer: Memoizer<I> by lazy {
            singleInputMemoizer(computeAndCount)
        }
    }
    /**
     * special single input selector that should be used when you just want to retrieve a single field that
     * is a primitive type like Int, Float, Double, etc..., because it compares memoized values, instead of references
     */
    fun <I : Any> withSingleFieldByValue(fn: S.() -> I) = object : AbstractSelector<S, I>() {
        @Suppress("UNCHECKED_CAST")
        private val inputField=InputField(fn, byValEqualityCheck) as SelectorInput<Any, Any>
        override val computeAndCount = fun(i: Array<out Any>): I {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return i[0] as I
        }
        override operator fun invoke(state: S): I {
            return memoizer.memoize(state,inputField)
        }
        override val equalityCheck: EqualityCheckFn
            get() = byValEqualityCheck
        override val memoizer: Memoizer<I> by lazy {
            singleInputMemoizer(computeAndCount)
        }

        operator fun <I : Any> invoke(fn: S.() -> I): AbstractSelector<S, I> {
            return withSingleField(fn)
        }
    }
}
