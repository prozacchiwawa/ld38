/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

//
// When it's my turn, I can click on things
// There's a simple state machine transitioned by click
//
// Nothing Selected ->
//   Non character moving -> describe window
//   Char moving -> Describe char window
//
// describe char window ->
//   Special effect button -> specialEffect(char)
//   Attack button -> attack(char)
//   Facing -> facing(char)
//   Highlighted squares -> move(char)
//
// facing ->
//   char.turn() -> describe char window
//
// attack ->
//   char.attack()
//   hasTurn.remove(char)
//   ephemeralAnimation(char.specialAnim)
//   if hasTurn.count > 0 then
//     Nothing moving
//   else
//     End turn
//
// move ->
//   char.move()
//   selection.moveTo(char)
//   describe char window
//
// specialEffect
//   char.specialEffect()
//   hasTurn.remove(char)
//   ephemeralAnimation(char.specialAnim)
//   if hasTurn.count > 0 then
//     Nothing moving
//   else
//     End turn
//
// describe window -> Nothing moving
//

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

fun getMoves(board : GameBoard, ch : Character) : Map<Int,Int> {
    val moves = ch.availMoves()
    val visited : MutableMap<Int, Int> = mutableMapOf(Pair(ch.y * board.dimX + ch.x, moves))
    val results : MutableMap<Int, Int> = mutableMapOf()
    while (visited.count() > 0) {
        val check = visited.asSequence().first()
        visited.remove(check.key)
        val checkM = results.get(check.key)
        val passable = board.isPassable(check.key % board.dimX, check.key / board.dimX)
        if ((checkM == null || checkM < check.value) && passable) {
            results.put(check.key, check.value)
        }
        if (check.value == 0 || !passable) {
            continue
        }
        val left = check.key - 1
        val right = check.key + 1
        val up = check.key - board.dimX
        val down = check.key + board.dimX
        visited.put(left, check.value - 1)
        visited.put(right, check.value - 1)
        visited.put(up, check.value - 1)
        visited.put(down, check.value - 1)
    }
    return results
}

interface IGameSubmode {
    abstract fun finish() : Boolean
    abstract fun update(t : Double)
    abstract fun overlay(ctx : CanvasRenderingContext2D)
    abstract fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D)
    abstract fun click(x : Double, y : Double) : Pair<GameState,IGameSubmode>
}

fun menuForGameState(dim : BoardDim, state : GameState, ch : Character, dp : CharacterDisplay) : Menu<CommandType> {
    val near = Rect(dim.boardLeft + (dp.targetx * dim.tileSize), dim.boardTop + (dp.targety * dim.tileSize), dim.tileSize, dim.tileSize)
    val menuItems = arrayListOf(
            Pair(ch.name, CommandType.NOTHING)
    )
    return Menu(menuItems, 20.0, 5.0, near)
}

data class PickingCharacterMode(val state : GameState, val hasTurn : Set<String>) : IGameSubmode {
    var elapsed = 0.0
    override fun finish() : Boolean { return hasTurn.count() == 0 }
    override fun update(t : Double) {
        elapsed += t
    }

    fun getAvailMoves(chars : Map<String, Character>) : MutableMap<String, Int> {
        val result : MutableMap<String, Int> = mutableMapOf()
        for (kv in chars) {
            result.put(kv.key, kv.value.availMoves())
        }
        return result
    }

    override fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
    }

    override fun overlay(ctx: CanvasRenderingContext2D) {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        for (name in hasTurn) {
            val ch = state.display.characters.get(name)
            if (ch != null) {
                var cycle = elapsed * 2.0
                var stage = elapsed - Math.floor(elapsed)
                var sprite = Math.floor(3.0 + (stage * 4.0))
                placeSprite(assets, dim, ctx, sprite, ch.dispx, ch.dispy)
            }
        }
    }
    override fun click(x : Double, y : Double) : Pair<GameState,IGameSubmode> {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        val xTile = Math.floor((x - dim.boardLeft) / dim.tileSize)
        val yTile = Math.floor((y - dim.boardTop) / dim.tileSize)
        for (chName in hasTurn) {
            val dp = state.display.characters.get(chName)
            val ch = state.logical.characters.get(chName)
            if (dp != null && ch != null && Math.abs(dp.targetx - xTile) < 0.01 && Math.abs(dp.targety - yTile) < 0.01) {
                val menu = menuForGameState(dim, state, ch, dp)
                return Pair(state, CharacterMenuMode(state, ch, menu, emptyMap(), hasTurn))
            }
        }
        return Pair(state, this)
    }
}

