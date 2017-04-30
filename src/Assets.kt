/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.EventSource
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.events.EventListener

val TILESIZE = 128.0

class Assets {
    var sprites : org.w3c.dom.HTMLImageElement? = null
    var loaded : ArrayList<(() -> Unit)> = arrayListOf()
    // Key is 10000 * team + image#
    val paletteSwaps : MutableMap<Int, HTMLCanvasElement> = mutableMapOf()

    fun maybeTriggerLoaded(l : () -> Unit) : Boolean {
        val spr = sprites
        if (spr != null) {
            l()
            return true
        }
        return false
    }

    fun addLoadListener(l : () -> Unit) {
        if (!maybeTriggerLoaded(l)) {
            loaded.add(l)
        }
    }

    fun start() {
        var image : HTMLImageElement = js("new Image()")
        image.addEventListener("load", { _ ->
            sprites = image

            // Prepare palette swaps
            for (s in 40..119) {
                val tx = s % 20
                val ty = s / 20
                paletteSwaps.put(-10000 + s, makePaletteSwap(image, (tx * TILESIZE).toInt(), (ty * TILESIZE).toInt(), TILESIZE.toInt(), 120.0, null))
                paletteSwaps.put(s, makePaletteSwap(image, (tx * TILESIZE).toInt(), (ty * TILESIZE).toInt(), TILESIZE.toInt(), 120.0, 0))
                paletteSwaps.put(10000 + s, makePaletteSwap(image, (tx * TILESIZE).toInt(), (ty * TILESIZE).toInt(), TILESIZE.toInt(), 120.0, 1))
                paletteSwaps.put(20000 + s, makePaletteSwap(image, (tx * TILESIZE).toInt(), (ty * TILESIZE).toInt(), TILESIZE.toInt(), 120.0, 2))
                paletteSwaps.put(30000 + s, makePaletteSwap(image, (tx * TILESIZE).toInt(), (ty * TILESIZE).toInt(), TILESIZE.toInt(), 120.0, 3))
            }

            var llist = loaded
            loaded = arrayListOf()

            for (l in llist) {
                l()
            }
        })
        image.src = "./sprites.png"
    }
}