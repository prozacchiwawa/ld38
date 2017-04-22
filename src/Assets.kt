/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.EventSource
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.events.EventListener
import java.util.*

class Assets {
    var sprites : org.w3c.dom.HTMLImageElement? = null
    var loaded : ArrayList<(() -> Unit)> = arrayListOf()

    fun maybeTriggerLoaded(l : () -> Unit) : Boolean {
        if (sprites != null) {
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
        image.addEventListener("load", { evt ->
            sprites = image

            console.log("sprites",sprites)

            var llist = loaded
            loaded = arrayListOf()

            for (l in llist) {
                l()
            }
        })
        image.src = "./sprites.png"
    }
}