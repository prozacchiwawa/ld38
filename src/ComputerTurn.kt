/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D



class ComputerTurnMode(val turn : Int, var state : GameState) : IGameMode {
    val colorSwaps = mapOf(Pair(0, "red"), Pair(1, "blue"), Pair(2, "green"), Pair(3, "yellow"))
    var elapsed = 0.0
    override fun runMode(t : Double) : IGameMode {
        elapsed += t
        if (elapsed > 2.0) {
            val commands = state.findAWayForward(turn)
            for (cmd in commands) {
                state = state.executeCommand(cmd.first, cmd.second.type, cmd.second.location.first, cmd.second.location.second)
            }
            if (turn == 3) {
                return YourTurnIntroMode(state.doPostTurn())
            } else {
                return ComputerTurnMode(turn + 1, state)
            }
        }
        return this
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
        val tmetrics = ctx.measureText("Player ${turn} Move")
        val tleft = (screenX.toDouble() - tmetrics.width) / 2.0
        val ttop = (screenY - 48.0) / 2.0
        console.log(tleft,ttop)
        ctx.fillStyle = colorSwaps[turn]
        ctx.fillText("Player ${turn} Move", tleft, ttop)
    }
    override fun underlay(boardDim : BoardDim, ctx : CanvasRenderingContext2D) {
    }
}