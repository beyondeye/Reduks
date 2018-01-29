package com.beyondeye.reduks

/**
 * A rewrite for kotlin of https://github.com/reactjs/reselect library for redux (https://github.com/reactjs/redux)
 * see also "Computing Derived Data" in redux documentation http://redux.js.org/docs/recipes/ComputingDerivedData.html
 * Created by Dario Elyasy  on 3/18/2016.
 */
/**
 * equality check by reference
 */
private val byRefEqualityCheck = { a: Any, b: Any -> a === b }

/**
 * equality check by value: for primitive type
 */
private val byValEqualityCheck = { a: Any, b: Any -> a == b }

/**
 * a class for keeping a non null object reference even when actual reference is null
 * Needed because selectors do not work for nullable fields: if you have a field in the state that is
 * nullable T? the define instead as Opt<T> if you want selectors to work
 */
class Opt<T>(@JvmField val it:T?)

interface Memoizer<T> {
    fun memoize(vararg inputArgs: Any): T
}

inline fun <T : Any> Array<out T>.every(transform: (Int, T) -> Boolean): Boolean {
    forEachIndexed { i, t -> if (!transform(i, t)) return false }
    return true
}



// {a:Any,b:Any -> a===b}
fun <T> defaultMemoizer(func: (Array<out Any>) -> T, equalityCheck: (a: Any, b: Any) -> Boolean = byRefEqualityCheck) = object : Memoizer<T> {
    var lastArgs: Array<out Any>? = null
    var lastResult: T? = null
    override fun memoize(vararg inputArgs: Any): T {
        if (lastArgs != null &&
                lastArgs!!.size == inputArgs.size && inputArgs.every { index, value -> equalityCheck(value, lastArgs!![index]) }) {
            return lastResult!!
        }
        lastArgs = inputArgs
        lastResult = func(inputArgs)
        return lastResult!!
    }
}

/**
 * specialization for the case of single input (a little bit faster)
 */
fun <T> singleInputMemoizer(func: (Array<out Any>) -> T, equalityCheck: (a: Any, b: Any) -> Boolean = byRefEqualityCheck)=object:Memoizer<T> {
    var lastArg:Any?=null
    var lastResult:T?=null
    override fun memoize(vararg inputArgs: Any): T {
        val arg=inputArgs[0]
        if (lastArg != null &&
                equalityCheck(arg,lastArg!!)){
            return lastResult!!
        }
        lastArg = arg
        lastResult = func(inputArgs)
        return lastResult!!
    }
}


interface SelectorInput<S, I> {
    operator fun invoke(state: S): I
}

/**
 * a selector function is a function that map a field in state object to the input for the selector compute function
 */
class InputField<S, I>(val fn: S.() -> I) : SelectorInput<S, I> {
    override operator fun invoke(state: S): I = state.fn()
}


/**
 * note: [Selector] inherit from [SelectorInput] because of support for composite selectors
 */
interface Selector<S, O> : SelectorInput<S, O> {
    val recomputations: Long
    fun isChanged(): Boolean
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


    override fun isChanged(): Boolean = _recomputations != recomputationsLastChanged
    override fun resetChanged() {
        recomputationsLastChanged = _recomputations
    }


