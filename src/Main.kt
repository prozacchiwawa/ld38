/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

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
}

//
// When it's my turn, I can click on things
// There's a simple state machine transitioned by click
//
// Nothing Selected ->
//   Non character selected -> describe window
//   Char selected -> Describe char window
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
//     Nothing selected
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
//     Nothing selected
//   else
//     End turn
//
// describe window -> Nothing selected
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
}

class YourTurnMode(var state : GameState) : IGameMode {
    val hasTurn : MutableSet<String> = getHasTurn(state.logical.characters)
    var elapsed = 0.0

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
        if (hasTurn.count() < 1) {
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
        if (xTile < 0 || yTile < 0 || xTile >= board.dimX || yTile >= board.dimY) {
            state.sel = null
        } else {
            state.sel = Pair(xTile.toInt(), yTile.toInt())
        }
    }

    override fun overlay(ctx : CanvasRenderingContext2D) {
        val board = state.logical.board
        val dim = getBoardSize(screenX, screenY, board)
        for (name in hasTurn) {
            val ch = state.logical.characters.get(name)
            if (ch != null) {
                var cycle = elapsed * 2.0
                var stage = elapsed - Math.floor(elapsed)
                var sprite = Math.floor(3.0 + (stage * 4.0))
                placeSprite(assets, dim, ctx, sprite, ch.x, ch.y)
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
                drawBoard(screenX, screenY, context, mode.getState(), assets)
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
