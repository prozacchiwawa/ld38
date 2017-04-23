/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import java.util.*

val DOOR_START_HP = 50
val CHAR_START_HP = 30

public enum class CharClass {
    OFFICER, LIFESUPPORT, ENGINEER, DOCTOR, SECURITY
}

public enum class SquareRole {
    NOROLE, COMMAND_SEAT, HEALING_BED, WORK_STATION, WALL
}

public enum class SquareAssoc {
    NOASSOC, ENGINEERING, LIFE_SUPPORT, MEDICAL, SECURITY, BRIDGE, HALLWAY
}

public enum class CharacterDirection {
    NORTH, EAST, SOUTH, WEST
}

public enum class CharacterAnimType {
    IDLE, HURT, WALK, CRAWL, FIGHT, OPERATE
}

public enum class DoorType {
    INTERIOR, AIRLOCK
}

public data class Ord(val idx : Int) { }

public data class CharacterAnim(
        val dir : CharacterDirection,
        val type : CharacterAnimType
        ) {
}

public data class Character(
        val id : String,
        val name : String,
        val x : Int,
        val y : Int,
        val charclass : CharClass,
        val team : Int,
        val health : Int
        ) {

    fun availMoves() : Int {
        var moves = 3
        if (health < CHAR_START_HP * 0.3) {
            moves = 1
        } else if (health < CHAR_START_HP) {
            moves = 2
        }
        return moves
    }
}

public data class DoorState(
        val x : Int,
        val y : Int,
        val hp : Int,
        val type : DoorType,
        val vertical : Boolean,
        val open : Boolean,
        val locked : Boolean,
        val airlock : Boolean
        )

public data class Square(val role : SquareRole, val assoc : SquareAssoc, val team : Int) { }

public data class GameBoard(
        val dimX : Int,
        val dimY : Int,
        val square : Array<Square>,
        val doors : Map<Ord, DoorState>
        ) {
    fun isPassable(x : Int, y : Int) : Boolean {
        if (y < 0 || x < 0 || y >= dimY || x >= dimX) {
            return false
        } else {
            val idx = (y * dimX) + x
            val theSquare = square[idx]
            val theDoor = doors.get(Ord(idx))
            if (theDoor != null) {
                return theDoor.open
            } else {
                return theSquare.role != SquareRole.WALL
            }
        }
    }

    fun getNeighbor(x : Int, y : Int) : Square? {
        if (x < 0 || y < 0 || x >= dimX || y >= dimY) {
            return null
        } else {
            var idx = (y * dimX) + x
            return square[idx]
        }
    }

    fun ordOfCoords(x : Int, y : Int) : Ord { return Ord((y * dimX) + x) }
    fun coordsOfOrd(o : Ord) : Pair<Int,Int> { return Pair(o.idx % dimX, o.idx / dimX) }

    fun getNeighborsWithDoors(x : Int, y : Int) : Int {
        var res : Int = 0
        val leftNeighbor = getNeighbor(x - 1, y)
        val rightNeighbor = getNeighbor(x + 1, y)
        val upNeighbor = getNeighbor(x, y - 1)
        val downNeighbor = getNeighbor(x, y + 1)
        if ((leftNeighbor != null && leftNeighbor.role != SquareRole.NOROLE) || doors.containsKey(ordOfCoords(x - 1, y))) {
            res = res.or(1)
        }
        if ((rightNeighbor != null && rightNeighbor.role != SquareRole.NOROLE) || doors.containsKey(ordOfCoords(x + 1, y))) {
            res = res.or(4)
        }
        if ((upNeighbor != null && upNeighbor.role != SquareRole.NOROLE) || doors.containsKey(ordOfCoords(x, y - 1))) {
            res = res.or(2)
        }
        if ((downNeighbor != null && downNeighbor.role != SquareRole.NOROLE) || doors.containsKey(ordOfCoords(x, y + 1))) {
            res = res.or(8)
        }
        return res
    }
}

public data class CharacterDisplay(
        val dispx : Double,
        val dispy : Double,
        val targetx : Double,
        val targety : Double,
        val animation : CharacterAnim,
        val animstart : Double
        ) {
}

public data class GameStateData(
        val characters : Map<String, Character>,
        val board : GameBoard
        ) {
}

public data class DoorDisplayState(
        val vertical : Boolean,
        val type : DoorType,
        val towardState : Boolean,
        val lastState : Boolean,
        val currentOpen : Double,
        val startMoveTime : Double
        ) {
}