data class CharacterMenuMode(val state : GameState, val ch : Character, val menu : Menu<CommandType>, val usable : Map<CommandType,Set<Ord>>, val hasTurn : Set<String>) : IGameSubmode {
    override fun finish() : Boolean { return hasTurn.count() == 0 }
    override fun update(t : Double) {
    }
    override fun overlay(ctx : CanvasRenderingContext2D) {
        menu.render(ctx)
    }
    override fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
    }
    override fun click(x : Double, y : Double) : Pair<GameState,IGameSubmode> {
        val clicked = menu.getSelection(x, y)
        if (clicked != null) {
            val selectableMoves = usable.get(clicked)
            if (selectableMoves != null && selectableMoves.count() > 0) {
                if (clicked == CommandType.MOVE) {
                    return Pair(state, CharacterMovementMode(state, ch, menu, usable, hasTurn))
                } else {
                    return Pair(state, PlacementSelectionMode(state, ch, clicked, usable, hasTurn))
                }
            }
        }
        return Pair(state,PickingCharacterMode(state, hasTurn))
    }
}

data class CharacterMovementMode(val state : GameState, val ch : Character, val menu : Menu<CommandType>, val usable : Map<CommandType,Set<Ord>>, val hasTurn : Set<String>) : IGameSubmode {
    override fun finish(): Boolean {
        return hasTurn.count() == 0
    }

    override fun update(t: Double) {
    }

    override fun overlay(ctx: CanvasRenderingContext2D) {
    }

    override fun underlay(dim: BoardDim, ctx: CanvasRenderingContext2D) {
    }

    override fun click(x: Double, y: Double): Pair<GameState, IGameSubmode> {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        val xTile = Math.floor((x - dim.boardLeft) / dim.tileSize)
        val yTile = Math.floor((y - dim.boardTop) / dim.tileSize)
        val cdisp = state.display.characters.entries.filter { kv -> Math.floor(kv.value.targetx) == xTile && Math.floor(kv.value.targety) == yTile }.first()
        if (cdisp != null)
        {
            state.display.characters.put(cdisp.key, cdisp.value.copy(targetx = xTile.toDouble(), targety = yTile.toDouble()))
            val menu = menuForGameState(dim, state, ch, cdisp.value)
            return Pair(state, CharacterMenuMode(state, ch, menu, emptyMap(), hasTurn))
        }
        return Pair(state, CharacterMenuMode(state, ch, menu, usable, hasTurn))
    }
}

data class PlacementSelectionMode(val state : GameState, val ch : Character, val cmd : CommandType, val usable : Map<CommandType,Set<Ord>>, val hasTurn : Set<String>) : IGameSubmode {
    override fun finish() : Boolean { return hasTurn.count() == 0 }
    override fun update(t : Double) {
    }
    override fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
        for (ord in usable.getOrElse(cmd, { setOf() })) {
            val coords = state.logical.board.coordsOfOrd(ord)
            ctx.fillStyle = "rgba(247, 245, 178, 0.3)"
            ctx.fillRect(dim.boardLeft + (coords.first * dim.tileSize), dim.boardTop + (coords.second * dim.tileSize), dim.tileSize, dim.tileSize)
            ctx.strokeStyle = "rgba(247, 245, 178, 0.6)"
            ctx.strokeRect(dim.boardLeft + (coords.first * dim.tileSize), dim.boardTop + (coords.second * dim.tileSize), dim.tileSize , dim.tileSize)
        }
    }
    override fun overlay(ctx : CanvasRenderingContext2D) {
    }
    override fun click(x : Double, y : Double) : Pair<GameState,IGameSubmode> {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        val xTile = Math.floor((x - dim.boardLeft) / dim.tileSize)
        val yTile = Math.floor((y - dim.boardTop) / dim.tileSize)
        val ord = board.ordOfCoords(xTile, yTile)
        if (usable.getOrElse(cmd, { setOf() }).contains(ord)) {
            val newState = state.executeCommand(ch, cmd, xTile, yTile)
            return Pair(newState, PickingCharacterMode(newState, hasTurn.minus(ch.id)))
        } else {
            return Pair(state, PickingCharacterMode(state, hasTurn))
        }
    }
}

class YourTurnMode(var state : GameState) : IGameMode {
    var endTurn : Menu<Boolean>? = null
    var elapsed = 0.0
    var submode : IGameSubmode = PickingCharacterMode(state, getHasTurn(state.logical.characters))
    var clickAnims : List<ClickAnim> = listOf()

    fun updateAnims(t : Double) {
        val empty : List<ClickAnim> = emptyList()
        clickAnims = empty.plus(clickAnims.mapNotNull { kv -> kv.update(t) })
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
        submode.update(t)
        updateAnims(t)
        moveCharactersCloserToTargets(state, t)
        if (getHasTurn(state.logical.characters).count() < 1) {
            return YourTurnIntroMode(state)
        } else {
            return this
        }
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

        val res = submode.click(x, y)
        state = res.first
        submode = res.second
    }

    override fun overlay(ctx : CanvasRenderingContext2D) {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)

        submode.overlay(ctx)

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
    }

    override fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
        submode.underlay(dim, ctx)
    }
}
