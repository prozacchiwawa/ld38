/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import java.util.*

val TILE_WALK_TIME = 0.3

var lastTime = getCurTime()

fun doError(container : org.w3c.dom.Element, content : org.w3c.dom.Element, t : String) {
    container.setAttribute("style", "top: 0")
    content.innerHTML = t;
}

var screenX : Int = kotlin.browser.window.innerWidth.toInt()
var screenY : Int = kotlin.browser.window.innerHeight.toInt()

fun getRenderContext() : org.w3c.dom.CanvasRenderingContext2D? {
    val canvasElt = kotlin.browser.document.getElementById("main")
    canvasElt?.setAttribute("width", screenX.toString())
    canvasElt?.setAttribute("height", screenY.toString())
    var context : org.w3c.dom.CanvasRenderingContext2D? = null
    when (canvasElt) {
        is HTMLCanvasElement ->
        {
            var ctx = canvasElt.getContext("2d")
            when (ctx) {
                is CanvasRenderingContext2D ->
                    context = ctx
            }
        }
    }
    return context
}

val rawWindow : dynamic = kotlin.browser.window
val error = kotlin.browser.window.document.getElementById("error")
val errorContent = kotlin.browser.window.document.getElementById("error-content")
var assets = Assets()

interface IGameMode {
    abstract fun runMode(t : Double) : IGameMode
    abstract fun getState() : GameState
    abstract fun click(x : Double, y : Double)
    abstract fun overlay(ctx : CanvasRenderingContext2D)
    abstract fun underlay(boardDim : BoardDim, ctx : CanvasRenderingContext2D)
}

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

class YourTurnIntroMode(var state : GameState) : IGameMode {
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

    override fun click(x : Double, y : Double) {
        elapsed = 5.0
    }

    override fun overlay(ctx : CanvasRenderingContext2D) {
        var alpha = 0.5
        if (elapsed > 4.0) {
            alpha = 0.5 * (5.0 - elapsed)
        } else if (elapsed < 1.0) {
            alpha = 0.5 * elapsed
        }
        ctx.fillStyle = "rgba(0,0,0,${alpha})"
        ctx.fillRect(0.0, 0.0, screenX.toDouble(), screenY.toDouble())
        ctx.font = "48px serif"
        val tmetrics = ctx.measureText("Your Move")
        val tleft = (screenX.toDouble() - tmetrics.width) / 2.0
        val ttop = (screenY - 48.0) / 2.0
        console.log(tleft,ttop)
        ctx.fillStyle = "rgba(255,255,255,1)"
        ctx.fillText("Your Move", tleft, ttop)
    }

    override fun underlay(boardDim : BoardDim, ctx : CanvasRenderingContext2D) {

    }
}

