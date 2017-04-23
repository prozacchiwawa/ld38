/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasImageSource
import org.w3c.dom.CanvasRenderingContext2D

val TILESIZE = 128.0
val FLOOR_SPRITE_CORNER = 26
val FLOOR_SPRITE_EDGE = 27
val FLOOR_SPRITE = 28
val WALL_CORNER = 24
val WALL_LONG = 25

val TO_RADIANS = Math.PI/180

data class BoardDim(val boardLeft : Double, val boardTop : Double, val boardWidth : Double, val boardHeight : Double, val tileSize : Double) {
}

fun placeSprite(assets : Assets, dim : BoardDim, ctx : CanvasRenderingContext2D, spriteId : Int, x : Double, y : Double) {
    var imageSource : CanvasImageSource = assets.sprites.asDynamic()
    var spx = spriteId % 20
    var spy = spriteId / 20
    ctx.drawImage(imageSource, spx * TILESIZE, spy * TILESIZE, TILESIZE, TILESIZE, dim.boardLeft + x * dim.tileSize, dim.boardTop + y * dim.tileSize, dim.tileSize, dim.tileSize)
}

fun placeSpriteRotated(assets : Assets, dim : BoardDim, ctx : CanvasRenderingContext2D, spriteId : Int, x : Double, y : Double, angle : Double) {
    var imageSource : CanvasImageSource = assets.sprites.asDynamic()
    var spx = spriteId % 20
    var spy = spriteId / 20
    ctx.save()
    ctx.translate(dim.boardLeft + x * dim.tileSize + dim.tileSize / 2.0, dim.boardTop + y * dim.tileSize + dim.tileSize / 2.0)
    ctx.rotate(angle * TO_RADIANS)
    ctx.drawImage(imageSource, spx * TILESIZE, spy * TILESIZE, TILESIZE, TILESIZE, -(dim.tileSize / 2.0), -(dim.tileSize / 2.0), dim.tileSize, dim.tileSize)
    ctx.restore()
}

fun getBoardSize(screenx : Int, screeny : Int, board : GameBoard) : BoardDim {
    val height80Pct = screeny.toDouble() * 0.8
    val width80Pct = screenx.toDouble() * 0.8
    val tileWidthMax = width80Pct / board.dimX
    val tileHeightMax = height80Pct / board.dimY
    val tileSize : Double = Math.min(tileWidthMax, tileHeightMax)
    val boardHeight = board.dimY.toDouble() * tileSize
    val boardWidth = board.dimX.toDouble() * tileSize
    val boardTop = (screeny.toDouble() - boardHeight) / 2.0
    val boardLeft = (screenx.toDouble() - boardWidth) / 2.0
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

val wallSchemes = mapOf(
        Pair(0, arrayOf()),
        Pair(1, arrayOf()),
        Pair(2, arrayOf()),
        Pair(3, arrayOf(Pair(WALL_CORNER, 90.0))),
        Pair(4, arrayOf()),
        Pair(5, arrayOf(Pair(WALL_LONG, 0.0))),
        Pair(6, arrayOf(Pair(WALL_CORNER, 180.0))),
        Pair(7, arrayOf(Pair(WALL_CORNER, 180.0), Pair(WALL_LONG, 0.0))),
        Pair(8, arrayOf()),
        Pair(9, arrayOf(Pair(WALL_CORNER, 0.0))),
        Pair(10, arrayOf(Pair(WALL_LONG, 90.0))),
        Pair(11, arrayOf(Pair(WALL_CORNER, 90.0), Pair(WALL_LONG, 90.0))),
        Pair(12, arrayOf(Pair(WALL_CORNER, -90.0))),
        Pair(13, arrayOf(Pair(WALL_CORNER, -90.0), Pair(WALL_LONG, 0))),
        Pair(14, arrayOf(Pair(WALL_CORNER, -90.0), Pair(WALL_LONG, 90.0))),
        Pair(15, arrayOf(Pair(WALL_LONG, 0), Pair(WALL_LONG, 90.0)))
)

fun drawBoard(screenx : Int, screeny : Int, ctx : CanvasRenderingContext2D, state : GameState, assets : Assets, underlay : (BoardDim) -> Unit) {
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
            if (i == 0 && j == 0) {
                placeSpriteRotated(assets, dim, ctx, FLOOR_SPRITE_CORNER, 0.0, 0.0, -90.0)
            } else if (i == board.dimY - 1 && j == 0) {
                placeSpriteRotated(assets, dim, ctx, FLOOR_SPRITE_CORNER, j.toDouble(), i.toDouble(), 180.0)
            } else if (i == 0 && j == board.dimX - 1) {
                placeSprite(assets, dim, ctx, FLOOR_SPRITE_CORNER, j.toDouble(), 0.0)
            } else if (i == board.dimY - 1 && j == board.dimX - 1) {
                placeSpriteRotated(assets, dim, ctx, FLOOR_SPRITE_CORNER, j.toDouble(), i.toDouble(), 90.0)
            } else if (i == 0) {
                placeSprite(assets, dim, ctx, FLOOR_SPRITE_EDGE, j.toDouble(), 0.0)
            } else if (j == 0) {
                placeSpriteRotated(assets, dim, ctx, FLOOR_SPRITE_EDGE, 0.0, i.toDouble(), -90.0)
            } else if (i == board.dimY - 1) {
                placeSpriteRotated(assets, dim, ctx, FLOOR_SPRITE_EDGE, j.toDouble(), i.toDouble(), 180.0)
            } else if (j == board.dimX - 1) {
                placeSpriteRotated(assets, dim, ctx, FLOOR_SPRITE_EDGE, j.toDouble(), i.toDouble(), 90.0)
            } else if (i > 0 && j > 0 && i < board.dimY - 1 && j < board.dimX - 1) {
                placeSprite(assets, dim, ctx, FLOOR_SPRITE, j.toDouble(), i.toDouble())
            }
            if (board.square[idx].role == SquareRole.WALL) {
                val row = idx / board.dimX
                val col = idx % board.dimX
                val neighbors = board.getNeighborsWithDoors(col, row)
                val scheme = wallSchemes.get(neighbors)
                for (w in scheme) {
                    placeSpriteRotated(assets, dim, ctx, w.first, j.toDouble(), i.toDouble(), w.second)
                }
            }
            val roomColor = roomColors.get(board.square[idx].assoc)
            if (roomColor != null) {
                ctx.fillStyle = roomColor
                ctx.fillRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize - 2.0, dim.tileSize - 2.0)
            }
            // Render objects
            if (board.square[idx].role == SquareRole.HEALING_BED) {
                placeSprite(assets, dim, ctx, 1, j.toDouble(), i.toDouble())
            } else if (board.square[idx].role == SquareRole.COMMAND_SEAT) {
                placeSprite(assets, dim, ctx, 2, j.toDouble(), i.toDouble())
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

    // Underlay
    underlay(dim)

    // Render people
    for (disp in state.display.characters) {
        placeSprite(assets, dim, ctx, 0, disp.value.dispx, disp.value.dispy)
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
