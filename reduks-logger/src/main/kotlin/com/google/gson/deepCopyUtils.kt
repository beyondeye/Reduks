package com.google.gson

/**
 * hack for making deepCopy public
 * see also https://github.com/google/gson/issues/760 (why deepCopy is not public)
 * Created by daely on 7/22/2016.
 */
fun JsonElement.publicDeepCopy():JsonElement {
    return this.deepCopy()
}