/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import java.util.*

val TILE_WALK_TIME = 0.3

var lastTime = getCurTime()

fun doError(container : org.w3c.dom.Element, content : org.w3c.dom.Element, t : String) {
    container.setAttribute("style", "top: 0")
    content.innerHTML = t;
}

var screenX : Int = 0
var screenY : Int = 0

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

var error : Element? = null
var errorContent : Element? = null
var assets = Assets()

fun doWithException(doit : () -> Unit) {
    val err = error
    val errc = errorContent
    try {
        doit()
    } catch (e : Exception) {
        if (err != null && errc != null) {
            doError(err, errc, "${e}");
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

        var running = testBoard()
        val ga = GameAnimator(YourTurnIntroMode(running))

        kotlin.browser.window.addEventListener("click", { evt : dynamic ->
            ga.getMode().click(evt.clientX, evt.clientY)
        })

        ga.start()
    }
}

fun isNode() : Boolean {
    return js("typeof window === 'undefined'")
}

fun export(e : dynamic) {
    js("module.exports = e")
}

fun main(args: Array<String>) {
    if (!isNode()) {
        screenX = kotlin.browser.window.innerWidth.toInt()
        screenY = kotlin.browser.window.innerHeight.toInt()
        error = kotlin.browser.window.document.getElementById("error")
        errorContent = kotlin.browser.window.document.getElementById("error-content")
        assets.addLoadListener { rungame() }
        assets.start()
    } else {
        val exports : dynamic = js("new Object()")
        val boardCvt : (dynamic) -> GameBoard = { desc : dynamic ->
            val dimX : Int = desc.dimX
            val dimY : Int = desc.dimY
            val squares : Array<Square> = Array(dimX * dimY, { i ->
                Square(SquareRole.valueOf(desc.role), SquareAssoc.valueOf(desc.assoc), desc.team)
            })
            val doors : MutableMap<Ord, DoorState> = mutableMapOf()
            for (i in 0..(desc.doors.length - 1)) {
                val doorDesc = desc.doors[i]
                val door = DoorState(doorDesc.x, doorDesc.y, doorDesc.hp, DoorType.valueOf(doorDesc.type), doorDesc.vertical, doorDesc.open, doorDesc.locked, doorDesc.airlock)
                val ord = Ord(door.x + (door.y * dimX))
                doors[ord] = door
            }
            GameBoard(dimX, dimY, squares, doors)
        }
        val gameStateCvt : (dynamic, dynamic) -> GameState = { chars : dynamic,board : dynamic ->
            var chlen = chars.length
            val charMap : MutableMap<String, Character> = mutableMapOf()
            for (i in 0..(chlen-1)) {
                val chent = chars[i]
                val ch = Character(chent.id, chent.name, chent.x, chent.y, CharClass.valueOf(chent.charclass), chent.team, chent.health, arrayListOf())
                charMap[ch.id] = ch
            }
            GameState(GameStateData(charMap, boardCvt(board)))
        }
        exports.GameState = gameStateCvt
        export(exports)
    }
}