fun getMoves(board : GameBoard, moves : Int, ch : Character) : Map<Int,Int> {
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

data class AvailableMove(val who : Character, val haveMoves : Int, val moves : Map<Int, Int>) {
}

class YourTurnMode(var state : GameState) : IGameMode {
    val hasTurn : MutableSet<String> = getHasTurn(state.logical.characters)
    var elapsed = 0.0
    var moving : AvailableMove? = null
    var selected : Character? = null
    var menu : Menu<Pair<MenuCommandDirection, MenuCommandType>>? = null

    fun getHasTurn(chars : Map<String, Character>) : MutableSet<String> {
        val res : MutableSet<String> = mutableSetOf()
        for (kv in chars) {
            if (kv.value.team == 0) {
                res.add(kv.key)
            }
        }
        return res
    }

    fun getAvailMoves(chars : Map<String, Character>) : MutableMap<String, Int> {
        val result : MutableMap<String, Int> = mutableMapOf()
        for (kv in chars) {
            result.put(kv.key, kv.value.availMoves())
        }
        return result
    }

    override fun runMode(t : Double) : IGameMode {
        elapsed += t
        moveCharactersCloserToTargets(t)
        if (hasTurn.count() < 1) {
            return YourTurnIntroMode(state)
        } else {
            return this
        }
    }

    override fun getState() : GameState {
        return state
    }

    fun moveCharactersCloserToTargets(delta : Double) {
        val kvlist = state.display.characters
        for (kv in kvlist) {
            if (kv.value.dispx != kv.value.targetx || kv.value.dispy != kv.value.targety) {
                val path = state.pathfind(kv.value.targetx, kv.value.targety, kv.value.dispx, kv.value.dispy)
                if (path != null) {
                    if (path.size > 1) {
                        var goTo = path[1]
                        val prevAnim = kv.value.animation
                        var newX = kv.value.dispx
                        var newY = kv.value.dispy
                        var newAnim = CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.WALK)
                        if (goTo.first < kv.value.dispx) {
                            newX = Math.max(kv.value.targetx, newX - delta * (1.0 / TILE_WALK_TIME))
                            newAnim = CharacterAnim(CharacterDirection.WEST, CharacterAnimType.WALK)
                        } else if (goTo.first > kv.value.dispx) {
                            newX = Math.min(kv.value.targetx, newX + delta * (1.0 / TILE_WALK_TIME))
                            newAnim = CharacterAnim(CharacterDirection.EAST, CharacterAnimType.WALK)
                        } else if (goTo.second < kv.value.dispy) {
                            newY = Math.max(kv.value.targety, newY - delta * (1.0 / TILE_WALK_TIME))
                            newAnim = CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.WALK)
                        } else {
                            newY = Math.min(kv.value.targety, newY + delta * (1.0 / TILE_WALK_TIME))
                        }
                        var animStart = kv.value.animstart
                        if (prevAnim != newAnim) {
                            animStart = lastTime
                        }
                        state.display.characters.put(kv.key, kv.value.copy(animation = newAnim, animstart = animStart, dispx = newX, dispy = newY))
                    }
                }
            }
        }
    }

    fun canAttack(ch : Character) : Boolean {
        return true
    }

    enum class MenuCommandDirection {
        NONE, NORTH, SOUTH, EAST, WEST
    }

    enum class MenuCommandType {
        NOTHING, WAIT, MOVE, OPEN, CLOSE, ATTACK, SPECIAL, SUPER
    }

    fun menuCommandDirection(dir : CharacterDirection) : MenuCommandDirection {
        if (dir == CharacterDirection.EAST) { return MenuCommandDirection.EAST }
        else if (dir == CharacterDirection.WEST) { return MenuCommandDirection.WEST }
        else if (dir == CharacterDirection.NORTH) { return MenuCommandDirection.NORTH }
        else if (dir == CharacterDirection.SOUTH) { return MenuCommandDirection.SOUTH }
        else { return MenuCommandDirection.NONE }
    }

    override fun click(x : Double, y : Double) {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        val xTile = Math.floor((x - dim.boardLeft) / dim.tileSize)
        val yTile = Math.floor((y - dim.boardTop) / dim.tileSize)
        console.log("mouse click ",xTile,yTile)
        val csel = moving
        val m = menu
        if (csel != null) {
            val idx = xTile + (board.dimX * yTile)
            val move = csel.moves.get(idx)
            if (move != null) {
                val cdisp = state.display.characters.get(csel.who.id)
                if (cdisp != null) {
                    state.display.characters.put(csel.who.id, cdisp.copy(targetx = xTile.toDouble(), targety = yTile.toDouble()))
                    moving = null
                }
            } else if (xTile == csel.who.x && xTile == csel.who.y) {
                // Preserve moving
            } else {
                moving = null
                selected = null
                menu = null
            }
        } else if (m != null) {
            val sel = m.getSelection(x, y)
            val csel = selected
            if (sel != null && csel != null) {
                val cdisp = state.display.characters.get(csel.id)
                if (sel.second == MenuCommandType.MOVE) {
                    menu = null
                    var movesLeft = csel.availMoves()
                    moving = AvailableMove(csel, movesLeft, getMoves(board, movesLeft, csel))
                } else if (cdisp != null) {
                    moving = null
                    selected = null
                    hasTurn.remove(csel.id)
                    val newCharacters = state.logical.characters.plus(Pair(csel.id, csel.copy(x = cdisp.targetx.toInt(), y = cdisp.targety.toInt())))
                    state = GameState(logical = state.logical.copy(characters = newCharacters))
                } else {
                    menu = null
                    moving = null
                    selected = null
                }
            }
        } else {
            moving = null
            menu = null
            for (chName in hasTurn) {
                val dp = state.display.characters.get(chName)
                val ch = state.logical.characters.get(chName)
                if (dp != null && ch != null && Math.abs(dp.dispx - xTile) < 0.01 && Math.abs(dp.dispy - yTile) < 0.01) {
                    val near = Rect(dim.boardLeft + (dp.dispx * dim.tileSize), dim.boardTop + (dp.dispy * dim.tileSize), dim.tileSize, dim.tileSize)
                    selected = ch
                    val menuItems = arrayListOf(
                            Pair(ch.name, Pair(MenuCommandDirection.NONE, MenuCommandType.NOTHING)),
                            Pair("Wait", Pair(MenuCommandDirection.NONE, MenuCommandType.WAIT)),
                            Pair("Move", Pair(MenuCommandDirection.NONE, MenuCommandType.MOVE))
                    )
                    val canAttack = canAttack(ch)
                    for (door in state.getNearbyDoors(ch.x, ch.y)) {
                        var dir = state.directionByDiff(ch.x,ch.y,door.value.x,door.value.y)
                        if (door.value.locked) {
                            menuItems.add(Pair("Attack ${state.directionName(dir)}", Pair(menuCommandDirection(dir), MenuCommandType.ATTACK)))
                        } else if (door.value.open) {
                            menuItems.add(Pair("Close ${state.directionName(dir)} door", Pair(menuCommandDirection(dir), MenuCommandType.CLOSE)))
                        } else {
                            menuItems.add(Pair("Open ${state.directionName(dir)} door", Pair(menuCommandDirection(dir), MenuCommandType.OPEN)))
                        }
                    }
                    menu = Menu(menuItems, 20.0, 5.0, near)
                }
            }
        }
    }

    override fun overlay(ctx : CanvasRenderingContext2D) {
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
        var m = menu
        if (m != null) {
            m.render(ctx)
        }
    }

    override fun underlay(dim : BoardDim, ctx : CanvasRenderingContext2D) {
        var sel = moving
        if (sel != null) {
            for (kv in sel.moves) {
                val y = kv.key / state.logical.board.dimX
                val x = kv.key % state.logical.board.dimX
                ctx.fillStyle = "rgba(247, 245, 178, 0.3)"
                ctx.fillRect(dim.boardLeft + (x * dim.tileSize), dim.boardTop + (y * dim.tileSize), dim.tileSize, dim.tileSize)
                ctx.strokeStyle = "rgba(247, 245, 178, 0.6)"
                ctx.strokeRect(dim.boardLeft + (x * dim.tileSize), dim.boardTop + (y * dim.tileSize), dim.tileSize , dim.tileSize)
            }
        }
    }
}

