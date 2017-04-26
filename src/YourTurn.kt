/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D
import java.util.*

data class ClickAnim(val x : Double, val y : Double, val start : Double, val at : Double) {
    fun update(t : Double) : ClickAnim? {
        val newTime = this.at + t
        if (newTime > 0.3) {
            return null
        } else {
            return this.copy(at = this.at + t)
        }
    }

    fun render(ctx : CanvasRenderingContext2D) {
        val rad = 90.0 * at
        val fade = Math.max(0.0, 2.0 * (0.3 - at))
        val grd = ctx.createRadialGradient(x, y, 0.0, x, y, rad)
        grd.addColorStop(0.0, "rgba(255,255,0,0.0)")
        grd.addColorStop(1.0, "rgba(255,255,0,${fade})")
        ctx.fillStyle = grd
        ctx.fillRect(x - rad, y - rad, 2.0 * rad, 2.0 * rad)
    }
}

interface IGameSubmode {
    abstract fun finish() : Boolean
    abstract fun update(t : Double) : Pair<GameState,IGameSubmode>
    abstract fun overlay(ctx : CanvasRenderingContext2D)
    abstract fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D)
    abstract fun click(x : Double, y : Double) : Pair<GameState,IGameSubmode>
}

fun menuForGameState(dim : BoardDim, state : GameState, ch : Character, dp : CharacterDisplay, usable : Map<CommandType,Set<Ord>>) : Menu<CommandType?> {
    val near = Rect(dim.boardLeft + (dp.targetx * dim.tileSize), dim.boardTop + (dp.targety * dim.tileSize), dim.tileSize, dim.tileSize)
    val health = ch.health
    val menuItems : ArrayList<Pair<String,CommandType?>> = arrayListOf(
            Pair(ch.name, null),
            Pair(ch.charclass.toString(), null),
            Pair("HP ${health}", null),
            Pair("----", null)
    )
    for (cmd in arrayOf(
            Pair("Attack", CommandType.ATTACK),
            Pair("Open", CommandType.OPEN),
            Pair("Close", CommandType.CLOSE),
            Pair("Room", CommandType.SUPER),
            Pair("Special", CommandType.SPECIAL))) {
        if (usable.getOrElse(cmd.second, { setOf() }).count() > 0) {
            menuItems.add(cmd)
        }
    }
    return Menu(menuItems, 20.0, 5.0, near)
}

class SpriteAnim(val frames : Iterable<Int>, val duration : Double, val x : Int, val y : Int) {
    var elapsed = 0.0
    val frameList = frames.toList()

    fun update(t : Double) : SpriteAnim? {
        elapsed += t
        if (elapsed < duration) {
            return this
        } else {
            return null
        }
    }

    fun render(dim : BoardDim, ctx : CanvasRenderingContext2D) {
        val frame = Math.floor((elapsed / duration) * frameList.size)
        val atX = dim.boardLeft + (dim.tileSize * x)
        val atY = dim.boardTop + (dim.tileSize * y)
        console.log("Render ${frame} at ${atX},${atY}")
        placeSprite(assets, dim, ctx, frameList[frame], atX, atY)
    }
}

class YourTurnMode(var state : GameState) : IGameMode {
    var endTurn : Menu<Boolean>? = null
    var elapsed = 0.0
    var clickAnims : List<ClickAnim> = listOf()
    var doorSparks : List<SpriteAnim> = listOf()

    fun updateAnims(t : Double) {
        val empty : List<ClickAnim> = emptyList()
        clickAnims = empty.plus(clickAnims.mapNotNull { kv -> kv.update(t) })
        val ampty : List<SpriteAnim> = emptyList()
        doorSparks = ampty.plus(doorSparks.mapNotNull { kv -> kv.update(t) })
    }

    fun getHasTurn(chars : Map<String, Character>) : MutableSet<String> {
        val res : MutableSet<String> = mutableSetOf()
        for (kv in chars) {
            if (kv.value.team == 0) {
                res.add(kv.key)
            }
        }
        return res
    }

    override fun runMode(t : Double) : IGameMode {
        elapsed += t
        updateAnims(t)

        if ((rand() * 90.0).toInt() == 0) {
            val doors = state.logical.board.doors.filter { d -> d.value.hp == 0 }.toList()
            if (doors.size > 0) {
                val theDoor = Math.floor(rand() * doors.size)
                val door = doors[theDoor]
                val coords = state.logical.board.coordsOfOrd(door.first)
                doorSparks = doorSparks.plus(SpriteAnim(15..19, 0.5, coords.first, coords.second))
            }
        }

        // Run the game state
        state = state.run(t)

        return this
    }

    override fun getState() : GameState {
        return state
    }

    override fun click(x : Double, y : Double) {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        val xTile = Math.floor((x - dim.boardLeft) / dim.tileSize)
        val yTile = Math.floor((y - dim.boardTop) / dim.tileSize)
        console.log("mouse click ",xTile,yTile)
        clickAnims = clickAnims.plus(ClickAnim(x, y, elapsed, 0.0))
    }

    override fun overlay(ctx : CanvasRenderingContext2D) {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)

        // End turn
        val fontHeight = 1.2 * dim.tileSize
        val x = dim.boardLeft + dim.boardWidth
        val y = dim.boardTop + dim.boardHeight
        val et = Menu<Boolean>(arrayListOf(Pair("End Turn", true)), fontHeight, 0.0, Rect(screenX.toDouble(),screenY.toDouble(),0.0,0.0))
        endTurn = et
        if (et != null) {
            et.render(ctx)
        }

        // Fluff
        for (a in clickAnims) {
            a.render(ctx)
        }

        for (d in doorSparks) {
            d.render(dim, ctx)
        }
    }

    override fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
    }
}