public data class SquareDisplay(
        val role : SquareRole,
        val assoc : SquareAssoc,
        val door : DoorDisplayState
        ) {
}

public class GameDisplay(logical: GameStateData) {
    var logical : GameStateData = logical;
    val characters : MutableMap<String, CharacterDisplay> =
            initDisplayFromState(logical);
    val board : Array<SquareDisplay> = initBoardFromState(logical);

    fun initDisplayFromState(logical : GameStateData) :
            MutableMap<String, CharacterDisplay> {
        val charMap : MutableMap<String, CharacterDisplay> = mutableMapOf();
        for (kv in logical.characters) {
            charMap[kv.key] =
                    CharacterDisplay(
                            kv.value.x.toDouble(),
                            kv.value.y.toDouble(),
                            kv.value.x.toDouble(),
                            kv.value.y.toDouble(),
                            CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.IDLE),
                            0.0
                    )
        }
        return charMap;
    }

    fun initBoardFromState(logical : GameStateData) : Array<SquareDisplay> {
        val boardDisp : Array<SquareDisplay> = arrayOf()
        val board = logical.board
        for (i in 0..(board.square.size - 1)) {
            val door = board.doors.get(Ord(i))
            if (door != null) {
                boardDisp[i] = SquareDisplay(SquareRole.NOROLE, SquareAssoc.HALLWAY, DoorDisplayState(door.vertical, door.type, false, false, 0.0, 0.0))
            }
        }
        return boardDisp
    }
}

public class GameState(logical : GameStateData) {
    val logical : GameStateData = logical;
    var display : GameDisplay = computeDisplay(logical)
    var sel : Pair<Int,Int>? = null

    fun computeDisplay(logical : GameStateData) : GameDisplay {
        return GameDisplay(logical);
    }

    data class PathComponent(val prev : PathComponent?, val me : Pair<Int, Int>) {}

    fun addIfPassable(targetX : Int, targetY : Int, first : PathComponent, visited : ArrayList<PathComponent>) {
        if (logical.board.isPassable(targetX, targetY)) {
            visited.add(PathComponent(first, Pair(targetX, targetY)))
        }
    }

    fun pathfind(fromX : Double, fromY : Double, toX : Double, toY : Double) : ArrayList<Pair<Int,Int>>? {
        val atX : Int = Math.round(fromX)
        val atY : Int = Math.round(fromY)
        val wantX : Int = Math.round(toX)
        val wantY : Int = Math.round(toY)
        val wantIdx = wantY * logical.board.dimX + wantX
        val visited : ArrayList<PathComponent> = arrayListOf()
        visited.add(PathComponent(null, Pair(atX, atY)))
        while (visited.count() > 0) {
            val first = visited[0]
            visited.removeAt(0)
            console.log("check",first)
            if (first.me.first == wantX && first.me.second == wantY) {
                val al : ArrayList<Pair<Int,Int>> = arrayListOf()
                var f : PathComponent? = first
                while (f != null) {
                    al.add(0, f.me)
                    f = f.prev
                }
                console.log("route", al)
                return al
            }
            addIfPassable(first.me.first - 1, first.me.second, first, visited)
            addIfPassable(first.me.first + 1, first.me.second, first, visited)
            addIfPassable(first.me.first, first.me.second - 1, first, visited)
            addIfPassable(first.me.first, first.me.second + 1, first, visited)
        }
        return null
    }

    fun executeCommand(ch : Character, cmd : CommandType, x : Int, y : Int) : GameState {
        val cdisp = display.characters.get(ch.id)
        if (cdisp != null) {
            val newChar = ch.copy(x = cdisp.targetx.toInt(), y = cdisp.targety.toInt())
            val logical = logical.copy(characters = logical.characters.plus(Pair(ch.id, newChar)))
            if (cmd == CommandType.OPEN) {
                val ord = logical.board.ordOfCoords(x, y)
                val door = logical.board.doors.get(ord)
                if (door != null) {
                    val newDoor = door.copy(open = true, locked = true)
                    return GameState(logical.copy(board = logical.board.copy(doors = logical.board.doors.plus(Pair(ord, newDoor)))))
                }
            } else if (cmd == CommandType.CLOSE) {
                val ord = logical.board.ordOfCoords(x, y)
                val door = logical.board.doors.get(ord)
                if (door != null) {
                    val newDoor = door.copy(open = false)
                    return GameState(logical.copy(board = logical.board.copy(doors = logical.board.doors.plus(Pair(ord, newDoor)))))
                }
            }
        }
        return this
    }
}
