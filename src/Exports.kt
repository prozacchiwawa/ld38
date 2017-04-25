package ldjam.prozacchiwawa

import java.util.*

/**
 * Created by arty on 4/25/17.
 */

val boardCvt : (dynamic) -> GameBoard = { desc : dynamic ->
    val dimX : Int = desc.dimX
    val dimY : Int = desc.dimY
    val squares : Array<Square> = Array(dimX * dimY, { i ->
        Square(SquareRole.valueOf(desc.role), SquareAssoc.valueOf(desc.assoc), desc.team)
    })
    val doors : MutableMap<Ord, DoorState> = mutableMapOf()
    for (i in 0..(desc.doors.length - 1)) {
        val doorDesc = desc.doors[i]
        val door = DoorState(doorDesc.x, doorDesc.y, doorDesc.hp, DoorType.valueOf(doorDesc.type), doorDesc.vertical, doorDesc.open, doorDesc.locked, doorDesc.airlock)
        val ord = Ord(door.x + (door.y * dimX))
        doors[ord] = door
    }
    GameBoard(dimX, dimY, squares, doors)
}
val gameStateCvt : (dynamic, dynamic) -> GameState = { chars : dynamic, board : dynamic ->
    var chlen = chars.length
    val charMap : MutableMap<String, Character> = mutableMapOf()
    for (i in 0..(chlen-1)) {
        val chent = chars[i]
        val ch = Character(chent.id, chent.name, chent.x, chent.y, CharClass.valueOf(chent.charclass), chent.team, chent.health, arrayListOf())
        charMap[ch.id] = ch
    }
    GameState(GameStateData(charMap, boardCvt(board)))
}

fun createExports() : dynamic {
    val exports : dynamic = js("new Object()")
    exports.GameState = gameStateCvt
    exports.simpleBoardConvert = { s: Array<String> -> simpleBoardConvert(s) }
    exports.getCharList = { t: Int, s: GameState ->
        var r = js("[]")
        for (ch in s.logical.characters.values.filter { ch -> ch.team == t }) {
            var o = js("new Object()")
            o.name = ch.name
            o.team = ch.team
            o.health = ch.health
            o.x = ch.x
            o.y = ch.y
            r.push(o)
        }
        r
    }
    exports.showBoard = { s: GameState ->
        var res = ArrayList<String>()
        var chars = s.logical.characters.values.map { ch ->
            Pair(s.logical.board.ordOfCoords(ch.x, ch.y), ch.team)
        }.toMap()

        for (i in 0..(s.logical.board.dimY - 1)) {
            if (i != 0) {
                res.add("\n")
            }
            for (j in 0..(s.logical.board.dimX - 1)) {
                val ord = s.logical.board.ordOfCoords(j, i)
                val sq = s.logical.board.square[ord.idx]
                val door = s.logical.board.doors.get(ord)
                if (door != null) {
                    if (door.vertical) {
                        res.add("|")
                    } else {
                        res.add("-")
                    }
                } else {
                    val ch = chars.get(ord)
                    if (sq.role == SquareRole.COMMAND_SEAT) {
                        if (chars.containsKey(ord)) {
                            res.add("@")
                        } else {
                            if (sq.assoc == SquareAssoc.BRIDGE) {
                                res.add("B")
                            } else if (sq.assoc == SquareAssoc.ENGINEERING) {
                                res.add("E")
                            } else if (sq.assoc == SquareAssoc.LIFE_SUPPORT) {
                                res.add("L")
                            } else if (sq.assoc == SquareAssoc.MEDICAL) {
                                res.add("M")
                            } else {
                                res.add("S")
                            }
                        }
                    } else if (sq.role == SquareRole.WORK_STATION) {
                        if (sq.assoc == SquareAssoc.BRIDGE) {
                            res.add("b")
                        } else if (sq.assoc == SquareAssoc.ENGINEERING) {
                            res.add("e")
                        } else if (sq.assoc == SquareAssoc.LIFE_SUPPORT) {
                            res.add("l")
                        } else if (sq.assoc == SquareAssoc.MEDICAL) {
                            res.add("m")
                        } else {
                            res.add("s")
                        }
                    } else if (sq.role == SquareRole.WALL) {
                        res.add("#")
                    } else if (ch != null) {
                        if (ch == -1) {
                            res.add("X")
                        } else {
                            res.add(ch.toString())
                        }
                    } else {
                        res.add(".")
                    }
                }
            }
        }
        res.joinToString("")
    }
    exports.execute = { state : GameState, input : String ->
        try {
            val split = input.split("@")
            if (split.size == 2) {
                val name = split[0].trim()
                val locAndCmd = split[1].trim().split(" ")
                val loc = locAndCmd[0].split(",")
                val action = CommandType.valueOf(locAndCmd[1])
                var dir : CharacterDirection? = null
                if (locAndCmd.size > 2) {
                    dir = CharacterDirection.valueOf(locAndCmd[2])
                }
                val locPair = Pair(parseInt(loc[0]), parseInt(loc[1]))
                val who = state.logical.characters.get(name)
                if (who != null) {
                    val moves = getMoves(state.logical.board, who)
                    var dirCoords = Pair(locPair.first, locPair.second - 1)
                    if (dir == CharacterDirection.SOUTH) {
                        dirCoords = Pair(locPair.first, locPair.second + 1)
                    } else if (dir == CharacterDirection.EAST) {
                        dirCoords = Pair(locPair.first + 1, locPair.second)
                    } else if (dir == CharacterDirection.WEST) {
                        dirCoords = Pair(locPair.first - 1, locPair.second)
                    }
                    if (!moves.contains(state.logical.board.ordOfCoords(locPair.first, locPair.second))) {
                        "Impossible move location"
                    } else if (action == CommandType.WAIT) {
                        state.executeCommand(who.copy(x = locPair.first, y = locPair.second), action, locPair.first, locPair.second)
                    } else {
                        state.executeCommand(who.copy(x = locPair.first, y = locPair.second), action, dirCoords.first, dirCoords.second)
                    }
                } else {
                    "No character named " + name
                }
            } else {
                "Format [name] @ x,y ACTION [DIR]"
            }
        } catch (e : Exception) {
            "Exception: " + e.toString()
        }
    }
    exports.enemyturn = { state : GameState, turn : Int ->
        ComputerTurnMode(turn, state).doTurn()
    }
    exports.doPostTurn = { state : GameState -> state.doPostTurn() }
    exports.setRandom = { r : () -> Double -> random = r }
    return exports
}

fun export(e : dynamic) {
    js("module.exports = e")
}
