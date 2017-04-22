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

fun main(args: Array<String>) {
    val rawWindow : dynamic = kotlin.browser.window
    val error = kotlin.browser.window.document.getElementById("error")
    val errorContent = kotlin.browser.window.document.getElementById("error-content")
    try {
        var state = GameStateData(mapOf(), testBoard)
        var running = GameState(state)

        fun onResize(evt : dynamic) {
            screenX = kotlin.browser.window.innerWidth.toInt()
            screenY = kotlin.browser.window.innerHeight.toInt()
            var lc = getRenderContext()
            if (lc != null) {
                drawBoard(screenX, screenY, lc, running.logical.board)
            } else {
                throw Exception("No canvas named main")
            }
        }

        kotlin.browser.window.addEventListener("resize", { evt -> onResize(evt); })

        val context = getRenderContext()
        if (context != null) {
            drawBoard(screenX, screenY, context, running.logical.board)
        } else {
            throw Exception("No canvas named main")
        }
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
