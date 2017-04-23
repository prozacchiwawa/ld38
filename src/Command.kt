/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

data class Command(
        val who : String,
        val move : Pair<Int, Int>?,
        val open : Pair<Int, Int>?,
        val close : Pair<Int, Int>?,
        val attack : Pair<Int, Int>?,
        val superpower : Pair<Int, Int>?,
        val special : Pair<SquareAssoc, SquareAssoc>?
)

private val emptyCommand = Command(who = "", move = null, open = null, close = null, attack = null, superpower = null, special = null)
fun NullCommand() : Command { return emptyCommand }
fun MoveCommand(who : String, where : Pair<Int, Int>) : Command { return emptyCommand.copy(who = who, move = where) }
fun OpenCommand(who : String, where : Pair<Int, Int>) : Command { return emptyCommand.copy(who = who, open = where) }
fun CloseCommand(who : String, where : Pair<Int, Int>) : Command { return emptyCommand.copy(who = who, close = where) }
fun AttackCommand(who : String, where : Pair<Int, Int>) : Command { return emptyCommand.copy(who = who, attack = where) }
fun SuperCommand(who : String, where : Pair<Int, Int>) : Command { return emptyCommand.copy(who = who, superpower = where) }
fun SpecialCommand(who : String, special : Pair<SquareAssoc, SquareAssoc>) : Command { return emptyCommand.copy(who = who, special = special) }
