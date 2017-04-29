/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageBitmap
import org.w3c.dom.ImageData
import kotlin.js.Math

// My brother made the character art with a green vest to indicate team.
// the method we thought of to chroma key these was to detect pixel color by HLS value
// and rotate in HLS (or reduce saturation to 0 to indicate a neural, non-recruited NPC)
// and convert back to RGB.  This will allow us to palette swap the chars per team.

data class RGBA(val r : Double, val g : Double, val b : Double, val a : Double) { }
data class HLSA(val h : Double, val l : Double, val s : Double, val a : Double) { }

// Thanks: http://www.geekymonkey.com/Programming/CSharp/RGB2HSL_HSL2RGB.htm
fun rgbaToHLSA(rgba : RGBA) : HLSA {
    val r = rgba.r/255.0
    val g = rgba.g/255.0
    val b = rgba.b/255.0
    var v = 0.0
    var m = 0.0
    var vm = 0.0
    var r2 = 0.0
    var g2 = 0.0
    var b2 = 0.0

    var h = 0.0 // default to black
    var s = 0.0
    var l = 0.0

    v = Math.max(r,g)
    v = Math.max(v,b)
    m = Math.min(r,g)
    m = Math.min(m,b)
    l = (m + v) / 2.0

    if (l <= 0.0)
    {
        return HLSA(h,l,s,rgba.a)
    }

    vm = v - m;
    s = vm;
    if (s > 0.0)
    {
        var divisor = 2.0 - v - m
        if (l <= 0.5) {
            divisor = v + m
        }
        s /= divisor
    }
    else
    {
        return HLSA(h,l,s,rgba.a)
    }

    r2 = (v - r) / vm;
    g2 = (v - g) / vm;
    b2 = (v - b) / vm;

    if (r == v)
    {
        if (g == m) {
            h = 5.0 + b2
        } else {
            h = 1.0 - g2
        }
    }
    else if (g == v)
    {
        if (b == m) {
            h = 1.0 + r2
        } else {
            h = 3.0 - b2
        }
    }
    else
    {
        if (r == m) {
            h = 3.0 + g2
        } else {
            h = 5.0 - r2
        }
    }

    h /= 6.0

    return HLSA(h*Math.PI*2.0,l,s,rgba.a)
}

val GREEN_HUE = 120.0

val swaps = arrayOf(1, 4, 2, 3)

// Return an HTMLCanvasElement containing the palette swap
fun makePaletteSwap(i : org.w3c.dom.HTMLImageElement, sx : Int, sy : Int, tilesize : Int, findHue : Double, newHue : Int?) : HTMLCanvasElement {
    val canvas : HTMLCanvasElement = kotlin.browser.document.createElement("canvas").asDynamic()
    if (canvas == null) {
        throw Exception("no canvas")
    }
    canvas.width = tilesize
    canvas.height = tilesize
    val context : CanvasRenderingContext2D = canvas.getContext("2d").asDynamic()
    context.drawImage(i, sx.toDouble(), sy.toDouble(), tilesize.toDouble(), tilesize.toDouble(), 0.0, 0.0, tilesize.toDouble(), tilesize.toDouble())
    val imagedata = context.getImageData(0.0, 0.0, tilesize.toDouble(), tilesize.toDouble())
    for (i in 0..(tilesize - 1)) {
        for (j in 0..(tilesize - 1)) {
            val idx = 4 * (j + (i * tilesize))
            val rgba = RGBA(
                    imagedata.data[idx].toDouble(),
                    imagedata.data[idx+1].toDouble(),
                    imagedata.data[idx+2].toDouble(),
                    imagedata.data[idx+3].toDouble() / 255.0
            )
            val hlsa = rgbaToHLSA(rgba)
            if (hlsa.h / TO_RADIANS > findHue - 40.0 && hlsa.h / TO_RADIANS < findHue + 40.0) {
                if (newHue != null) {
                    val mask = swaps[newHue]
                    val p = imagedata.data[idx+1]
                    val o = imagedata.data[idx]
                    if (mask.and(1) != 0) {
                        imagedata.data[idx] = p
                    } else {
                        imagedata.data[idx] = o
                    }
                    if (mask.and(2) != 0) {
                        imagedata.data[idx + 1] = p
                    } else {
                        imagedata.data[idx + 1] = o
                    }
                    if (mask.and(4) != 0) {
                        imagedata.data[idx + 2] = p
                    } else {
                        imagedata.data[idx + 2] = o
                    }
                } else {
                    imagedata.data[idx] = imagedata.data[idx+1]
                    imagedata.data[idx + 1] = imagedata.data[idx+1]
                    imagedata.data[idx + 2] = imagedata.data[idx+1]
                }
            }
        }
    }
    context.putImageData(imagedata, 0.0, 0.0)
    return canvas
}