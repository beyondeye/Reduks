package com.beyondeye.reduks.middlewares.saga

/**
 * Created by daely on 12/19/2017.
 */

/**
 * type alias for lambda to be run by a saga
 */
typealias  TopLevelSagaFn<S> = suspend Saga<S>.() -> Unit

interface SagaFn<S:Any,out R:Any> {
    val name:String
    suspend fun invoke(saga: Saga<S>):R
}

class SagaFn0<S:Any,out R:Any>(override val name:String,val  fn:suspend Saga<S>.()->R): SagaFn<S, R>
{
    override suspend fun invoke(saga: Saga<S>): R {
        return saga.fn()
    }
}

class SagaFn1<S:Any,P1,out R:Any>(val name:String, val  fn:suspend Saga<S>.(p1:P1)->R) {
    fun withArgs(p1:P1)= SagaFn1WithArgs(this, p1)
}
class SagaFn2<S:Any,P1,P2,out R:Any>(val name:String, val  fn:suspend Saga<S>.(p1:P1, p2:P2)->R) {
    fun withArgs(p1:P1,p2:P2)= SagaFn2WithArgs(this, p1, p2)
}
class SagaFn3<S:Any,P1,P2,P3,out R:Any>(val name:String, val  fn:suspend Saga<S>.(p1:P1, p2:P2, p3:P3)->R) {
    fun withArgs(p1:P1,p2:P2,p3:P3)= SagaFn3WithArgs(this, p1, p2, p3)
}


class SagaFn1WithArgs<S:Any,P1,out R:Any>(override val name:String, val  fn:suspend Saga<S>.(p1:P1)->R, val p1:P1): SagaFn<S, R>
{
    constructor(fn_: SagaFn1<S, P1, R>, p1:P1): this(fn_.name,fn_.fn,p1)
    override suspend fun invoke(saga: Saga<S>): R {
        return saga.fn(p1)
    }
}
class SagaFn2WithArgs<S:Any,P1,P2,out R:Any>(override val name:String, val  fn:suspend Saga<S>.(p1:P1, p2:P2)->R, val p1:P1, val p2:P2): SagaFn<S, R>
{
    constructor(fn_: SagaFn2<S, P1, P2, R>, p1:P1, p2:P2): this(fn_.name,fn_.fn,p1,p2)
    override suspend fun invoke(saga: Saga<S>): R {
        return saga.fn(p1,p2)
    }
}
class SagaFn3WithArgs<S:Any,P1,P2,P3,out R:Any>(override val name:String, val  fn:suspend Saga<S>.(p1:P1, p2:P2, p3:P3)->R, val p1:P1, val p2:P2, val p3:P3): SagaFn<S, R>
{
    constructor(fn_: SagaFn3<S, P1, P2, P3, R>, p1:P1, p2:P2, p3:P3): this(fn_.name,fn_.fn,p1,p2,p3)
    override suspend fun invoke(saga: Saga<S>): R {
        return saga.fn(p1,p2,p3)
    }
}
