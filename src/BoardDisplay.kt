/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasRenderingContext2D

val roomColors =
        mutableMapOf(Pair(SquareAssoc.ENGINEERING, "rgba(192,86,85,0.3)"))

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
            val door = board.doors.get(idx)
            if (board.square[idx].role == SquareRole.WALL) {
                ctx.fillStyle = "#2a2b2d"
                ctx.fillRect(boardLeft + (j * tileSize) + 1, boardTop + (i * tileSize) + 1, tileSize - 2, tileSize - 2)
            } else {
                ctx.fillStyle = "#bcc4d1"
                ctx.fillRect(boardLeft + (j * tileSize) + 1, boardTop + (i * tileSize) + 1, tileSize - 2, tileSize - 2)
            }
            val roomColor = roomColors.get(board.square[idx].assoc)
            if (roomColor != null) {
                ctx.fillStyle = roomColor
                ctx.fillRect(boardLeft + (j * tileSize) + 1, boardTop + (i * tileSize) + 1, tileSize - 2, tileSize - 2)
            }
            if (door != null) {
                ctx.fillStyle = "#e5e5e5"
                ctx.strokeStyle = "black"
                if (door.vertical) {
                    ctx.fillRect(boardLeft + ((j + 0.4) * tileSize), boardTop + (i * tileSize) + 1, tileSize * 0.2, tileSize - 2)
                    ctx.strokeRect(boardLeft + ((j + 0.4) * tileSize), boardTop + (i * tileSize) + 1, tileSize * 0.2, tileSize - 2)
                } else {
                    ctx.fillRect(boardLeft + (j * tileSize) + 1, boardTop + ((i + 0.4) * tileSize), tileSize - 2, tileSize * 0.2)
                    ctx.strokeRect(boardLeft + (j * tileSize) + 1, boardTop + ((i + 0.4) * tileSize), tileSize - 2, tileSize * 0.2)
                }
            }
        }
    }
}