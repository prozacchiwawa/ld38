/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import java.util.*

fun getSquare(xdim : Int, board : ArrayList<Square>, x : Int, y : Int) : Square {
    val idx = (y * xdim) + x
    val square = board[idx]
    return square
}

fun simpleBoardConvert(vararg s : String) : GameBoard {
    console.log("simpleBoardConvert")
    val ydim = s.size
    var xdim = 0
    for (i in 0..(ydim - 1)) {
        var st = s[i]
        if (st.size > xdim) {
            xdim = st.size
        }
    }
    console.log("simpleBoardConvert 2")
    val boardContents = ArrayList<Square>()
    val commandChairs : MutableMap<SquareAssoc, Pair<Int, Int>> = mutableMapOf()
    val doors : MutableMap<Int, DoorState> = mutableMapOf()

    for (i in 0..(ydim - 1)) {
        for (j in 0..(xdim - 1)) {
            var ch = '#'
            val st = s[i]
            if (j < st.size) {
                ch = st[j]
            }
            val idx = (i * xdim) + j
            console.log("idx",idx)
            if (ch == '#') {
                boardContents.add(Square(SquareRole.WALL, SquareAssoc.NOASSOC, 0))
            } else if (ch == '|') {
                doors.put(idx, DoorState(j, i, DOOR_START_HP, DoorType.INTERIOR, true, false, false, false))
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            } else if (ch == '-') {
                doors.put(idx, DoorState(j, i, DOOR_START_HP, DoorType.INTERIOR, false, false, false, false))
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            } else if (ch == 'E') {
                commandChairs.put(SquareAssoc.ENGINEERING, Pair(j,i))
                boardContents.add(Square(SquareRole.COMMAND_SEAT, SquareAssoc.ENGINEERING, 0))
            } else {
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            }
        }
    }

    for (coord in commandChairs) {
        val nextPoint = ArrayList<Pair<Int,Int>>()
        nextPoint.add(coord.value)
        while (nextPoint.count() > 0) {
            val pt = nextPoint[0]
            var idx = (pt.second * xdim) + pt.first
            nextPoint.removeAt(0)
            val sq = getSquare(xdim, boardContents, pt.first, pt.second)
            val door = doors.get(idx)
            if (door != null && sq.role != SquareRole.WALL) {
                boardContents[idx] = boardContents[idx].copy(assoc = coord.key)
                nextPoint.add(Pair(pt.first - 1, pt.second))
                nextPoint.add(Pair(pt.first + 1, pt.second))
                nextPoint.add(Pair(pt.first, pt.second - 1))
                nextPoint.add(Pair(pt.first, pt.second + 1))
            }
        }
    }

    console.log("simpleBoardConvert ",xdim,",",ydim)
    return GameBoard(xdim, ydim, boardContents.toTypedArray(), doors)
}

var testBoard =
    simpleBoardConvert(
            "####################################",
            "#   #    #       #     E           #",
            "#   #    #       #                 #",
            "##-####-########-#                 #",
            "##               #                 #",
            "########-####### #############-#####",
            "#              |                 ###",
            "#              ##############-######",
            "#              #                   #",
            "#              #                   #",
            "####################################"
            )