fun doWithException(doit : () -> Unit) {
    try {
        doit()
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}

class GameAnimator(var mode : IGameMode) {
    fun getMode() : IGameMode { return mode }
    fun runFrame() {
        doWithException {
            val t : Double = getCurTime();
            val delta = t - lastTime
            lastTime = t
            mode = mode.runMode(delta)
            kotlin.browser.window.requestAnimationFrame { runFrame() }
            val context = getRenderContext()
            if (context != null) {
                drawBoard(screenX, screenY, context, mode.getState(), assets, { dim -> mode.underlay(dim, context) })
                mode.overlay(context)
            } else {
                throw Exception("No canvas named main")
            }
        }
    }

    fun start() {
        doWithException {
            kotlin.browser.window.requestAnimationFrame { runFrame() }
        }
    }
}

fun rungame() {
    doWithException {
        fun onResize(evt : dynamic) {
            screenX = kotlin.browser.window.innerWidth.toInt()
            screenY = kotlin.browser.window.innerHeight.toInt()
        }

        kotlin.browser.window.addEventListener("resize", { evt -> onResize(evt); })

        var running = testBoard
        val ga = GameAnimator(YourTurnIntroMode(running))

        kotlin.browser.window.addEventListener("click", { evt : dynamic ->
            ga.getMode().click(evt.clientX, evt.clientY)
        })

        ga.start()
    }
}

fun main(args: Array<String>) {
    assets.addLoadListener { rungame() }
    assets.start()
}
