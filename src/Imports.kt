/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa;

fun getCurTime() : Double {
    return js("(new Date().getTime()) / 1000.0")
}

var random = { js("Math.random()") }

fun rand() : Double {
    return random()
}
