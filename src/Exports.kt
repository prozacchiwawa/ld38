package ldjam.prozacchiwawa

import java.util.*

/**
 * Created by arty on 4/25/17.
 */

fun createExports() : dynamic {
    val exports : dynamic = js("new Object()")
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
            Pair(s.logical.board.ordOfCoords(ch.x.toInt(), ch.y.toInt()), ch.team)
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
                val toward = locAndCmd[1].split(",")
                val action = CommandType.valueOf(locAndCmd[2])
                var dir : CharacterDirection? = null
                if (locAndCmd.size > 2) {
                    dir = CharacterDirection.valueOf(locAndCmd[3])
                }
                val locPair = Pair(parseInt(loc[0]), parseInt(loc[1]))
                val towardPair = Pair(parseInt(toward[0]), parseInt(toward[1]))
                val who = state.logical.characters.get(name)
                if (who != null) {
                    state.useCommand(who.id, Command(action, locPair, towardPair))
                } else {
                    "No character named " + name
                }
            } else {
                "Format: 'lt. name @ x,y x,y ACTION [DIR]'"
            }
        } catch (e : Exception) {
            "Exception: " + e.toString()
        }
    }
    exports.pathfind = {state : GameState, ax : Double, ay : Double, bx : Double, by : Double ->
        val p = state.logical.hints.pathfind(Pair(ax,ay), Pair(bx,by))
        console.log("p",p)
        if (p != null) {
            p.toTypedArray()
        } else {
            null
        }
    }
    exports.run = {state : GameState, t : Double ->
        state.run(t)
    }
    exports.setRandom = { r : () -> Double -> random = r }
    return exports
}

fun export(e : dynamic) {
    js("module.exports = e")
}
