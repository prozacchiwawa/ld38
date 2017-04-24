/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import org.w3c.dom.CharacterData
import java.util.*

val normalRanks : ArrayList<String> =
        arrayListOf("crewman", "ensign", "chief")
val officerRanks : ArrayList<String> =
        arrayListOf("lt.", "lt.cmdr.", "cmdr.")

val names : ArrayList<String> =
    arrayListOf(
    "Sophia", 	"Aiden",
    "Emma", 	"Jackson",
    "Olivia", 	"Ethan",
    "Isabella", 	"Liam",
    "Ava", 	"Mason",
    "Lily", 	"Noah",
    "Zoe", 	"Lucas",
    "Chloe", 	"Jacob",
    "Mia", 	"Jayden",
    "Madison", 	"Jack",
    "Emily", 	"Logan",
    "Ella", 	"Ryan",
    "Madelyn", 	"Caleb",
    "Abigail", 	"Benjamin",
    "Aubrey", 	"William",
    "Addison", 	"Michael",
    "Avery", 	"Alexander",
    "Layla", 	"Elijah",
    "Hailey", 	"Matthew",
    "Amelia", 	"Dylan",
    "Hannah", 	"James",
    "Charlotte", 	"Owen",
    "Kaitlyn", 	"Connor",
    "Harper", 	"Brayden",
    "Kaylee", 	"Carter",
    "Sophie", 	"Landon",
    "Mackenzie", 	"Joshua",
    "Peyton", 	"Luke",
    "Riley", 	"Daniel",
    "Grace", 	"Gabriel",
    "Brooklyn", 	"Nicholas",
    "Sarah", 	"Nathan",
    "Aaliyah", 	"Oliver",
    "Anna", 	"Henry",
    "Arianna", 	"Andrew",
    "Ellie", 	"Gavin",
    "Natalie", 	"Cameron",
    "Isabelle", 	"Eli",
    "Lillian", 	"Max",
    "Evelyn", 	"Isaac",
    "Elizabeth", 	"Evan",
    "Lyla", 	"Samuel",
    "Lucy", 	"Grayson",
    "Claire", 	"Tyler",
    "Makayla", 	"Zachary",
    "Kylie", 	"Wyatt",
    "Audrey", 	"Joseph",
    "Maya", 	"Charlie",
    "Leah", 	"Hunter",
    "Gabriella", 	"David"
    )

fun getSquare(xdim : Int, board : ArrayList<Square>, x : Int, y : Int) : Square {
    val idx = (y * xdim) + x
    val square = board[idx]
    return square
}

fun charClassFromAssoc(assoc: SquareAssoc) : CharClass {
    when (assoc) {
        SquareAssoc.ENGINEERING ->
                return CharClass.ENGINEER
        SquareAssoc.LIFE_SUPPORT ->
                return CharClass.LIFESUPPORT
        SquareAssoc.BRIDGE ->
                return CharClass.OFFICER
        SquareAssoc.MEDICAL ->
                return CharClass.DOCTOR
    }
    return CharClass.SECURITY
}

fun rollName(square : Square) : String {
    val nameNumber = (names.size * rand()).toInt()
    val name = names[nameNumber]
    val rankNumber = (3 * rand()).toInt()
    var rank = normalRanks[rankNumber]
    if (square.assoc == SquareAssoc.BRIDGE) {
        rank = officerRanks[rankNumber]
    }
    return rank + " " + name
}

