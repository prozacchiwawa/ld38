/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.*
import kotlin.math.floor
import kotlin.math.max

val PAUSE_BUTTON_TEXT = 30.0
val PAUSE_BUTTON_MARGIN = 7.5

data class ClickAnim(var x : Double, var y : Double, val start : Double, val at : Double, val repeat : Boolean, val color : RGBA) {
    fun update(t : Double) : ClickAnim? {
        val newTime = this.at + t
        if (newTime > 0.3 && !repeat) {
            return null
        } else {
            return this.copy(at = this.at + t)
        }
    }

    fun render(ctx : CanvasRenderingContext2D) {
        val rad = 90.0 * at
        val fade = max(0.0, 2.0 * (0.3 - at))
        val grd = ctx.createRadialGradient(x, y, 0.0, x, y, rad)
        grd.addColorStop(0.0, "rgba(${color.r},${color.g},${color.b},0.0)")
        grd.addColorStop(1.0, "rgba(${color.r},${color.g},${color.b},${fade})")
        ctx.fillStyle = grd
        ctx.fillRect(x - rad, y - rad, 2.0 * rad, 2.0 * rad)
    }
}

class SpriteAnim(val frames : Iterable<Int>, val duration : Double, val x : Double, val y : Double) {
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
        val frame = floor((elapsed / duration) * frameList.size).toInt()
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
    var orderMarker : ClickAnim? = null
    var showMe : String? = null
    var paused : Boolean = true
    var pausedRect : Rect? = null
    var sel : Ord? = null

    fun updateAnims(t : Double) {
        val empty : List<ClickAnim> = emptyList()
        clickAnims = empty.plus(clickAnims.mapNotNull { kv -> kv.update(t) })
        val ampty : List<SpriteAnim> = emptyList()
        doorSparks = ampty.plus(doorSparks.mapNotNull { kv -> kv.update(t) })
    }

