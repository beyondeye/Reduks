package com.beyondeye.reduks

/**
 * Created by Dario on 4/3/2016.
 */
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class ReselectTestIgnored {
    @Ignore //not implemented
    @Test
    fun FirstArgumentCanBeAnArrayTest() {
        /*
            const selector = createSelector(
      [ state => state.a, state => state.b ],
      (a, b) => {
        return a + b
      }
    )
    assert.equal(selector({ a: 1, b: 2 }), 3)
    assert.equal(selector({ a: 1, b: 2 }), 3)
    assert.equal(selector.recomputations(), 1)
    assert.equal(selector({ a: 3, b: 2 }), 5)
    assert.equal(selector.recomputations(), 2)
         */
    }
    //TODO I have not really implemented props, but not sure what is the use case.
    @Test
    fun CanAcceptPropsTest() {

        val selector = SelectorBuilder<ReselectTest.StateAB>()
                .withField { a }
                .withField { b }
                .withField { 100 }
                .compute { a: Int, b: Int, c: Int -> a + b + c }
        val state1 = ReselectTest.StateAB(a = 1, b = 2)
        assertThat(selector(state1)).isEqualTo(103)

        /*
       let called = 0
       const selector = createSelector(
         state => state.a,
         state => state.b,
         (state, props) => props.c,
         (a, b, c) => {
           called++
           return a + b + c
         }
       )
       assert.equal(selector({ a: 1, b: 2 }, { c: 100 }), 103)
            */
    }
    @Ignore //not implemented
    @Test
    fun ChainedSelectorWithProps() {
        /*
     const selector1 = createSelector(
      state => state.sub,
        (state, props) => props.x,
        (sub, x) => ({ sub, x })
    )
    const selector2 = createSelector(
      selector1,
      (state, props) => props.y,
        (param, y) => param.sub.value + param.x + y
    )
    const state1 = { sub: {  value: 1 } }
    assert.equal(selector2(state1, { x: 100, y: 200 }), 301)
    assert.equal(selector2(state1, { x: 100, y: 200 }), 301)
    assert.equal(selector2.recomputations(), 1)
    const state2 = { sub: {  value: 2 } }
    assert.equal(selector2(state2, { x: 100, y: 201 }), 303)
    assert.equal(selector2.recomputations(), 2)
         */
    }
    @Ignore //not implemented
    @Test
    fun ChainedSelectorWithVariadicArgs() {
        /*
    const selector1 = createSelector(
      state => state.sub,
        (state, props, another) => props.x + another,
        (sub, x) => ({ sub, x })
    )
    const selector2 = createSelector(
      selector1,
      (state, props) => props.y,
        (param, y) => param.sub.value + param.x + y
    )
    const state1 = { sub: {  value: 1 } }
    assert.equal(selector2(state1, { x: 100, y: 200 }, 100), 401)
    assert.equal(selector2(state1, { x: 100, y: 200 }, 100), 401)
    assert.equal(selector2.recomputations(), 1)
    const state2 = { sub: {  value: 2 } }
    assert.equal(selector2(state2, { x: 100, y: 201 }, 200), 503)
    assert.equal(selector2.recomputations(), 2)
         */
    }

    @Ignore //not implemented
    @Test
    fun overrideValueEquals() {
        /*
       // a rather absurd equals operation we can verify in tests
    const createOverridenSelector = createSelectorCreator(
      defaultMemoize,
      (a, b) => typeof a === typeof b
    )
    const selector = createOverridenSelector(
      state => state.a,
        a => a
    )
    assert.equal(selector({ a: 1 }), 1)
    assert.equal(selector({ a: 2 }), 1) // yes, really true
    assert.equal(selector.recomputations(), 1)
    assert.equal(selector({ a: 'A' }), 'A')
    assert.equal(selector.recomputations(), 2)

         */
    }

    @Ignore //not implemented
    @Test
    fun customMemoizeTest() {
        /*
       const hashFn = (...args) => args.reduce((acc, val) => acc + '-' + JSON.stringify(val))
    const customSelectorCreator = createSelectorCreator(
      lodashMemoize,
      hashFn
    )
    const selector = customSelectorCreator(
      state => state.a,
      state => state.b,
      (a, b) => a + b
    )
    assert.equal(selector({ a: 1, b: 2 }), 3)
    assert.equal(selector({ a: 1, b: 2 }), 3)
    assert.equal(selector.recomputations(), 1)
    assert.equal(selector({ a: 1, b: 3 }), 4)
    assert.equal(selector.recomputations(), 2)
    assert.equal(selector({ a: 1, b: 3 }), 4)
    assert.equal(selector.recomputations(), 2)
    assert.equal(selector({ a: 2, b: 3 }), 5)
    assert.equal(selector.recomputations(), 3)
    // TODO: Check correct memoize function was called
         */
    }
    @Ignore //not implemented
    @Test
    fun exportedMemoizeTest() {
        /*
       let called = 0
    const memoized = defaultMemoize(state => {
      called++
      return state.a
    })

    const o1 = { a: 1 }
    const o2 = { a: 2 }
    assert.equal(memoized(o1), 1)
    assert.equal(memoized(o1), 1)
    assert.equal(called, 1)
    assert.equal(memoized(o2), 2)
    assert.equal(called, 2)
         */
    }

    @Ignore //not implemented
    @Test
    fun exportedMemoizeWithMultipleArgsTest() {
        /*
            const memoized = defaultMemoize((...args) => args.reduce((sum, value) => sum + value, 0))
    assert.equal(memoized(1, 2), 3)
    assert.equal(memoized(1), 1)
         */
    }

    @Ignore //not implemented
    @Test
    fun exportedMemoizeWithValueEqualsOverride() {
        /*
     // a rather absurd equals operation we can verify in tests
    let called = 0
    const valueEquals = (a, b) => typeof a === typeof b
    const memoized = defaultMemoize(
      a => {
        called++
        return a
      },
      valueEquals
    )
    assert.equal(memoized(1), 1)
    assert.equal(memoized(2), 1) // yes, really true
    assert.equal(called, 1)
    assert.equal(memoized('A'), 'A')
    assert.equal(called, 2)
         */
    }

    @Ignore //not implemented
    @Test
    fun structuredSelectorTest() {
        /*
      const selector = createStructuredSelector({
      x: state => state.a,
      y: state => state.b
    })
    const firstResult = selector({ a: 1, b: 2 })
    assert.deepEqual(firstResult, { x: 1, y: 2 })
    assert.strictEqual(selector({ a: 1, b: 2 }), firstResult)
    const secondResult = selector({ a: 2, b: 2 })
    assert.deepEqual(secondResult, { x: 2, y: 2 })
    assert.strictEqual(selector({ a: 2, b: 2 }), secondResult)
         */
    }

    @Ignore //not implemented
    @Test
    fun structuredSelectorWithCustomSelectorCreator() {
        /*
       const customSelectorCreator = createSelectorCreator(
      defaultMemoize,
      (a, b) => a === b
    )
    const selector = createStructuredSelector({
      x: state => state.a,
      y: state => state.b
    }, customSelectorCreator)
    const firstResult = selector({ a: 1, b: 2 })
    assert.deepEqual(firstResult, { x: 1, y: 2 })
    assert.strictEqual(selector({ a: 1, b: 2 }), firstResult)
    assert.deepEqual(selector({ a: 2, b: 2 }), { x: 2, y: 2 })
         */
    }

}