package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.applyMiddleware

/**
 * generic redux Redux
 * TODO substistute KovenantReduks, SimpleReduks, and RxReduks using only ReduksModule and make them deprecated
 * Created by daely on 6/8/2016.
 */
class ReduksModule<State>(moduleDef: ReduksModule.Def<State>) : Reduks<State> {
    /**
     * all data needed for creating a ReduksModule
     */
    class Def<State>(
            /**
             * an id that identify the module
             */
            val ctx: ReduksContext,
            /**
             * factory method for store
             */
            val  storeCreator:StoreCreator<State>,
            /**
             * return the initial state
             */
            val initialState: State,
            /**
             * return the initial action to dispatch to the Store
             */
            val startAction: Any,
            /**
             * return the state reducer
             */
            val stateReducer: Reducer<State>,
            /**
             * return the main store subscriber
             * we pass as argument the store itself, so that we can create an object that implement the
             * [StoreSubscriberFn] interface that keep a reference to the store itself, in case the we need call dispatch
             * in the subscriber
             */
            val subscriberBuilder: StoreSubscriberBuilder<State>)
    override val ctx: ReduksContext
    override val store: Store<State>
    override val storeSubscriber: StoreSubscriber<State>
    override val storeSubscription: StoreSubscription
    init {
        ctx=moduleDef.ctx
        val storeCreator= moduleDef.storeCreator
        store=storeCreator.create(moduleDef.stateReducer, moduleDef.initialState)
        store.applyMiddleware(*storeCreator.storeStandardMiddlewares)
        storeSubscriber= moduleDef.subscriberBuilder.build(store)
        storeSubscription = store.subscribe(storeSubscriber)
        //split multiaction to list if required
        val actionList: List<Any> = MultiActionWithContext.toActionList(moduleDef.startAction)
        actionList.forEach { store.dispatch(it) }
    }


