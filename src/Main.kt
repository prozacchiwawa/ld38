/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

var lastTime = getCurTime()

fun doError(container : org.w3c.dom.Element, content : org.w3c.dom.Element, t : String) {
    container.setAttribute("style", "top: 0")
    content.innerHTML = t;
}

fun main(args: Array<String>) {
    val rawWindow : dynamic = kotlin.browser.window
    val error = kotlin.browser.window.document.getElementById("error")
    val errorContent = kotlin.browser.window.document.getElementById("error-content")
    try {
        fun onResize(evt : dynamic) {

        }

        kotlin.browser.window.addEventListener("resize", { evt -> onResize(evt); });
    } catch (e : Exception) {
        if (error != null && errorContent != null) {
            doError(error, errorContent, "${e}");
        }
    }
}
