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
        } else if (health < CHAR_START_HP * 0.75) {
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

public data class ClassStats(
        val attack : Double,
        val defense : Double
) {
}

val classStats = mapOf(
        Pair(CharClass.DOCTOR, ClassStats(1.0, 1.0)),
        Pair(CharClass.ENGINEER, ClassStats(3.0, 2.0)),
        Pair(CharClass.LIFESUPPORT, ClassStats(2.0, 2.0)),
        Pair(CharClass.SECURITY, ClassStats(3.0, 3.0)),
        Pair(CharClass.OFFICER, ClassStats(2.0, 2.0))
)

public data class CharacterDisplay(
        val dispx : Double,
        val dispy : Double,
        val targetx : Double,
        val targety : Double,
        val animation : CharacterAnim,
        val animstart : Double,
        val lastDamage : Double
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
                            0.0,
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

    fun isPassable(x : Int, y : Int) : Boolean {
        return logical.board.isPassable(x, y) && !(logical.characters.any { ch -> ch.value.x == x && ch.value.y == y })
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
            if (first.me.first == wantX && first.me.second == wantY) {
                val al : ArrayList<Pair<Int,Int>> = arrayListOf()
                var f : PathComponent? = first
                while (f != null) {
                    al.add(0, f.me)
                    f = f.prev
                }
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
            var health = ch.health
            val ord = logical.board.ordOfCoords(cdisp.dispx.toInt(),cdisp.dispy.toInt())
            if (logical.board.square[ord.idx].role == SquareRole.HEALING_BED) {
                health = CHAR_START_HP
            }
            val newChar = ch.copy(x = cdisp.dispx.toInt(), y = cdisp.dispy.toInt(), health = health)
            var logical = logical.copy(characters = logical.characters.plus(Pair(ch.id, newChar)))
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
            } else if (cmd == CommandType.ATTACK) {
                val stats = classStats.getOrElse(ch.charclass, { ClassStats(2.0, 2.0) })
                val ot = logical.characters.filter { kv -> kv.value.x == x && kv.value.y == y }.toList().first()
                if (ot != null) {
                    val targetStats = classStats.getOrElse(ot.second.charclass, { ClassStats(2.0, 2.0) })
                    val damage = (rand() * 5.0) + (rand() * 5.0 * (stats.attack / targetStats.defense))
                    val cdisp = display.characters.get(ch.id)
                    if (cdisp != null) {
                        display.characters.put(ch.id, cdisp.copy(lastDamage = damage, animation = CharacterAnim(cdisp.animation.dir, CharacterAnimType.FIGHT)))
                    }
                    val health = Math.max(ot.second.health - damage, 0.0).toInt()
                    val gs = GameState(logical.copy(characters = logical.characters.plus(Pair(ot.first, ot.second.copy(health = health)))))
                    gs.display = display
                    return gs
                } else {
                    val ord = logical.board.ordOfCoords(x, y)
                    val door = logical.board.doors.get(ord)
                    if (door != null) {
                        val damage = (rand() * 20.0)
                        val cdisp = display.characters.get(ch.id)
                        if (cdisp != null) {
                            display.characters.put(ch.id, cdisp.copy(lastDamage = damage, animation = CharacterAnim(cdisp.animation.dir, CharacterAnimType.FIGHT)))
                        }
                        val health = Math.max(door.hp - damage, 0.0).toInt()
                        var open = door.open
                        var locked = door.locked
                        if (health == 0) {
                            open = true
                            locked = false
                        }
                        val newDoor = door.copy(hp = health, open = open, locked = locked)
                        val gs = GameState(logical.copy(board = logical.board.copy(doors = logical.board.doors.plus(Pair(ord, door.copy(hp = health))))))
                        gs.display = display
                        return gs
                    }
                }
            }
            return GameState(logical)
        }
        return this
    }
}
