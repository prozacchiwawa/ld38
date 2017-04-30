/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

enum class CommandType {
    IDLE, ATTACK, SPECIAL, SUPER
}

data class Command(val type : CommandType, val at : Ord, val toward : Ord) {
}