    fun  subscribe(storeSubscriber: StoreSubscriber<State>): StoreSubscription =store.subscribe(storeSubscriber)
    companion object {
        //calculate context for multiple modules
        fun multiContext(vararg ctxs: ReduksContext): ReduksContext {
            val res = ctxs.reduce { prevCtx, ctx -> prevCtx + ctx }
            if (ctxs.toSet().size < ctxs.size)
                throw IllegalArgumentException("Invalid MultiContext: when combining multiple modules, each module MUST have a distinct context!: $res")
            return res
        }

        fun <S1 : Any, S2 : Any> MultiDef(storeCreator_: StoreCreator<MultiState2<S1, S2>>,
                                          m1: ReduksModule.Def<S1>,
                                          m2: ReduksModule.Def<S2>): ReduksModule.Def<MultiState2<S1, S2>> {
            val mctx = multiContext(m1.ctx, m2.ctx)
            return ReduksModule.Def<MultiState2<S1, S2>>(
                    ctx = mctx,
                    storeCreator = MultiStore2.Factory<S1, S2>(storeCreator_.ofType(), m1.ctx, m2.ctx),
                    initialState = MultiState2(mctx, m1.initialState, m2.initialState),
                    startAction = MultiActionWithContext(
                            ActionWithContext(m1.startAction, m1.ctx),
                            ActionWithContext(m2.startAction, m2.ctx)),
                    stateReducer = MultiReducer2<S1, S2>(m1, m2),
                    subscriberBuilder = StoreSubscriberBuilderFn { store ->
                        if (store !is MultiStore2<S1, S2>) throw IllegalArgumentException("error")
                        val selector = SelectorBuilder<MultiState2<S1, S2>>()
                        val s1sel = selector.withSingleField { s1 }
                        val s2sel = selector.withSingleField { s2 }
                        val sub1 = m1.subscriberBuilder.build(store.store1)
                        val sub2 = m2.subscriberBuilder.build(store.store2)
                        StoreSubscriberFn {
                            val newS=store.state
                            s1sel.onChangeIn(newS) {
                                sub1.onStateChange()
                            }
                            s2sel.onChangeIn(newS) {
                                sub2.onStateChange()
                            }
                        }
                    })
        }
        //--------------------------------
        fun <S1 : Any, S2 : Any, S3 : Any> MultiDef(storeCreator_: StoreCreator<MultiState3<S1, S2, S3>>,
                                                          m1: ReduksModule.Def<S1>,
                                                          m2: ReduksModule.Def<S2>,
                                                          m3: ReduksModule.Def<S3>): ReduksModule.Def<MultiState3<S1, S2, S3>> {
            val mctx = multiContext(m1.ctx, m2.ctx, m3.ctx)
            return ReduksModule.Def<MultiState3<S1, S2, S3>>(
                    ctx = mctx,
                    storeCreator = MultiStore3.Factory<S1, S2, S3>(storeCreator_.ofType(), m1.ctx, m2.ctx, m3.ctx),
                    initialState = MultiState3(mctx, m1.initialState, m2.initialState, m3.initialState),
                    startAction = MultiActionWithContext(
                            ActionWithContext(m1.startAction, m1.ctx),
                            ActionWithContext(m2.startAction, m2.ctx),
                            ActionWithContext(m3.startAction, m3.ctx)),
                    stateReducer = MultiReducer3<S1, S2, S3>(m1, m2, m3),
                    subscriberBuilder = StoreSubscriberBuilderFn { store ->
                        if (store !is MultiStore3<S1, S2, S3>) throw IllegalArgumentException("error")
                        val selector = SelectorBuilder<MultiState3<S1, S2, S3>>()
                        val s1sel = selector.withSingleField { s1 }
                        val s2sel = selector.withSingleField { s2 }
                        val s3sel = selector.withSingleField { s3 }
                        val sub1 = m1.subscriberBuilder.build(store.store1)
                        val sub2 = m2.subscriberBuilder.build(store.store2)
                        val sub3 = m3.subscriberBuilder.build(store.store3)
                        StoreSubscriberFn {
                            val newS=store.state
                            s1sel.onChangeIn(newS) { sub1.onStateChange() }
                            s2sel.onChangeIn(newS) { sub2.onStateChange() }
                            s3sel.onChangeIn(newS) { sub3.onStateChange() }
                        }
                    })
        }
        //--------------------------------
        fun <S1 : Any, S2 : Any, S3 : Any, S4 : Any> MultiDef(storeCreator_: StoreCreator<MultiState4<S1, S2, S3, S4>>,
                                                                    m1: ReduksModule.Def<S1>,
                                                                    m2: ReduksModule.Def<S2>,
                                                                    m3: ReduksModule.Def<S3>,
                                                                    m4: ReduksModule.Def<S4>): ReduksModule.Def<MultiState4<S1, S2, S3, S4>> {
            val mctx = multiContext(m1.ctx, m2.ctx, m3.ctx, m4.ctx)
            return ReduksModule.Def<MultiState4<S1, S2, S3, S4>>(
                    ctx = mctx,
                    storeCreator = MultiStore4.Factory<S1, S2, S3, S4>(storeCreator_.ofType(), m1.ctx, m2.ctx, m3.ctx, m4.ctx),
                    initialState = MultiState4(mctx, m1.initialState, m2.initialState, m3.initialState, m4.initialState),
                    startAction = MultiActionWithContext(
                            ActionWithContext(m1.startAction, m1.ctx),
                            ActionWithContext(m2.startAction, m2.ctx),
                            ActionWithContext(m3.startAction, m3.ctx),
                            ActionWithContext(m4.startAction, m4.ctx)),
                    stateReducer = MultiReducer4<S1, S2, S3, S4>(m1, m2, m3, m4),
                    subscriberBuilder = StoreSubscriberBuilderFn { store ->
                        if (store !is MultiStore4<S1, S2, S3, S4>) throw IllegalArgumentException("error")
                        val selector = SelectorBuilder<MultiState4<S1, S2, S3, S4>>()
                        val s1sel = selector.withSingleField { s1 }
                        val s2sel = selector.withSingleField { s2 }
                        val s3sel = selector.withSingleField { s3 }
                        val s4sel = selector.withSingleField { s4 }
                        val sub1 = m1.subscriberBuilder.build(store.store1)
                        val sub2 = m2.subscriberBuilder.build(store.store2)
                        val sub3 = m3.subscriberBuilder.build(store.store3)
                        val sub4 = m4.subscriberBuilder.build(store.store4)
                        StoreSubscriberFn {
                            val newS=store.state
                            s1sel.onChangeIn(newS) { sub1.onStateChange() }
                            s2sel.onChangeIn(newS) { sub2.onStateChange() }
                            s3sel.onChangeIn(newS) { sub3.onStateChange() }
                            s4sel.onChangeIn(newS) { sub4.onStateChange() }
                        }
                    })
        }

        //--------------------------------
        fun <S1 : Any, S2 : Any, S3 : Any, S4 : Any, S5 : Any> MultiDef(storeCreator_: StoreCreator<MultiState5<S1, S2, S3, S4, S5>>,
                                                                              m1: ReduksModule.Def<S1>,
                                                                              m2: ReduksModule.Def<S2>,
                                                                              m3: ReduksModule.Def<S3>,
                                                                              m4: ReduksModule.Def<S4>,
                                                                              m5: ReduksModule.Def<S5>): ReduksModule.Def<MultiState5<S1, S2, S3, S4, S5>> {
            val mctx = multiContext(m1.ctx, m2.ctx, m3.ctx, m4.ctx, m5.ctx)
            return ReduksModule.Def<MultiState5<S1, S2, S3, S4, S5>>(
                    ctx = mctx,
                    storeCreator = MultiStore5.Factory<S1, S2, S3, S4, S5>(storeCreator_.ofType(), m1.ctx, m2.ctx, m3.ctx, m4.ctx, m5.ctx),
                    initialState = MultiState5(mctx, m1.initialState, m2.initialState, m3.initialState, m4.initialState, m5.initialState),
                    startAction = MultiActionWithContext(
                            ActionWithContext(m1.startAction, m1.ctx),
                            ActionWithContext(m2.startAction, m2.ctx),
                            ActionWithContext(m3.startAction, m3.ctx),
                            ActionWithContext(m4.startAction, m4.ctx),
                            ActionWithContext(m5.startAction, m5.ctx)),
                    stateReducer = MultiReducer5<S1, S2, S3, S4, S5>(m1, m2, m3, m4, m5),
                    subscriberBuilder = StoreSubscriberBuilderFn { store ->
                        if (store !is MultiStore5<S1, S2, S3, S4, S5>) throw IllegalArgumentException("error")
                        val selector = SelectorBuilder<MultiState5<S1, S2, S3, S4, S5>>()
                        val s1sel = selector.withSingleField { s1 }
                        val s2sel = selector.withSingleField { s2 }
                        val s3sel = selector.withSingleField { s3 }
                        val s4sel = selector.withSingleField { s4 }
                        val s5sel = selector.withSingleField { s5 }
                        val sub1 = m1.subscriberBuilder.build(store.store1)
                        val sub2 = m2.subscriberBuilder.build(store.store2)
                        val sub3 = m3.subscriberBuilder.build(store.store3)
                        val sub4 = m4.subscriberBuilder.build(store.store4)
                        val sub5 = m5.subscriberBuilder.build(store.store5)
                        StoreSubscriberFn {
                            val newS=store.state
                            s1sel.onChangeIn(newS) { sub1.onStateChange() }
                            s2sel.onChangeIn(newS) { sub2.onStateChange() }
                            s3sel.onChangeIn(newS) { sub3.onStateChange() }
                            s4sel.onChangeIn(newS) { sub4.onStateChange() }
                            s5sel.onChangeIn(newS) { sub5.onStateChange() }
                        }
                    })
        }
    }

}
