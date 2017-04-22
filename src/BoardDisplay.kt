/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasImageSource
import org.w3c.dom.CanvasRenderingContext2D

data class BoardDim(val boardLeft : Double, val boardTop : Double, val boardWidth : Double, val boardHeight : Double, val tileSize : Double) {
}

fun placeSprite(assets : Assets, dim : BoardDim, ctx : CanvasRenderingContext2D, spriteId : Int, x : Int, y : Int) {
    var imageSource : CanvasImageSource = assets.sprites.asDynamic()
    var spx = spriteId % 200
    var spy = spriteId / 200
    ctx.drawImage(imageSource, spx * 50.0, spy * 50.0, 50.0, 50.0, dim.boardLeft + x * dim.tileSize + 1, dim.boardTop + y * dim.tileSize + 1, dim.tileSize - 2.0, dim.tileSize - 2.0)
}

fun getBoardSize(screenx : Int, screeny : Int, board : GameBoard) : BoardDim {
    val height80Pct = screeny.toDouble() * 0.8
    val width80Pct = screenx.toDouble() * 0.8
    val tileWidthMax = width80Pct / board.dimX
    val tileHeightMax = height80Pct / board.dimY
    val tileSize = Math.min(tileWidthMax, tileHeightMax)
    val boardHeight = board.dimY * tileSize
    val boardWidth = board.dimX * tileSize
    val boardTop = (screeny - boardHeight) / 2
    val boardLeft = (screenx - boardWidth) / 2
    return BoardDim(boardLeft, boardTop, boardWidth, boardHeight, tileSize)
}

val roomColors =
        mutableMapOf(
                Pair(SquareAssoc.ENGINEERING, "rgba(192,86,85,0.3)"),
                Pair(SquareAssoc.BRIDGE, "rgba(206, 183, 53, 0.3)"),
                Pair(SquareAssoc.LIFE_SUPPORT, "rgba(206, 183, 53, 0.3)"),
                Pair(SquareAssoc.MEDICAL, "rgba(65, 100, 160, 0.3)"),
                Pair(SquareAssoc.SECURITY, "rgba(112, 68, 70, 0.3)")
        )

fun drawBoard(screenx : Int, screeny : Int, ctx : CanvasRenderingContext2D, state : GameState, assets : Assets) {
    var board = state.logical.board
    var chars = state.logical.characters
    val dim = getBoardSize(screenx, screeny, board)
    // Render world
    ctx.fillStyle = "black"
    ctx.fillRect(dim.boardLeft, dim.boardTop, dim.boardWidth, dim.boardHeight)
    for (i in 0..(board.dimY - 1)) {
        for (j in 0..(board.dimX - 1)) {
            val idx = (i * board.dimX) + j
            val door = board.doors.get(idx)
            if (board.square[idx].role == SquareRole.WALL) {
                ctx.fillStyle = "#2a2b2d"
                ctx.fillRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize - 2.0, dim.tileSize - 2.0)
            } else {
                ctx.fillStyle = "#bcc4d1"
                ctx.fillRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize - 2.0, dim.tileSize - 2.0)
            }
            val roomColor = roomColors.get(board.square[idx].assoc)
            if (roomColor != null) {
                ctx.fillStyle = roomColor
                ctx.fillRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize - 2.0, dim.tileSize - 2.0)
            }
            // Render objects
            if (board.square[idx].role == SquareRole.HEALING_BED) {
                placeSprite(assets, dim, ctx, 1, j, i)
            } else if (door != null) {
                ctx.fillStyle = "#e5e5e5"
                ctx.strokeStyle = "black"
                if (door.vertical) {
                    ctx.fillRect(dim.boardLeft + ((j + 0.4) * dim.tileSize), dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize * 0.2, dim.tileSize - 2.0)
                    ctx.strokeRect(dim.boardLeft + ((j + 0.4) * dim.tileSize), dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize * 0.2, dim.tileSize - 2.0)
                } else {
                    ctx.fillRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + ((i + 0.4) * dim.tileSize), dim.tileSize - 2.0, dim.tileSize * 0.2)
                    ctx.strokeRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + ((i + 0.4) * dim.tileSize), dim.tileSize - 2.0, dim.tileSize * 0.2)
                }
            }
        }
    }
    // Render people
    for (p in chars) {
        placeSprite(assets, dim, ctx, 0, p.value.x, p.value.y)
    }
    // Selection
    val sel = state.sel
    if (sel != null) {
        val x = sel.first
        val y = sel.second
        ctx.strokeStyle = "yellow"
        ctx.strokeRect(dim.boardLeft + (x * dim.tileSize), dim.boardTop + (y * dim.tileSize), dim.tileSize, dim.tileSize)
    }
    // Timer
    ctx.fillStyle = "black"
    ctx.fillText("Time: " + lastTime, 0.0, 12.0, screenx.toDouble())
}