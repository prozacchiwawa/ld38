/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import java.util.*

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
            } else {
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            }
        }
    }
    console.log("simpleBoardConvert ",xdim,",",ydim)
    return GameBoard(xdim, ydim, boardContents.toTypedArray(), mapOf())
}

var testBoard =
    simpleBoardConvert(
            "####################################",
            "#   #    #       #                 #",
            "#   #    #       #                 #",
            "## #### ######## #                 #",
            "##               ############# #####",
            "######## #######                   #",
            "#              ############## ######",
            "#              #                   #",
            "#              #                   #",
            "#              #                   #",
            "####################################"
            )
