/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import java.util.*

val DOOR_START_HP = 50.0
val CHAR_START_HP = 30.0
val BASE_DPS = 10.0

fun distance(x : Double, y : Double, s : Double, t : Double) : Double {
    val dx = (x - s)
    val dy = (y - t)
    return Math.sqrt((dx * dx) + (dy * dy))
}

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

data class RoutedCommand(val hints : Hints, val ch : Pair<Int,Int>, val cmd : Command, val path : ArrayList<Pair<Int,Int>>? = hints.pathfind(Pair(ch.first.toDouble(),ch.second.toDouble()), Pair(cmd.at.first.toDouble(),cmd.at.second.toDouble()))) { }

public data class Character(
        val id : String,
        val name : String,
        val x : Double,
        val y : Double,
        val lastx : Double,
        val lasty : Double,
        val charclass : CharClass,
        val team : Int,
        val health : Double,
        val dir : CharacterDirection,
        val doing : RoutedCommand
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
        val hp : Double,
        val type : DoorType,
        val vertical : Boolean,
        val wantState : Boolean,
        val amtOpen : Double,
        val openTime : Double,
        val locked : Boolean
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
                return theDoor.amtOpen >= 0.75
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

    fun getNeighbors(x : Int, y : Int) : Int {
        var res : Int = 0
        val leftNeighbor = getNeighbor(x - 1, y)
        val rightNeighbor = getNeighbor(x + 1, y)
        val upNeighbor = getNeighbor(x, y - 1)
        val downNeighbor = getNeighbor(x, y + 1)
        if ((leftNeighbor != null && leftNeighbor.role == SquareRole.WALL)) {
            res = res.or(1)
        }
        if ((rightNeighbor != null && rightNeighbor.role == SquareRole.WALL)) {
            res = res.or(4)
        }
        if ((upNeighbor != null && upNeighbor.role == SquareRole.WALL)) {
            res = res.or(2)
        }
        if ((downNeighbor != null && downNeighbor.role == SquareRole.WALL)) {
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
        val animation : CharacterAnim,
        val animstart : Double,
        val lastDamage : Double
        ) {
}

enum class EquivType {
    OPEN_DOOR, CLOSED_DOOR, UNIT
}

data class EquivPosRecord(var type : EquivType, val x : Int, val y : Int, val team : Int) {
}

public data class GameStateData(
        val characters : Map<String, Character>,
        val board : GameBoard,
        val chairs : Map<SquareAssoc,Ord> = board.square.mapIndexed { i, square -> Pair(i,square) }.filter { p -> p.second.role == SquareRole.COMMAND_SEAT }.map { p -> Pair(p.second.assoc, Ord(p.first)) }.toMap(),
        val stations : Map<SquareAssoc,Ord> = board.square.mapIndexed { i, square -> Pair(i,square) }.filter { p -> p.second.role == SquareRole.WORK_STATION }.map { p -> Pair(p.second.assoc, Ord(p.first)) }.toMap(),
        val hints : Hints = Hints(board, board.square.mapIndexed { i, square -> Pair(i,square) }.filter { p -> p.second.role == SquareRole.COMMAND_SEAT }.map { p -> Pair(p.second.assoc, Ord(p.first)) }.toMap())
        ) {
    fun distinctState() : List<EquivPosRecord> {
        val doors = board.doors.values.map { d ->
            if (d.amtOpen >= 0.75) {
                EquivPosRecord(EquivType.OPEN_DOOR, d.x, d.y, 0)
            } else {
                EquivPosRecord(EquivType.CLOSED_DOOR, d.x, d.y, 0)
            }
        }
        return characters.values.map { ch -> EquivPosRecord(EquivType.UNIT, ch.x.toInt(), ch.y.toInt(), ch.team) }.plus(doors)
    }
    fun equivalent(other : GameStateData) : Boolean {
        return distinctState().equals(other.distinctState())
    }

    override fun hashCode() : Int {
        return toString().hashCode()
    }

    fun visualize() : String {
        return toString()
    }

    override fun toString() : String {
        return characters.values.map { ch -> ch.toString() }.joinToString("\n") +
            board.doors.values.map { d -> d.toString() }.joinToString("\n")
    }

    fun isWin(team : Int) : Boolean {
        return characters.values.filter { ch ->
                if (ch.team == team) {
                    val ord = board.ordOfCoords(ch.x.toInt(), ch.y.toInt())
                    board.square[ord.idx].role == SquareRole.COMMAND_SEAT
                } else {
                    false
                }
            }.count() >= 3
    }

    fun isBetterScore(team : Int, score : Double) : Boolean {
        return score(team) > score
    }

    fun score(team : Int) : Double {
        val stuff = characters.values.map { ch ->
            val ord = board.ordOfCoords(ch.x.toInt(), ch.y.toInt())
            val ords = arrayOf(
                    board.ordOfCoords((ch.x-1).toInt(),ch.y.toInt()),
                    board.ordOfCoords((ch.x+1).toInt(),ch.y.toInt()),
                    board.ordOfCoords(ch.x.toInt(),(ch.y-1).toInt()),
                    board.ordOfCoords(ch.x.toInt(),(ch.y+1).toInt())
            ).filter { ord -> ord.idx >= 0 && ord.idx < board.dimX * board.dimY }
            if (ch.team == team)
            {
                var score = ch.health
                if (board.square[ord.idx].role == SquareRole.COMMAND_SEAT) {
                    score += 1000
                }
                if (ords.any { ord ->
                    board.square[ord.idx].role == SquareRole.WORK_STATION }) {
                    score += 100
                }
                score
            } else {
                var score = -ch.health
                if (board.square[ord.idx].role == SquareRole.COMMAND_SEAT) {
                    score -= 1000
                }
                if (ords.any { ord ->
                    board.square[ord.idx].role == SquareRole.WORK_STATION }) {
                    score -= 100
                }
                score
            }
        }
        if (stuff.count() > 0) {
            return stuff.sumByDouble { s -> s.toDouble() }
        } else {
            return 0.0
        }
    }

    fun isPassable(x : Int, y : Int) : Boolean {
        return board.isPassable(x, y) && !(characters.any { ch -> (ch.value.x).toInt() == x && (ch.value.y).toInt() == y })
    }
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

fun initDisplayFromState(logical : GameStateData) :
        Map<String, CharacterDisplay> {
    val charMap : MutableMap<String, CharacterDisplay> = mutableMapOf();
    for (kv in logical.characters) {
        charMap[kv.key] =
                CharacterDisplay(
                        CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.IDLE),
                        0.0,
                        0.0
                )
    }
    return charMap;
}

data class GameDisplay(val logical: GameStateData, val characters : Map<String, CharacterDisplay> = initDisplayFromState(logical)) {
}

data class PathCommand(val x : Int, val y : Int, val open : Boolean) {
}

fun bitsToNeighbors(bits : Int, pt : Pair<Int, Int>) : Iterable<Pair<Int,Int>> {
    val res : ArrayList<Pair<Int,Int>> = arrayListOf()
    if (bits.and(1) != 0) {
        res.add(Pair(pt.first - 1, pt.second))
    }
    if (bits.and(2) != 0) {
        res.add(Pair(pt.first, pt.second - 1))
    }
    if (bits.and(4) != 0) {
        res.add(Pair(pt.first + 1, pt.second))
    }
    if (bits.and(8) != 0) {
        res.add(Pair(pt.first, pt.second + 1))
    }
    return res
}

fun directionOf(a : Pair<Double,Double>, b : Pair<Double,Double>) : CharacterDirection {
    if (a.first == b.first) {
        if (a.second < b.second) { return CharacterDirection.SOUTH } else { return CharacterDirection.NORTH }
    } else {
        if (a.first < b.first) { return CharacterDirection.EAST } else { return CharacterDirection.WEST }
    }
}


data class PathComponent(val prev: PathComponent?, val open: Boolean, val me: Pair<Int, Int>) {}

fun addIfPassable(board : GameBoard, targetX: Int, targetY: Int, first: PathComponent, visited: ArrayList<PathComponent>) {
    if (board.isPassable(targetX, targetY)) {
        visited.add(PathComponent(first, false, Pair(targetX, targetY)))
    }
}

fun pathfind(board : GameBoard, fromX: Double, fromY: Double, toX: Double, toY: Double): ArrayList<Pair<Int, Int>>? {
    val atX: Int = Math.round(fromX)
    val atY: Int = Math.round(fromY)
    val wantX: Int = Math.round(toX)
    val wantY: Int = Math.round(toY)
    val wantIdx = wantY * board.dimX + wantX
    val visited: ArrayList<PathComponent> = arrayListOf()
    visited.add(PathComponent(null, false, Pair(atX, atY)))
    while (visited.count() > 0) {
        val first = visited[0]
        visited.removeAt(0)
        if (first.me.first == wantX && first.me.second == wantY) {
            val al: ArrayList<Pair<Int, Int>> = arrayListOf()
            var f: PathComponent? = first
            while (f != null) {
                al.add(0, f.me)
                f = f.prev
            }
            return al
        }
        addIfPassable(board, first.me.first - 1, first.me.second, first, visited)
        addIfPassable(board, first.me.first + 1, first.me.second, first, visited)
        addIfPassable(board, first.me.first, first.me.second - 1, first, visited)
        addIfPassable(board, first.me.first, first.me.second + 1, first, visited)
    }
    return null
}

class Hints(val board : GameBoard, chairs : Map<SquareAssoc,Ord>) {
    fun createTowardCommandGradient(chair : Ord) : Array<CharacterDirection?> {
        val arr = Array<CharacterDirection?>(board.dimX * board.dimY, { idx -> null });
        if (chair != null) {
            val queue = arrayListOf(Pair(chair, 0))
            while (queue.size > 0) {
                val qe = queue[0]
                queue.removeAt(0)
                val qpt = board.coordsOfOrd(qe.first)
                val neighborsBits = board.getNeighbors(qpt.first, qpt.second).xor(15)
                val newNeighbors = bitsToNeighbors(neighborsBits, qpt).map { pt ->
                    Pair(board.ordOfCoords(pt.first, pt.second), qe.second + 1)
                }
                for (o in newNeighbors) {
                    if (arr[o.first.idx] == null) {
                        val c1 = board.coordsOfOrd(qe.first)
                        val c2 = board.coordsOfOrd(o.first)
                        arr[o.first.idx] = directionOf(Pair(c1.first.toDouble(),c1.second.toDouble()), Pair(c2.first.toDouble(),c2.second.toDouble()))
                        queue.add(o)
                    }
                }
            }
            arr[chair.idx] = null
        }
        return arr
    }
    val towardCommand = SquareAssoc.values().flatMap({ x ->
        val chair = chairs.get(x)
        if (chair != null) {
            listOf(Pair(x, createTowardCommandGradient(chair)))
        } else {
            listOf()
        }
    }).toMap()
    val towardDoor : Map<Ord,Array<CharacterDirection?>> = board.doors.map { x ->
        Pair(x.key, createTowardCommandGradient(x.key))
    }.toMap()

    fun followDirection(dir : CharacterDirection, atX : Int, atY : Int) : Pair<Int,Int> {
        if (dir == CharacterDirection.SOUTH) {
            return Pair<Int, Int>(atX, atY - 1)
        } else if (dir == CharacterDirection.NORTH) {
            return Pair<Int, Int>(atX, atY + 1)
        } else if (dir == CharacterDirection.WEST) {
            return Pair<Int, Int>(atX + 1, atY)
        } else {
            return Pair<Int, Int>(atX - 1, atY)
        }
    }

    fun followGradient(gradient : Array<CharacterDirection?>, atX : Int, atY : Int) : ArrayList<Pair<Int,Int>>? {
        var count = 0
        val res : ArrayList<Pair<Int,Int>> = arrayListOf()
        var where = Pair<Int,Int>(atX, atY)
        var start = gradient[board.ordOfCoords(atX, atY).idx]
        if (start == null) {
            console.log("No start!")
            return null
        } else {
            while (start != null) {
                res.add(where)
                where = followDirection(start, where.first, where.second)
                start = gradient[board.ordOfCoords(where.first, where.second).idx]
                count += 1
                if (count > 1000) { throw Exception("Bad Following") }
            }
            res.add(where)
            return res
        }
    }

    fun showgradient(g : Array<CharacterDirection?>) : String {
        val raw =
            g.map { x ->
                if (x == null) { "." } else {
                    x.toString().substring(0,1)
                }
            }.joinToString("")
        return (0..(board.dimY - 1)).map { x ->
                raw.substring(x * board.dimX, (x+1) * board.dimX)
            }.joinToString("\n")
    }

    fun pathfind(a : Pair<Double, Double>, b : Pair<Double, Double>) : ArrayList<Pair<Int,Int>>? {
        // Find the closest door to each
        if (a == b) {
            return ArrayList<Pair<Int,Int>>(listOf(Pair(a.first.toInt(),a.second.toInt())))
        }
        val doorA = board.doors.values.sortedBy { door ->
            distance(a.first, a.second, door.x.toDouble(), door.y.toDouble())
        }.firstOrNull()
        val doorB = board.doors.values.sortedBy { door ->
            distance(b.first, b.second, door.x.toDouble(), door.y.toDouble())
        }.firstOrNull()
        // Follow the gradients, stopping at the first door we cross.
        if (doorA != null && doorB != null) {
            val agrad = towardDoor[board.ordOfCoords(doorA.x, doorA.y)]
            val bgrad = towardDoor[board.ordOfCoords(doorB.x, doorB.y)]
            if (agrad != null && bgrad != null) {
                val pathToDoorA = followGradient(agrad, a.first.toInt(), a.second.toInt())
                val pathToDoorB = followGradient(bgrad, b.first.toInt(), b.second.toInt())
                // Truncate each path at the first door it crosses in case it isn't the closest in space distance
                if (pathToDoorA != null && pathToDoorB != null) {
                    val pathToFirstDoorA = ArrayList<Pair<Int,Int>>()
                    for (v in pathToDoorA.asIterable()) {
                        if (board.doors.containsKey(board.ordOfCoords(v.first, v.second))) {
                            pathToFirstDoorA.add(v)
                            break
                        } else {
                            pathToFirstDoorA.add(v)
                        }
                    }
                    val pathToFirstDoorB = ArrayList<Pair<Int,Int>>()
                    for (v in pathToDoorB.asIterable()) {
                        if (board.doors.containsKey(board.ordOfCoords(v.first, v.second))) {
                            pathToFirstDoorB.add(v)
                            break
                        } else {
                            pathToFirstDoorB.add(v)
                        }
                    }
                    if (pathToFirstDoorA.size > 0 && pathToFirstDoorB.size > 0) {
                        val lastA = pathToFirstDoorA.last()
                        val lastB = pathToFirstDoorB.last()
                        if (lastA == lastB) {
                            return pathfind(board, a.first, a.second, b.first, b.second)
                        } else {
                            // pathToFirstDoorA + pathFromDoorAToDoorB + pathToFirstDoorB.reverse()
                            val abgrad = towardDoor[board.ordOfCoords(lastB.first, lastB.second)]
                            if (abgrad != null) {
                                val pathAB = followGradient(abgrad, lastA.first, lastA.second)
                                if (pathAB != null) {
                                    val res : ArrayList<Pair<Int,Int>> = ArrayList()
                                    res.plusAssign(pathToFirstDoorA.plus(pathAB.drop(1)).plus(pathToFirstDoorB.reversed().drop(1)))
                                    // Filter dups
                                    var i = 0
                                    var j = 0
                                    while (i < res.size) {
                                        j = i + 1
                                        while (j < res.size) {
                                            if (res[i] == res[j]) {
                                                var k = i+1
                                                while (k != j+1) {
                                                    res.removeAt(i+1)
                                                    k++
                                                }
                                                j = i+1
                                            } else {
                                                j++
                                            }
                                        }
                                        i++
                                    }
                                    console.log("pathfind ${a} -> ${b} = ${res}")
                                    return res
                                }
                            }
                        }
                    }
                }
            }
        }
        console.log("pathfind ${a} -> ${b} failed!!")
        return null
    }
}

fun exaggerate(a : Double, b : Double) : Double {
    if (a < b) {
        return a + 0.4
    } else if (a > b) {
        return a - 0.4
    } else {
        return 0.0
    }
}

public class GameState(logical : GameStateData, display : GameDisplay = GameDisplay(logical)) {
    val logical : GameStateData = logical
    var display : GameDisplay = display

    fun copy(logical : GameStateData = this.logical, display : GameDisplay = this.display) : GameState {
        return GameState(logical, display)
    }

    fun computeDisplay(logical: GameStateData): GameDisplay {
        return GameDisplay(logical);
    }

    fun useCommand(chid : String, cmd : Command) : GameState {
        val ch = logical.characters.get(chid)
        if (ch != null) {
            return GameState(logical.copy(characters = logical.characters.plus(Pair(chid, ch.copy(doing = RoutedCommand(logical.hints, Pair(ch.x.toInt(), ch.y.toInt()), cmd))))), display)
        } else {
            return this
        }
    }

    fun run(t : Double) : GameState {
        var updatedDoors = logical.board.doors.entries.map { kv ->
            if (kv.value.locked || kv.value.openTime + t > DOOR_CLOSE_TIME) {
                Pair(kv.key, kv.value.copy(wantState = false, openTime = kv.value.openTime + t))
            } else if (kv.value.wantState && kv.value.amtOpen < 1.0) {
                console.log("Stand clear of the opening door ${kv.value}")
                Pair(kv.key, kv.value.copy(amtOpen = Math.min(1.0, kv.value.amtOpen + t / DOOR_OPEN_TIME), openTime = kv.value.openTime + t))
            } else if (!kv.value.wantState && kv.value.amtOpen > 0.0) {
                Pair(kv.key, kv.value.copy(amtOpen = Math.max(0.0, kv.value.amtOpen - t / DOOR_OPEN_TIME), openTime = kv.value.openTime + t))
            } else {
                Pair(kv.key, kv.value.copy(openTime = kv.value.openTime + t))
            }
        }.toMap()
        val characterLocations = logical.characters.values.map { ch ->
            Pair(logical.board.ordOfCoords(ch.x.toInt(), ch.y.toInt()), ch)
        }.toMap()

        var tookDamage : Map<String,Pair<Int,Double>> = mapOf()
        var roleSwaps : Map<String, String> = mapOf()

        // For each character that has a pending action, try to move the character closer to where it's going
        var updatedCharacters = logical.characters.values.map { kv ->
            if (kv.doing.path != null && kv.doing.path.size > 0) {
                var toward = kv.doing.path[0]
                var makeNewX = { kv: Character -> kv.x }
                var makeNewY = { kv: Character -> kv.y }
                if (kv.x < toward.first) {
                    makeNewX = { kv: Character -> Math.min(kv.x + t / TILE_WALK_TIME, toward.first.toDouble()) }
                } else if (kv.x > toward.first) {
                    makeNewX = { kv: Character -> Math.max(kv.x - t / TILE_WALK_TIME, toward.first.toDouble()) }
                } else if (kv.y < toward.second) {
                    makeNewY = { kv: Character -> Math.min(kv.y + t / TILE_WALK_TIME, toward.second.toDouble()) }
                } else if (kv.y > toward.second) {
                    makeNewY = { kv: Character -> Math.max(kv.y - t / TILE_WALK_TIME, toward.second.toDouble()) }
                }
                var newDoing = kv.doing
                var newDir = kv.dir
                val newX = makeNewX(kv)
                val newY = makeNewY(kv)
                val doorOrd = logical.board.ordOfCoords((newX + 0.4).toInt(), (newY + 0.4).toInt())
                val door = updatedDoors.get(doorOrd)
                var replaceDoor : DoorState? = null
                var canPass = true
                var mustFight : Character? = characterLocations.get(doorOrd)
                if (door != null && !door.locked) {
                    replaceDoor = door.copy(wantState = true, openTime = 0.0)
                    updatedDoors = updatedDoors.plus(Pair(doorOrd, replaceDoor))
                    canPass = door.amtOpen >= 0.75
                }
                if (mustFight != null) {
                    if (mustFight.id == kv.id) {
                        mustFight = null
                    } else {
                        canPass = false
                    }
                }
                if (canPass) {
                    if (newX == toward.first.toDouble() && newY == toward.second.toDouble()) {
                        val newPath = ArrayList<Pair<Int, Int>>()
                        newPath.plusAssign(kv.doing.path.drop(1))
                        newDoing = kv.doing.copy(path = newPath)
                    } else {
                        newDir = directionOf(Pair(kv.x, kv.y), Pair(toward.first.toDouble(), toward.second.toDouble()))
                    }
                    kv.copy(
                            dir = newDir,
                            x = newX,
                            y = newY,
                            lastx = kv.x,
                            lasty = kv.y,
                            doing = newDoing
                    )
                } else if (mustFight != null && mustFight.team != kv.team) {
                    if (kv.doing.path.size == 1) {
                        newDoing = kv.doing.copy(path=arrayListOf())
                    }
                    val whatStats = classStats.get(kv.charclass)
                    val otherStats = classStats.get(mustFight.charclass)
                    if (whatStats != null && otherStats != null) {
                        val hpDrain = BASE_DPS * whatStats.attack / otherStats.defense
                        tookDamage = tookDamage.plus(Pair(mustFight.id, Pair(kv.team, hpDrain)))
                    }
                    kv.copy(
                            dir = newDir,
                            x = newX,
                            y = newY,
                            lastx = kv.x,
                            lasty = kv.y,
                            doing = newDoing
                    )
                } else if (mustFight != null) {
                    console.log("Could not fight or pass, have to go around")
                    // Swap roles
                    roleSwaps = roleSwaps.plus(Pair(kv.id, mustFight.id))
                    kv
                } else {
                    kv
                }
            } else {
                kv.copy(dir = CharacterDirection.SOUTH, lastx = kv.x, lasty = kv.y)
            }
        }.map { ch -> Pair(ch.id, ch) }
        var newCharacters = logical.characters.plus(updatedCharacters)
        for (p in tookDamage.entries) {
            val currentState = newCharacters.get(p.key)
            if (currentState != null) {
                val newHealth = Math.max(currentState.health - p.value.second, 1.0)
                var newTeam = currentState.team
                if (newHealth == 1.0) {
                    newTeam = p.value.first
                }
                newCharacters = newCharacters.plus(
                        Pair(p.key, currentState.copy(health = newHealth, team = newTeam))
                )
            }
        }
        for (p in roleSwaps) {
            val c1 = newCharacters.get(p.key)
            val c2 = newCharacters.get(p.value)
            if (c1 != null && c2 != null) {
                newCharacters = newCharacters.plus(Pair(c1.id, c1.copy(doing = c2.doing))).plus(Pair(c2.id, c2.copy(doing = c1.doing)))
            }
        }
        val charDispUpdates =
                updatedCharacters.flatMap { ch ->
                    val disp = display.characters.get(ch.second.id)
                    if (disp != null) {
                        if (ch.second.dir != disp.animation.dir) {
                            if (ch.second.lastx != ch.second.x || ch.second.lasty != ch.second.y) {
                                listOf(Pair(ch.second.id, disp.copy(animation = CharacterAnim(ch.second.dir, CharacterAnimType.WALK))))
                            } else {
                                listOf(Pair(ch.second.id, disp.copy(animation = CharacterAnim(ch.second.dir, CharacterAnimType.IDLE))))
                            }
                        } else {
                            listOf(Pair(ch.second.id, disp))
                        }
                    } else {
                        listOf()
                    }
                }
        val newlog = logical.copy(characters = newCharacters, board = logical.board.copy(doors = updatedDoors))
        return GameState(
                logical = newlog,
                display = display.copy(
                        logical = newlog,
                        characters = display.characters.plus(charDispUpdates)
                        )
        )
    }
}