    override fun runMode(t : Double) : IGameMode {
        if (boardScale == 0.0) {
            var xScale = screenX / (state.logical.board.dimX * TILESIZE)
            var yScale = screenY / (state.logical.board.dimY * TILESIZE)
            boardScale = max(xScale, yScale)
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
            val doors = state.logical.board.doors.filter { d -> d.value.hp == 0.0 }.toList()
            if (doors.size > 0) {
                val theDoor = floor(rand() * doors.size).toInt()
                val door = doors[theDoor]
                val ord = state.logical.board.ordOfIdx(door.first)
                doorSparks = doorSparks.plus(SpriteAnim(15..19, 0.5, ord.x, ord.y))
            }
        }

        // Run the game state
        if (!paused) {
            state = state.run(t)
        }

        val seatPositions = state.logical.chairs.map { chair -> Pair(chair.value.idx,chair.key) }.toMap()
        val teamsHoldingSeats = (0..3).map { team ->
            Pair(team, state.logical.getCharacters().values.filter { ch ->
                ch.team == team && seatPositions.containsKey(ch.at.idx)
            }.count())
        }
        val yourGuys = state.logical.getCharacters().values.filter { ch -> ch.team == 0 }.count()
        if (yourGuys < 1) {
            return WinMode(-1, state)
        }
        val winningTeam = teamsHoldingSeats.filter { t -> t.second >= 3 }.firstOrNull()
        if (winningTeam != null) {
            return WinMode(winningTeam.first, state)
        } else {
            return this
        }
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

    fun getMouseTile(x : Double, y : Double) : Ord {
        val dim = getBoardDim(boardX, boardY, boardScale)
        val xTile = (x - dim.boardLeft) / dim.tileSize
        val yTile = (y - dim.boardTop) / dim.tileSize
        return state.logical.board.ordOfCoords(xTile, yTile)
    }

    override fun click(x : Double, y : Double) {
        val mouse = getMouseTile(x, y)
        val go = givingOrder
        clickAnims = clickAnims.plus(ClickAnim(x, y, elapsed, 0.0, false, RGBA(255.0, 255.0, 0.0, 0.0)))
        showMe = null

        val pr = pausedRect
        if (pr != null) {
            if (pr.inside(x,y)) {
                paused = !paused
                return
            }
        }

        if (go != null) {
            val mouse = mouse.set(floor(mouse.x) + 0.5, floor(mouse.y) + 0.5)
            state = state.useCommand(go, Command(CommandType.IDLE, mouse, mouse))
            showMe = null
            givingOrder = null
            orderMarker = null
        } else {
            val matchingChar = state.logical.getCollision().collide("", object : IObjGetPos {
                override fun getPos(): ObjPos { return ObjPos(mouse.x, mouse.y, 0.0, 0.1) }
            }).flatMap { name ->
                val ch = state.logical.getCharacters()[name]
                if (ch != null) { listOf(ch) } else { listOf() }
            }.firstOrNull()

            if (matchingChar != null) {
                console.log(matchingChar)
                if (matchingChar.team == 0) {
                    givingOrder = matchingChar.id
                } else {
                    showMe = matchingChar.id
                }
            }
        }
    }

    override fun drag(x : Double, y : Double, u : Double, v : Double) {
        var boardX = boardX + x - u
        var boardY = boardY + y - v
        val width = boardScale * state.logical.board.dimX * TILESIZE
        val height = boardScale * state.logical.board.dimY * TILESIZE
        val dim = getBoardDim(boardX, boardY, boardScale)
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

    override fun move(x : Double, y : Double) {
        val mouse = getMouseTile(x, y)
        sel = mouse
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
        val characters = state.logical.getCharacters().values.sortedBy { ch : Character ->
            if (ch.team < 0) {
                ch.team + 1000
            } else {
                ch.team
            }
        }

        // Character name
        val go = givingOrder
        if (go != null) {
            ctx.fillStyle = "white"
            ctx.font = "40px serif"
            ctx.fillText("Move: $go", 0.0, 40.0)
            val ch = state.logical.getCharacters()[go]
            if (ch != null) {
                val om = orderMarker
                if (om == null) {
                    orderMarker = ClickAnim(dim.boardLeft + (ch.at.x * TILESIZE), dim.boardTop + (ch.at.y * TILESIZE), 0.0, 0.0, true, RGBA(0.0, 255.0, 255.0, 0.7))
                } else {
                    om.x = dim.boardLeft + (ch.at.x * TILESIZE)
                    om.y = dim.boardTop + (ch.at.y * TILESIZE)
                }
            }
        }

        // Fluff
        for (a in clickAnims) {
            a.render(ctx)
        }

        for (d in doorSparks) {
            d.render(dim, ctx)
        }

        val sm = showMe
        if (sm != null) {
            val ch = state.logical.getCharacters()[sm]
            if (sm != null) {
                ctx.fillStyle = "rgba(0,0,0,0.5)"
                ctx.fillRect(0.0,0.0,screenX.toDouble(),screenY.toDouble())
                val printed = "$ch".split(",")
                for (p in 0..(printed.size-1)) {
                    ctx.font = "12px serif"
                    ctx.fillStyle = "#44ffaa"
                    ctx.fillText("${printed[p]}", 0.0, (70.0 + 12.0 * p))
                }
            }
        }

        val om = orderMarker
        if (om != null) {
            om.render(ctx)
        }

        val s = sel
        if (s != null) {
            val tx = floor(s.x)
            val ty = floor(s.y)
            ctx.fillStyle = "rgba(255,255,128,0.5)"
            ctx.fillRect(dim.boardLeft + (floor(s.x) * dim.tileSize), dim.boardTop + (floor(s.y) * dim.tileSize), dim.tileSize, dim.tileSize)
        }

        ctx.font = "${PAUSE_BUTTON_TEXT}px Serif"
        val pausedTm = ctx.measureText("PAUSED")
        val pauseTm = ctx.measureText("PAUSE")
        val pr = Rect(((screenX - pausedTm.width) / 2.0) - PAUSE_BUTTON_MARGIN,0.0,pausedTm.width + (2.0 * PAUSE_BUTTON_MARGIN),PAUSE_BUTTON_TEXT + (2.0 * PAUSE_BUTTON_MARGIN))
        pausedRect = pr
        if (paused) {
            ctx.fillStyle = "rgb(41, 160, 20)"
            ctx.fillRect(pr.left, pr.top, pr.width, pr.height)
            ctx.fillStyle = "white"
            ctx.textBaseline = CanvasTextBaseline.TOP
            ctx.fillText("PAUSED", (screenX - pausedTm.width) / 2.0, PAUSE_BUTTON_MARGIN)
        } else {
            ctx.fillStyle = "rgb(198, 43, 15)"
            ctx.fillRect(pr.left, pr.top, pr.width, pr.height)
            ctx.fillStyle = "white"
            ctx.textBaseline = CanvasTextBaseline.TOP
            ctx.fillText("PAUSE", (screenX - pauseTm.width) / 2.0, PAUSE_BUTTON_MARGIN)
        }
    }

    fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
    }
}
