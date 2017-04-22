/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

fun drawBoard(screenx : Int, screeny : Int, ctx : CanvasRenderingContext2D, board : GameBoard) {
    val height80Pct = screeny.toDouble() * 0.8
    val width80Pct = screenx.toDouble() * 0.8
    val tileWidthMax = width80Pct / board.dimX
    val tileHeightMax = height80Pct / board.dimY
    val tileSize = Math.min(tileWidthMax, tileHeightMax)
    val boardHeight = board.dimY * tileSize
    val boardWidth = board.dimX * tileSize
    val boardTop = (screeny - boardHeight) / 2
    val boardLeft = (screenx - boardWidth) / 2
    console.log("boardLeft",boardLeft,"boardTop",boardTop,"boardWidth",boardWidth,"boardHeight",boardHeight)
    ctx.fillStyle = "black"
    ctx.fillRect(boardLeft, boardTop, boardWidth, boardHeight)
    for (i in 0..(board.dimY - 1)) {
        for (j in 0..(board.dimX - 1)) {
            val idx = (i * board.dimX) + j
            if (board.square[idx].role == SquareRole.WALL) {
                ctx.fillStyle = "#2a2b2d"
                ctx.fillRect(boardLeft + (j * tileSize) + 1, boardTop + (i * tileSize) + 1, tileSize - 2, tileSize - 2)
            } else {
                ctx.fillStyle = "#bcc4d1"
                ctx.fillRect(boardLeft + (j * tileSize) + 1, boardTop + (i * tileSize) + 1, tileSize - 2, tileSize - 2)
            }
        }
    }
}