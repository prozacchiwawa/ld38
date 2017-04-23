/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

interface IGameMode {
    abstract fun runMode(t : Double) : IGameMode
    abstract fun getState() : GameState
    abstract fun click(x : Double, y : Double)
    abstract fun overlay(ctx : CanvasRenderingContext2D)
    abstract fun underlay(boardDim : BoardDim, ctx : CanvasRenderingContext2D)
}
