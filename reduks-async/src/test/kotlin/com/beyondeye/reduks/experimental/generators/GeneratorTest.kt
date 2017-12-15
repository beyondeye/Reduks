package com.beyondeye.reduks.experimental.generators

import org.junit.Assert.*
import org.junit.Test

/**
 * Created by daely on 12/15/2017.
 */
class GeneratorTest
{
   // tests inspired by https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Statements/function*
   @Test
    fun testSimple() {
        fun idMaker() = generate<Int, Unit> {
            var index = 0
            while (index < 3)
                yield(index++)
        }
        val gen = idMaker()
        println(gen.next(Unit)) // 0
        println(gen.next(Unit)) // 1
        println(gen.next(Unit)) // 2
        println(gen.next(Unit)) // null
    }

    @Test
    fun testYeldAll() {
        fun anotherGenerator(i: Int) = generate<Int, Unit> {
            yield(i + 1)
            yield(i + 2)
            yield(i + 3)
        }

        fun generator(i: Int) = generate<Int, Unit> {
            yield(i)
            yieldAll(anotherGenerator(i), Unit)
            yield(i + 10)
        }
        val gen = generator(10)
        println(gen.next(Unit)) // 10
        println(gen.next(Unit)) // 11
        println(gen.next(Unit)) // 12
        println(gen.next(Unit)) // 13
        println(gen.next(Unit)) // 20
        println(gen.next(Unit)) // null
    }
    @Test
    fun testPassingArgumentsToGenerator(){
        fun logGenerator() = generate<Unit, String> {
            println("Started with $it")
            println(yield(Unit))
            println(yield(Unit))
            println(yield(Unit))
        }
        val gen = logGenerator()
        gen.next("start") // start
        gen.next("pretzel") // pretzel
        gen.next("california") // california
        gen.next("mayonnaise") // mayonnaise
    }

}