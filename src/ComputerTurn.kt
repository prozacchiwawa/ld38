/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D
import java.util.*


class ComputerTurnMode(val turn : Int, var state : GameState) : IGameMode {
    val colorSwaps = mapOf(Pair(0, "red"), Pair(1, "blue"), Pair(2, "green"), Pair(3, "yellow"))
    var elapsed = 0.0
    var text = "Thinking Hard!"

    override fun runMode(t : Double) : IGameMode {
        if (elapsed == 0.0) {
            val pathsForChars = state.logical.characters.values.filter { ch -> ch.team == turn }.map { ch ->
                val commands = state.findAWayForward(ch)
                ch.copy(path = commands)
            }
            val doCommands = ArrayList<Pair<Character,Command>>()
            state = GameState(pathsForChars.fold(state.logical, { gs,ch ->
                val commands = ch.path
                if (commands.size > 1) {
                    val firstCommand = commands[1]
                    doCommands.add(firstCommand)
                    val rest = commands.subList(1, commands.size)
                    val restArrayList: ArrayList<Pair<Character, Command>> = arrayListOf()
                    restArrayList.plusAssign(rest)
                    console.log("rest ${restArrayList}")
                    gs.copy(characters = gs.characters.plus(Pair(ch.id, ch.copy(path = restArrayList))))
                } else {
                    gs
                }
            }))
            for (cmd in doCommands) {
                console.log("DO ${cmd}")
                val inTheWay = state.logical.characters.values.filter { ch ->
                    ch.x == cmd.second.location.first && ch.y == cmd.second.location.second && ch.team != turn
                }.toList()
                if (inTheWay.size > 0) {
                    val pathCopy : ArrayList<Pair<Character,Command>> = ArrayList<Pair<Character,Command>>(cmd.first.path)
                    pathCopy.add(0, cmd)
                    state = state.executeCommand(cmd.first.copy(path=pathCopy), CommandType.ATTACK, cmd.second.location.first, cmd.second.location.second)
                }
                state = state.executeCommand(cmd.first, cmd.second.type, cmd.second.location.first, cmd.second.location.second)
            }
        }
        elapsed += t
        if (turn == 3) {
            console.log("Handoff turn to player")
            return YourTurnIntroMode(state.doPostTurn())
        } else {
            console.log("Handoff turn to ${turn + 1}")
            return ComputerTurnMode(turn + 1, state)
        }
    }
    override fun getState() : GameState {
        return state
    }
    override fun click(x : Double, y : Double) {
    }
    override fun overlay(ctx : CanvasRenderingContext2D) {
        var alpha = 0.5
        if (elapsed > 1.5) {
            alpha = 2.0 - elapsed
        } else if (elapsed < 0.5) {
            alpha = elapsed
        }
        ctx.fillStyle = "rgba(0,0,0,${alpha})"
        ctx.fillRect(0.0, 0.0, screenX.toDouble(), screenY.toDouble())
        ctx.font = "48px serif"
        val tmetrics = ctx.measureText("Player ${turn} Move ${text}")
        val tleft = (screenX.toDouble() - tmetrics.width) / 2.0
        val ttop = (screenY - 48.0) / 2.0
        console.log(tleft,ttop)
        ctx.fillStyle = colorSwaps[turn]
        ctx.fillText("Player ${turn} Move: ${text}", tleft, ttop)
    }
    override fun underlay(boardDim : BoardDim, ctx : CanvasRenderingContext2D) {
    }
}