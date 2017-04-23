/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

val textSpacingVert = 5.0
val menuBorderSize = 5.0

data class Rect(val left : Double, val top : Double, val width : Double, val height : Double) { }

data class Menu(val selections : Array<String>, val tall : Double, val sep : Double, val near : Rect) {
    var placed : Rect? = null

    fun getSelection(x : Double, y : Double) : Int? {
        val p = placed
        console.log(p)
        if (p != null) {
            if (x < p.left + menuBorderSize || x >= p.left + p.width + menuBorderSize) {
                return null
            }
            val step = Math.floor(y - menuBorderSize - p.top / (tall + textSpacingVert))
            if (step < 0 || step >= selections.size) {
                return null
            }
            val startOfStep = step * (tall + textSpacingVert)
            val where = step - startOfStep
            if (where > tall) {
                return null
            }
            return step
        } else {
            return null
        }
    }

    fun render(ctx : CanvasRenderingContext2D) {
        var width = 0.0
        val height = selections.size * tall + ((selections.size - 1) * textSpacingVert)
        ctx.font = "${tall}px serif"
        for (s in selections) {
            val metrics = ctx.measureText(s)
            width = Math.max(width, metrics.width)
        }
        var left = near.left - sep - 2.0 * menuBorderSize
        if (near.left + near.width + sep + width + 2.0 * menuBorderSize < screenX) {
            // Place on right
            left = near.left + near.width + sep
        }
        var top = near.top - menuBorderSize
        if (top + height + menuBorderSize > screenY) {
            top = screenY - height - menuBorderSize
        }

        val pl = Rect(left, top, width + 2.0 * menuBorderSize, height + 2.0 * menuBorderSize)
        placed = pl

        ctx.textBaseline = "top"
        ctx.fillStyle = "#3d6649"
        ctx.fillRect(pl.left, pl.top, pl.width, pl.height)
        ctx.strokeStyle = "white"
        ctx.strokeRect(pl.left + 2, pl.top + 2, pl.width - 4, pl.height - 4)
        var atY = pl.top + menuBorderSize
        for (s in selections) {
            ctx.fillStyle = "white"
            ctx.fillText(s, pl.left + menuBorderSize, atY)
            atY += tall + textSpacingVert
        }
    }
}