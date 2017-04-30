/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

interface IGameMode {
    abstract fun runMode(t : Double) : IGameMode
    abstract fun getState() : GameState
    abstract fun render(ctx : CanvasRenderingContext2D)
    abstract fun click(x : Double, y : Double)
    abstract fun drag(x : Double, y : Double, u : Double, v : Double)
    abstract fun move(x : Double, y : Double)
}
