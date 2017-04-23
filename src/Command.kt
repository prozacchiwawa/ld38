/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

enum class CommandType {
    NOTHING, WAIT, MOVE, OPEN, CLOSE, ATTACK, SPECIAL, SUPER
}

data class Command(val type : CommandType, val location : Pair<Int, Int>) {

}