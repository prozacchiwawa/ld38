/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa;

fun getCurTime() : Double {
    return js("(new Date().getTime()) / 1000.0")
}

fun rand() : Double {
    return js("Math.random()")
}