fun simpleBoardConvert(vararg s : String) : GameState {
    val ydim = s.size
    var xdim = 0
    for (i in 0..(ydim - 1)) {
        var st = s[i]
        if (st.size > xdim) {
            xdim = st.size
        }
    }
    val boardContents = ArrayList<Square>()
    val commandChairs : MutableMap<SquareAssoc, Pair<Int, Int>> = mutableMapOf()
    val doors : MutableMap<Ord, DoorState> = mutableMapOf()
    val spawns : MutableSet<Ord> = mutableSetOf()

    for (i in 0..(ydim - 1)) {
        for (j in 0..(xdim - 1)) {
            var ch = '#'
            val st = s[i]
            if (j < st.size) {
                ch = st[j]
            }
            val idx = (i * xdim) + j
            if (ch == '#') {
                boardContents.add(Square(SquareRole.WALL, SquareAssoc.NOASSOC, 0))
            } else if (ch == '|') {
                doors.put(Ord(idx), DoorState(j, i, DOOR_START_HP, DoorType.INTERIOR, true, false, false, false))
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            } else if (ch == '-') {
                doors.put(Ord(idx), DoorState(j, i, DOOR_START_HP, DoorType.INTERIOR, false, false, false, false))
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            } else if (ch == 'E') {
                commandChairs.put(SquareAssoc.ENGINEERING, Pair(j, i))
                boardContents.add(Square(SquareRole.COMMAND_SEAT, SquareAssoc.ENGINEERING, 0))
            } else if (ch == 'B') {
                commandChairs.put(SquareAssoc.BRIDGE, Pair(j, i))
                boardContents.add(Square(SquareRole.COMMAND_SEAT, SquareAssoc.BRIDGE, 0))
            } else if (ch == 'M') {
                commandChairs.put(SquareAssoc.MEDICAL, Pair(j, i))
                boardContents.add(Square(SquareRole.COMMAND_SEAT, SquareAssoc.MEDICAL, 0))
            } else if (ch == 'L') {
                commandChairs.put(SquareAssoc.LIFE_SUPPORT, Pair(j, i))
                boardContents.add(Square(SquareRole.COMMAND_SEAT, SquareAssoc.LIFE_SUPPORT, 0))
            } else if (ch == 'S') {
                commandChairs.put(SquareAssoc.SECURITY, Pair(j, i))
                boardContents.add(Square(SquareRole.COMMAND_SEAT, SquareAssoc.SECURITY, 0))
            } else if (ch == 'e') {
                boardContents.add(Square(SquareRole.WORK_STATION, SquareAssoc.ENGINEERING, 0))
            } else if (ch == 'b') {
                boardContents.add(Square(SquareRole.WORK_STATION, SquareAssoc.BRIDGE, 0))
            } else if (ch == 'm') {
                boardContents.add(Square(SquareRole.WORK_STATION, SquareAssoc.MEDICAL, 0))
            } else if (ch == 'l') {
                boardContents.add(Square(SquareRole.WORK_STATION, SquareAssoc.LIFE_SUPPORT, 0))
            } else if (ch == 's') {
                boardContents.add(Square(SquareRole.WORK_STATION, SquareAssoc.SECURITY, 0))
            } else if (ch == '=') {
                boardContents.add(Square(SquareRole.HEALING_BED, SquareAssoc.MEDICAL, 0))
            } else if (ch == 'X') {
                spawns.add(Ord(idx))
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            } else {
                boardContents.add(Square(SquareRole.NOROLE, SquareAssoc.NOASSOC, 0))
            }
        }
    }

    for (coord in commandChairs) {
        val visited : MutableSet<Pair<Int,Int>> = mutableSetOf()
        val nextPoint : MutableSet<Pair<Int,Int>> = mutableSetOf()
        val gonext =
                { pt : Pair<Int,Int> ->
                    if (!visited.contains(pt)) {
                        visited.add(pt)
                        nextPoint.add(pt)
                    }
                }
        gonext(coord.value)
        while (!nextPoint.isEmpty()) {
            val pt = nextPoint.first()
            nextPoint.remove(pt)
            var idx = (pt.second * xdim) + pt.first
            val sq = getSquare(xdim, boardContents, pt.first, pt.second)
            val door = doors.get(Ord(idx))
            if (door == null && sq.role != SquareRole.WALL && sq.role != SquareRole.WORK_STATION) {
                boardContents[idx] = boardContents[idx].copy(assoc = coord.key)
                gonext(Pair(pt.first - 1, pt.second))
                gonext(Pair(pt.first + 1, pt.second))
                gonext(Pair(pt.first, pt.second - 1))
                gonext(Pair(pt.first, pt.second + 1))
            }
        }
    }

    val characters : MutableMap<String,Character> = mutableMapOf()
    val chosenNames = ArrayList<String>()
    for (ord in spawns) {
        var i = ord.idx / xdim
        var j = ord.idx % xdim
        val square = boardContents[ord.idx]
        val charClass = charClassFromAssoc(square.assoc)
        var fullName = rollName(square)
        while (characters.contains(fullName)) {
            fullName = rollName(square)
        }
        chosenNames.add(fullName)
        characters.put(fullName, Character(fullName, fullName, j, i, charClass, -1, CHAR_START_HP, arrayListOf()))
    }

    var assignedTeamLeads = 0
    while (assignedTeamLeads < 4) {
        // Select a character randomly to be the player char's starting faction member
        val whoAmI = chosenNames[Math.floor(rand() * chosenNames.size)]
        val myChar = characters.get(whoAmI)
        if (myChar != null && myChar.team == -1) {
            characters.put(whoAmI, myChar.copy(team = assignedTeamLeads++))
        }
    }

    val board = GameBoard(xdim, ydim, boardContents.toTypedArray(), doors)
    return GameState(GameStateData(characters, board))
}

fun testBoard() : GameState {
    return simpleBoardConvert(
            "######l#######s###############e#####",
            "#####  X # X    S#     E   X       #",
            "#####L   #    X  #     X           #",
            "#######-########-#   X        X    #",
            "##               #                 #",
            "########-####### #############-#####",
            "#=  X       X  |                 ###",
            "#=    X        ##############-######",
            "#=      X      #    X   X      X   #",
            "#  X   M       #           B       #",
            "###########m##################b#####"
    )
}