    protected abstract val computeAndCount: (i: Array<out Any>) -> O
    /**
     * 'lazy' because computeandcount is abstract. Cannot reference to it before it is initialized in concrete selectors
     * 'open' because we can provide a custom memoizer if needed
     */
    open val memoizer by lazy { defaultMemoizer(computeAndCount) }  //

}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP5<S, I0 : Any, I1 : Any, I2 : Any, I3 : Any, I4 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                                                         @JvmField val si1: SelectorInput<S, I1>,
                                                                         @JvmField val si2: SelectorInput<S, I2>,
                                                                         @JvmField val si3: SelectorInput<S, I3>,
                                                                         @JvmField val si4: SelectorInput<S, I4>
) {
    fun<O> compute(computeFun: (I0, I1, I2, I3, I4) -> O) = object : AbstractSelector<S, O>() {
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1, i[2] as I2, i[3] as I3, i[4] as I4)
        }

        override operator fun invoke(state: S): O {
            return memoizer.memoize(
                    si0(state),
                    si1(state),
                    si2(state),
                    si3(state),
                    si4(state)
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP4<S, I0 : Any, I1 : Any, I2 : Any, I3 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                                               @JvmField val si1: SelectorInput<S, I1>,
                                                               @JvmField val si2: SelectorInput<S, I2>,
                                                               @JvmField val si3: SelectorInput<S, I3>
) {
    fun<I4 : Any> withField(fn: S.() -> I4) = SelectorForP5<S, I0, I1, I2, I3, I4>(si0, si1, si2, si3, InputField(fn))
    fun<I4 : Any> withSelector(si: SelectorInput<S, I4>) = SelectorForP5<S, I0, I1, I2, I3, I4>(si0, si1, si2, si3, si)
    fun<O> compute(computeFun: (I0, I1, I2, I3) -> O) = object : AbstractSelector<S, O>() {
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1, i[2] as I2, i[3] as I3)
        }

        override operator fun invoke(state: S): O {
            return memoizer.memoize(
                    si0(state),
                    si1(state),
                    si2(state),
                    si3(state)
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP3<S, I0 : Any, I1 : Any, I2 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                                     @JvmField val si1: SelectorInput<S, I1>,
                                                     @JvmField val si2: SelectorInput<S, I2>
) {
    fun<I3 : Any> withField(fn: S.() -> I3) = SelectorForP4<S, I0, I1, I2, I3>(si0, si1, si2, InputField(fn))
    fun<I3 : Any> withSelector(si: SelectorInput<S, I3>) = SelectorForP4<S, I0, I1, I2, I3>(si0, si1, si2, si)
    fun<O> compute(computeFun: (I0, I1, I2) -> O) = object : AbstractSelector<S, O>() {
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1, i[2] as I2)
        }

        override operator fun invoke(state: S): O {
            return memoizer.memoize(
                    si0(state),
                    si1(state),
                    si2(state)
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP2<S, I0 : Any, I1 : Any>(@JvmField val si0: SelectorInput<S, I0>,
                                           @JvmField val si1: SelectorInput<S, I1>) {
    fun<I2 : Any> withField(fn: S.() -> I2) = SelectorForP3<S, I0, I1, I2>(si0, si1, InputField(fn))
    fun<I2 : Any> withSelector(si: SelectorInput<S, I2>) = SelectorForP3<S, I0, I1, I2>(si0, si1, si)
    fun<O> compute(computeFun: (I0, I1) -> O) = object : AbstractSelector<S, O>() {
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0, i[1] as I1)
        }

        override operator fun invoke(state: S): O {
            return memoizer.memoize(
                    si0(state),
                    si1(state)
            )
        }
    }
}

//use @JvmField annotation for avoiding generation useless getter methods
class SelectorForP1<S, I0 : Any>(@JvmField val si0: SelectorInput<S, I0>) {
    fun<I1 : Any> withField(fn: S.() -> I1) = SelectorForP2<S, I0, I1>(si0, InputField(fn))
    fun<I1 : Any> withSelector(si: SelectorInput<S, I1>) = SelectorForP2<S, I0, I1>(si0, si)
    fun<O> compute(computeFun: (I0) -> O) = object : AbstractSelector<S, O>() {
        override val computeAndCount = fun(i: Array<out Any>): O {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return computeFun(i[0] as I0)
        }

        override operator fun invoke(state: S): O {
            return memoizer.memoize(
                    si0(state)
            )
        }
    }
}

/**
 * wrapper class for Selector factory methods , that basically is used only to capture
 * type information for the state parameter
 */
class SelectorBuilder<S> {
    fun<I0 : Any> withField(fn: S.() -> I0) = SelectorForP1<S, I0>(InputField(fn))
    fun<I0 : Any> withSelector(si: SelectorInput<S, I0>) = SelectorForP1<S, I0>(si)

    /**
     * special single input selector that should be used when you just want to retrieve a single field:
     * Warning: Don't use this with primitive type fields, use [withSingleFieldByValue] instead!!!
     */
    fun <I : Any> withSingleField(fn: S.() -> I) = object : AbstractSelector<S, I>() {
        override val computeAndCount = fun(i: Array<out Any>): I {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return i[0] as I
        }

        override operator fun invoke(state: S): I {
            return memoizer.memoize(
                    fn(state)
            )
        }
        override val memoizer: Memoizer<I> by lazy {
            singleInputMemoizer(computeAndCount, byRefEqualityCheck)
        }
    }
    /**
     * special single input selector that should be used when you just want to retrieve a single field that
     * is a primitive type like Int, Float, Double, etc..., because it compares memoized values, instead of references
     */
    fun <I : Any> withSingleFieldByValue(fn: S.() -> I) = object : AbstractSelector<S, I>() {
        override val computeAndCount = fun(i: Array<out Any>): I {
            ++_recomputations
            @Suppress("UNCHECKED_CAST")
            return i[0] as I
        }

        override operator fun invoke(state: S): I {
            return memoizer.memoize(
                    fn(state)
            )
        }

        override val memoizer: Memoizer<I> by lazy {
            singleInputMemoizer(computeAndCount, byValEqualityCheck)
        }

        operator fun <I : Any> invoke(fn: S.() -> I): AbstractSelector<S, I> {
            return withSingleField(fn)
        }
    }
}
