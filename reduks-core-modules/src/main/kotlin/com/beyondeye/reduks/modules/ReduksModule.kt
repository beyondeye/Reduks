package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * a builder function for [ReduksModule.Def] that set the context to the default context for the state
 */
inline fun <reified S:Any> ModuleDef(
        ctx: ReduksContext = ReduksContext.default<S>(),
        storeCreator:StoreCreator<S>,
        initialState: S,
        startAction: Any=INIT(),
        stateReducer: Reducer<S>,
        subscriberBuilder: StoreSubscriberBuilder<S>?=null)=
        ReduksModule.Def<S>(ctx, storeCreator, initialState, startAction, stateReducer, subscriberBuilder)

/**
 * generic redux module
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
            val storeCreator:StoreCreator<State>,
            /**
             * return the initial state
             */
            val initialState: State,
            /**
             * return the initial action to dispatch to the Store
             */
            val startAction: Any?,
            /**
             * return the state reducer
             */
            val stateReducer: Reducer<State>,
            /**
             * return the main store subscriber
             * we pass as argument the store itself, so that we can create an object that implement the
             * [StoreSubscriberFn] interface that keep a reference to the store itself, in case the we need call dispatch
             * in the subscriber
             * Define as null if no main store subscriber is defined
             */
            val subscriberBuilder: StoreSubscriberBuilder<State>?)
    override val ctx: ReduksContext
    override val store: Store<State>
    override val storeSubscriptionsByTag: MutableMap<String,StoreSubscription> = mutableMapOf()
    override val busStoreSubscriptionsByTag:MutableMap<String,MutableList<StoreSubscription>> = mutableMapOf()
    init {
        ctx=moduleDef.ctx
        val storeCreator= moduleDef.storeCreator
        store=storeCreator.create(moduleDef.stateReducer, moduleDef.initialState)
        val storeSubscriber= moduleDef.subscriberBuilder?.build(store)
        storeSubscriber?.let {
            val storeSubscription = store.subscribe(storeSubscriber)
            storeSubscriptionsByTag.put(Reduks.TagMainSubscription,storeSubscription)
        }
        moduleDef.startAction?.let { sa->
            //split multiaction to list if required
            val actionList: List<Any?> = MultiActionWithContext.toActionList(sa)
            actionList.forEach { if(it!=null) store.dispatch(it) }
        }
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

        fun <S1 : Any, S2 : Any> MultiDef(m1: ReduksModule.Def<S1>,
                                          m2: ReduksModule.Def<S2>): ReduksModule.Def<MultiState2<S1, S2>> {
            val mctx = multiContext(m1.ctx, m2.ctx)
            return ReduksModule.Def<MultiState2<S1, S2>>(
                    ctx = mctx,
                    storeCreator = MultiStore2.Factory<S1, S2>(
                            m1.ctx, m1.storeCreator, m1.subscriberBuilder,
                            m2.ctx, m2.storeCreator, m2.subscriberBuilder),
                    initialState = MultiState2(m1.initialState, m2.initialState),
                    startAction = MultiActionWithContext(
                            m1.startAction?.let { ActionWithContext(it, m1.ctx) },
                            m2.startAction?.let { ActionWithContext(it, m2.ctx) }
                    ),
                    stateReducer = MultiReducer2<S1, S2>(m1, m2),
                    subscriberBuilder = null
            )
        }
        //--------------------------------
        fun <S1 : Any, S2 : Any, S3 : Any> MultiDef(      m1: ReduksModule.Def<S1>,
                                                          m2: ReduksModule.Def<S2>,
                                                          m3: ReduksModule.Def<S3>): ReduksModule.Def<MultiState3<S1, S2, S3>> {
            val mctx = multiContext(m1.ctx, m2.ctx, m3.ctx)
            return ReduksModule.Def<MultiState3<S1, S2, S3>>(
                    ctx = mctx,
                    storeCreator = MultiStore3.Factory<S1, S2, S3>(
                            m1.ctx,m1.storeCreator,m1.subscriberBuilder,
                            m2.ctx,m2.storeCreator,m2.subscriberBuilder,
                            m3.ctx,m3.storeCreator,m3.subscriberBuilder),
                    initialState = MultiState3(m1.initialState, m2.initialState, m3.initialState),
                    startAction = MultiActionWithContext(
                            m1.startAction?.let { ActionWithContext(it, m1.ctx) },
                            m2.startAction?.let { ActionWithContext(it, m2.ctx) },
                            m3.startAction?.let { ActionWithContext(it, m3.ctx) }
                    ),
                    stateReducer = MultiReducer3<S1, S2, S3>(m1, m2, m3),
                    subscriberBuilder = null
                    )
        }
        //--------------------------------
        fun <S1 : Any, S2 : Any, S3 : Any, S4 : Any> MultiDef(      m1: ReduksModule.Def<S1>,
                                                                    m2: ReduksModule.Def<S2>,
                                                                    m3: ReduksModule.Def<S3>,
                                                                    m4: ReduksModule.Def<S4>): ReduksModule.Def<MultiState4<S1, S2, S3, S4>> {
            val mctx = multiContext(m1.ctx, m2.ctx, m3.ctx, m4.ctx)
            return ReduksModule.Def<MultiState4<S1, S2, S3, S4>>(
                    ctx = mctx,
                    storeCreator = MultiStore4.Factory<S1, S2, S3, S4>(
                            m1.ctx,m1.storeCreator,m1.subscriberBuilder,
                            m2.ctx,m2.storeCreator,m2.subscriberBuilder,
                            m3.ctx,m3.storeCreator,m3.subscriberBuilder,
                            m4.ctx,m4.storeCreator,m4.subscriberBuilder),
                    initialState = MultiState4(m1.initialState, m2.initialState, m3.initialState, m4.initialState),
                    startAction = MultiActionWithContext(
                            m1.startAction?.let { ActionWithContext(it, m1.ctx) },
                            m2.startAction?.let { ActionWithContext(it, m2.ctx) },
                            m3.startAction?.let { ActionWithContext(it, m3.ctx) },
                            m4.startAction?.let { ActionWithContext(it, m4.ctx) }
                    ),
                    stateReducer = MultiReducer4<S1, S2, S3, S4>(m1, m2, m3, m4),
                    subscriberBuilder = null )
        }

        //--------------------------------
        fun <S1 : Any, S2 : Any, S3 : Any, S4 : Any, S5 : Any> MultiDef(      m1: ReduksModule.Def<S1>,
                                                                              m2: ReduksModule.Def<S2>,
                                                                              m3: ReduksModule.Def<S3>,
                                                                              m4: ReduksModule.Def<S4>,
                                                                              m5: ReduksModule.Def<S5>): ReduksModule.Def<MultiState5<S1, S2, S3, S4, S5>> {
            val mctx = multiContext(m1.ctx, m2.ctx, m3.ctx, m4.ctx, m5.ctx)
            return ReduksModule.Def<MultiState5<S1, S2, S3, S4, S5>>(
                    ctx = mctx,
                    storeCreator = MultiStore5.Factory<S1, S2, S3, S4, S5>(
                            m1.ctx, m1.storeCreator,m1.subscriberBuilder,
                            m2.ctx, m2.storeCreator,m2.subscriberBuilder,
                            m3.ctx, m3.storeCreator,m3.subscriberBuilder,
                            m4.ctx, m4.storeCreator,m4.subscriberBuilder,
                            m5.ctx, m5.storeCreator,m5.subscriberBuilder),
                    initialState = MultiState5(m1.initialState, m2.initialState, m3.initialState, m4.initialState, m5.initialState),
                    startAction = MultiActionWithContext(
                            m1.startAction?.let { ActionWithContext(it, m1.ctx) },
                            m2.startAction?.let { ActionWithContext(it, m2.ctx) },
                            m3.startAction?.let { ActionWithContext(it, m3.ctx) },
                            m4.startAction?.let { ActionWithContext(it, m4.ctx) },
                            m5.startAction?.let { ActionWithContext(it, m5.ctx) }
                    ),
                    stateReducer = MultiReducer5<S1, S2, S3, S4, S5>(m1, m2, m3, m4, m5),
                    subscriberBuilder = null )
        }
    }

}
