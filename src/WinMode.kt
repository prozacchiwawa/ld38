/**
 * Created by arty on 4/27/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

class WinMode(val team : Int, var state : GameState) : IGameMode {
    var elapsed = 0.0

    override fun runMode(t : Double) : IGameMode {
        elapsed += t
        if (elapsed > 5.0) {
            return YourTurnMode(state)
        } else {
            return this
        }
    }
    override fun getState() : GameState {
        return state
    }
    override fun render(ctx : CanvasRenderingContext2D) {
        var s1 = "rgba(255,255,255,0.6)"
        var s2 = "black"
        var txt = "You win"

        if (team != 0) {
            s1 = "rgba(255,0,0,0.6)"
            s2 = "white"
            if (team == -1) {
                txt = "You lost"
            } else {
                txt = "Team ${team} wins"
            }
        }

        ctx.fillStyle = s1
        ctx.fillRect(0.0, 0.0, screenX.toDouble(), screenY.toDouble())
        ctx.fillStyle = s2
        ctx.font = "40px sans"
        val m = ctx.measureText(txt)
        ctx.fillText(txt, (screenX - m.width) / 2.0, (screenY - 20.0) / 2.0)
    }
    override fun click(x : Double, y : Double) { }
    override fun drag(x : Double, y : Double, u : Double, v : Double) { }
    override fun move(x: Double, y: Double) {
    }
}
