/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import java.util.*

data class ClickAnim(val x : Double, val y : Double, val start : Double, val at : Double, val color : RGBA) {
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
        grd.addColorStop(0.0, "rgba(${color.r},${color.g},${color.b},0.0)")
        grd.addColorStop(1.0, "rgba(${color.r},${color.g},${color.b},${fade})")
        ctx.fillStyle = grd
        ctx.fillRect(x - rad, y - rad, 2.0 * rad, 2.0 * rad)
    }
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
    var elapsed = 0.0
    var clickAnims : List<ClickAnim> = listOf()
    var doorSparks : List<SpriteAnim> = listOf()
    var charScrollAt = 0.0
    var boardX = 0.0
    var boardY = 0.0
    var boardScale = 0.0
    var compact = false
    var background = makeBaseBoard(state, 1.0, assets)
    var givingOrder : String? = null

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
        if (boardScale == 0.0) {
            var xScale = screenX / (state.logical.board.dimX * TILESIZE)
            var yScale = screenY / (state.logical.board.dimY * TILESIZE)
            boardScale = Math.max(xScale, yScale)
        }

        var width = state.logical.board.dimX * TILESIZE * boardScale
        var height = state.logical.board.dimY * TILESIZE * boardScale
        val needWidth = screenX / 2.0
        val needHeight = screenY / 2.0
        if (width < needWidth) {
            boardScale = needWidth / width
        }
        width = state.logical.board.dimX * TILESIZE * boardScale
        height = state.logical.board.dimY * TILESIZE * boardScale
        if (height < needHeight) {
            boardScale = needHeight / height
        }

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

    fun getBoardDim(boardX : Double, boardY : Double, boardScale : Double) : BoardDim {
        val board = state.logical.board
        val renderWidth = board.dimX * TILESIZE * boardScale
        val renderHeight = board.dimY * TILESIZE * boardScale
        val left = (screenX / 2.0) + boardX - (renderWidth / 2.0)
        val top = (screenY / 2.0) + boardY - (renderHeight / 2.0)
        return BoardDim(left, top, renderWidth, renderHeight, TILESIZE * boardScale)
    }

    fun getMouseTile(x : Double, y : Double) : Pair<Int,Int> {
        val dim = getBoardDim(boardX, boardY, boardScale)
        val xTile = Math.floor((x - dim.boardLeft) / dim.tileSize)
        val yTile = Math.floor((y - dim.boardTop) / dim.tileSize)
        return Pair(xTile, yTile)
    }

    override fun click(x : Double, y : Double) {
        val mouse = getMouseTile(x, y)
        console.log("mouse click", mouse)
        clickAnims = clickAnims.plus(ClickAnim(x, y, elapsed, 0.0, RGBA(255.0, 255.0, 0.0, 0.0)))
        val go = givingOrder
        if (go != null) {
            state = state.useCommand(go, Command(CommandType.IDLE, mouse, mouse))
            givingOrder = null
        } else {
            val matchingChar = state.logical.characters.values.filter { ch ->
                ch.x.toInt() == mouse.first && ch.y.toInt() == mouse.second
            }.take(1).firstOrNull()
            if (matchingChar != null) {
                givingOrder = matchingChar.id
            }
        }
    }

    override fun drag(x : Double, y : Double, u : Double, v : Double) {
        var boardX = boardX + x - u
        var boardY = boardY + y - v
        val width = boardScale * state.logical.board.dimX * TILESIZE
        val height = boardScale * state.logical.board.dimY * TILESIZE
        val dim = getBoardDim(boardX, boardY, boardScale)
        console.log("${dim}")
        if (dim.boardLeft > (screenX / 4.0)) {
            boardX -= dim.boardLeft - (screenX / 4.0)
        }
        if ((dim.boardLeft + dim.boardWidth) < (3.0 * screenX / 4.0)) {
            boardX -= (dim.boardLeft + dim.boardWidth) - (3.0 * screenX / 4.0)
        }
        if (dim.boardTop > (screenY / 4.0)) {
            boardY -= dim.boardTop - (screenY / 4.0)
        }
        if ((dim.boardTop + dim.boardHeight) < (3.0 * screenY / 4.0)) {
            boardY -= (dim.boardTop + dim.boardHeight) - (3.0 * screenY / 4.0)
        }
        this.boardX = boardX
        this.boardY = boardY
    }

    override fun render(ctx : CanvasRenderingContext2D) {
        val board = state.logical.board
        val dim = getBoardDim(boardX, boardY, boardScale)

        drawBoard(ctx, state, background, assets, boardX, boardY, boardScale)

        // End turn
        val fontHeight = 1.2 * dim.tileSize
        val x = dim.boardLeft + dim.boardWidth
        val y = dim.boardTop + dim.boardHeight

        // Draw character descriptions
        val characters = state.logical.characters.values.sortedBy { ch : Character ->
            if (ch.team < 0) {
                ch.team + 1000
            } else {
                ch.team
            }
        }

        // Fluff
        for (a in clickAnims) {
            a.render(ctx)
        }

        for (d in doorSparks) {
            d.render(dim, ctx)
        }
    }

    fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
    }
}
