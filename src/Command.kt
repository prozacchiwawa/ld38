/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

enum class CommandType {
    IDLE, OPEN, CLOSE, ATTACK, SPECIAL, SUPER
}

data class Command(val type : CommandType, val at : Pair<Int, Int>, val toward : Pair<Int,Int>) {
}