/**
 * Created by arty on 4/22/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CanvasImageSource
import org.w3c.dom.CanvasRenderingContext2D

val TILESIZE = 128.0
val CHICKEN_SPRITE = 0
val BED_SPRITE = 1
val COMMAND_SPRITE = 2
val DOOR_CLOSED_SPRITE = 20
val DOOR_OPEN_SPRITE = 23
val FLOOR_SPRITE_CORNER = 26
val FLOOR_SPRITE_EDGE = 27
val FLOOR_SPRITE = 28
val WALL_CORNER = 24
val WALL_LONG = 25

val TO_RADIANS = Math.PI/180

data class AnimationDesc(val start : Int, val end : Int, val time : Double) { }

data class BoardDim(val boardLeft : Double, val boardTop : Double, val boardWidth : Double, val boardHeight : Double, val tileSize : Double) {
}

val charAnimations =
        mapOf(
                Pair(CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.IDLE), AnimationDesc(40, 42, 1.0)),
                Pair(CharacterAnim(CharacterDirection.WEST, CharacterAnimType.IDLE), AnimationDesc(50, 52, 1.0)),
                Pair(CharacterAnim(CharacterDirection.EAST, CharacterAnimType.IDLE), AnimationDesc(60, 62, 1.0)),
                Pair(CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.IDLE), AnimationDesc(70, 72, 1.0)),
                Pair(CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.WALK), AnimationDesc(40, 50, 0.3)),
                Pair(CharacterAnim(CharacterDirection.WEST, CharacterAnimType.WALK), AnimationDesc(50, 60, 0.3)),
                Pair(CharacterAnim(CharacterDirection.EAST, CharacterAnimType.WALK), AnimationDesc(60, 70, 0.3)),
                Pair(CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.WALK), AnimationDesc(70, 80, 0.3)),
                Pair(CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.CRAWL), AnimationDesc(40, 50, 0.7)),
                Pair(CharacterAnim(CharacterDirection.WEST, CharacterAnimType.CRAWL), AnimationDesc(50, 60, 0.7)),
                Pair(CharacterAnim(CharacterDirection.EAST, CharacterAnimType.CRAWL), AnimationDesc(60, 70, 0.7)),
                Pair(CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.CRAWL), AnimationDesc(70, 80, 0.7)),
                Pair(CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.IDLE), AnimationDesc(40, 42, 2.0)),
                Pair(CharacterAnim(CharacterDirection.WEST, CharacterAnimType.IDLE), AnimationDesc(50, 52, 2.0)),
                Pair(CharacterAnim(CharacterDirection.EAST, CharacterAnimType.IDLE), AnimationDesc(60, 62, 2.0)),
                Pair(CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.IDLE), AnimationDesc(70, 72, 2.0)),
                Pair(CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.FIGHT), AnimationDesc(80, 90, 0.3)),
                Pair(CharacterAnim(CharacterDirection.WEST, CharacterAnimType.FIGHT), AnimationDesc(90, 100, 0.3)),
                Pair(CharacterAnim(CharacterDirection.EAST, CharacterAnimType.FIGHT), AnimationDesc(100, 110, 0.3)),
                Pair(CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.FIGHT), AnimationDesc(110,120, 0.3)),
                Pair(CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.FIGHT), AnimationDesc(81, 83, 0.3)),
                Pair(CharacterAnim(CharacterDirection.WEST, CharacterAnimType.FIGHT), AnimationDesc(91, 93, 0.3)),
                Pair(CharacterAnim(CharacterDirection.EAST, CharacterAnimType.FIGHT), AnimationDesc(101, 103, 0.3)),
                Pair(CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.FIGHT), AnimationDesc(111, 113, 0.3))
        )

fun placeSprite(assets : Assets, dim : BoardDim, ctx : CanvasRenderingContext2D, spriteId : Int, x : Double, y : Double) {
    var imageSource : CanvasImageSource = assets.sprites.asDynamic()
    var spx = spriteId % 20
    var spy = spriteId / 20
    ctx.drawImage(imageSource, spx * TILESIZE, spy * TILESIZE, TILESIZE, TILESIZE, dim.boardLeft + x * dim.tileSize, dim.boardTop + y * dim.tileSize, dim.tileSize, dim.tileSize)
}

fun placeSpriteBigger(assets : Assets, dim : BoardDim, ctx : CanvasRenderingContext2D, spriteId : Int, x : Double, y : Double, scale : Double) {
    var imageSource : CanvasImageSource = assets.sprites.asDynamic()
    var spx = spriteId % 20
    var spy = spriteId / 20
    val originX = dim.boardLeft + x * dim.tileSize + (dim.tileSize / 2.0)
    val originY = dim.boardTop + y * dim.tileSize + (dim.tileSize / 2.0)
    val drawAtX = originX - (scale * dim.tileSize / 2.0)
    val drawAtY = originY - (scale * dim.tileSize / 2.0)
    ctx.drawImage(imageSource, spx * TILESIZE, spy * TILESIZE, TILESIZE, TILESIZE, drawAtX, drawAtY, dim.tileSize * scale, dim.tileSize * scale)
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
            val ord = board.ordOfCoords(j, i)
            val door = board.doors.get(ord)
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
            if (board.square[ord.idx].role == SquareRole.WALL) {
                val neighbors = board.getNeighborsWithDoors(j, i)
                val scheme = wallSchemes.get(neighbors)
                for (w in scheme) {
                    placeSpriteRotated(assets, dim, ctx, w.first, j.toDouble(), i.toDouble(), w.second)
                }
            }
            val roomColor = roomColors.get(board.square[ord.idx].assoc)
            if (roomColor != null) {
                ctx.fillStyle = roomColor
                ctx.fillRect(dim.boardLeft + (j * dim.tileSize) + 1, dim.boardTop + (i * dim.tileSize) + 1, dim.tileSize - 2.0, dim.tileSize - 2.0)
            }
            // Render objects
            if (board.square[ord.idx].role == SquareRole.HEALING_BED) {
                placeSprite(assets, dim, ctx, BED_SPRITE, j.toDouble(), i.toDouble())
            } else if (board.square[ord.idx].role == SquareRole.COMMAND_SEAT) {
                placeSprite(assets, dim, ctx, COMMAND_SPRITE, j.toDouble(), i.toDouble())
            } else if (door != null) {
                var doorSprite = DOOR_CLOSED_SPRITE
                if (door.open) {
                    doorSprite = DOOR_OPEN_SPRITE
                }
                var doorAngle = 0.0
                if (door.vertical) {
                    doorAngle = 90.0
                }
                placeSpriteRotated(assets, dim, ctx, doorSprite, j.toDouble(), i.toDouble(), doorAngle)
            }
        }
    }

    // Underlay
    underlay(dim)

    // Render people
    for (disp in state.display.characters) {
        val elapsed = lastTime - disp.value.animstart
        val animStart = charAnimations.get(disp.value.animation)
        if (animStart == null) {
            placeSprite(assets, dim, ctx, CHICKEN_SPRITE, disp.value.dispx, disp.value.dispy)
        } else {
            val whichCycle = Math.floor(elapsed / animStart.time)
            val frameFrac = (elapsed / animStart.time) - whichCycle
            val whichFrame = Math.floor(frameFrac * (animStart.end - animStart.start))
            placeSpriteBigger(assets, dim, ctx, animStart.start + whichFrame, disp.value.dispx, disp.value.dispy, 1.3)
        }
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
