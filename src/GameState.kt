/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

import kotlin.js.Math

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

public data class Ord(val idx : Int, val x : Double, val y : Double, val dimX : Int) {
    override fun equals(other: Any?): Boolean {
        if (other != null) {
            when (other) {
                is Ord -> other.idx == idx
            }
            return false
        } else {
            return false
        }
    }

    override fun hashCode(): Int {
        return idx
    }

    fun add(x : Double, y : Double) : Ord {
        val nx = this.x + x
        val ny = this.y + y
        val newIdx = (Math.floor(ny) * dimX) + Math.floor(nx)
        return Ord(newIdx, nx, ny, dimX)
    }

    fun set(nx : Double, ny : Double) : Ord {
        val newIdx = (Math.floor(ny) * dimX) + Math.floor(nx)
        return Ord(newIdx, nx, ny, dimX)
    }
}

public data class CharacterAnim(
        val dir : CharacterDirection,
        val type : CharacterAnimType
        ) {
}

data class RoutedCommand(val hints : Hints, val ch : Ord, val cmd : Command, val path : ArrayList<Ord>? = hints.pathfind(ch, cmd.at)) { }

public data class Character(
        val id : String,
        val name : String,
        val at : Ord,
        val lastAt : Ord,
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
        val ord : Ord,
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
        val doors : Map<Int, DoorState>
        ) {
    fun isPassable(at : Ord) : Boolean {
        if (at.y < 0.0 || at.x < 0.0 || at.y >= dimY || at.x >= dimX) {
            return false
        } else {
            val theSquare = square[at.idx]
            val theDoor = doors[at.idx]
            if (theDoor != null) {
                return theDoor.amtOpen >= 0.75
            } else {
                return theSquare.role != SquareRole.WALL
            }
        }
    }

    fun getNeighbor(at : Ord) : Square {
        if (at.x < 0.0 || at.y < 0.0 || at.x >= dimX || at.y >= dimY) {
            return Square(SquareRole.WALL, SquareAssoc.NOASSOC, -1)
        } else {
            return square[at.idx]
        }
    }

    fun ordOfCoords(x : Double, y : Double) : Ord { return Ord((Math.floor(y) * dimX) + Math.floor(x), x, y, dimX) }
    fun ordOfCoords(p : Pair<Double,Double>) : Ord { return ordOfCoords(p.first, p.second) }
    fun ordOfIdx(i : Int) : Ord {
        val y = i / dimX
        val x = i % dimX
        return ordOfCoords(x.toDouble(),y.toDouble())
    }

    fun getNeighborsWithDoors(at : Ord) : Int {
        var res : Int = 0
        val leftNeighbor = at.add(-1.0,0.0)
        val rightNeighbor = at.add(1.0,0.0)
        val upNeighbor = at.add(0.0,-1.0)
        val downNeighbor = at.add(0.0,1.0)
        if ((getNeighbor(leftNeighbor).role != SquareRole.NOROLE) || doors.containsKey(leftNeighbor.idx)) {
            res = res.or(1)
        }
        if ((getNeighbor(rightNeighbor).role != SquareRole.NOROLE) || doors.containsKey(rightNeighbor.idx)) {
            res = res.or(4)
        }
        if ((getNeighbor(upNeighbor).role != SquareRole.NOROLE) || doors.containsKey(upNeighbor.idx)) {
            res = res.or(2)
        }
        if ((getNeighbor(downNeighbor).role != SquareRole.NOROLE) || doors.containsKey(downNeighbor.idx)) {
            res = res.or(8)
        }
        return res
    }

    fun getNeighbors(at : Ord) : Int {
        var res : Int = 0
        val leftNeighbor = at.add(-1.0,0.0)
        val rightNeighbor = at.add(1.0,0.0)
        val upNeighbor = at.add(0.0,-1.0)
        val downNeighbor = at.add(0.0,1.0)
        if (getNeighbor(leftNeighbor).role == SquareRole.WALL) {
            res = res.or(1)
        }
        if (getNeighbor(rightNeighbor).role == SquareRole.WALL) {
            res = res.or(4)
        }
        if (getNeighbor(upNeighbor).role == SquareRole.WALL) {
            res = res.or(2)
        }
        if (getNeighbor(downNeighbor).role == SquareRole.WALL) {
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

data class EquivPosRecord(var type : EquivType, val at : Ord, val team : Int) {
}

public data class GameStateData(
        val characters : Map<String, Character>,
        val board : GameBoard,
        val chairs : Map<SquareAssoc,Ord> = board.square.mapIndexed { i, square -> Pair(board.ordOfIdx(i),square) }.filter { p -> p.second.role == SquareRole.COMMAND_SEAT }.map { p -> Pair(p.second.assoc, p.first) }.toMap(),
        val stations : Map<SquareAssoc,Ord> = board.square.mapIndexed { i, square -> Pair(board.ordOfIdx(i),square) }.filter { p -> p.second.role == SquareRole.WORK_STATION }.map { p -> Pair(p.second.assoc, p.first) }.toMap(),
        val hints : Hints = Hints(board, board.square.mapIndexed { i, square -> Pair(board.ordOfIdx(i),square) }.filter { p -> p.second.role == SquareRole.COMMAND_SEAT }.map { p -> Pair(p.second.assoc, p.first) }.toMap())
        ) {
    fun distinctState() : List<EquivPosRecord> {
        val doors = board.doors.values.map { d ->
            if (d.amtOpen >= 0.75) {
                EquivPosRecord(EquivType.OPEN_DOOR, d.ord, 0)
            } else {
                EquivPosRecord(EquivType.CLOSED_DOOR, d.ord, 0)
            }
        }
        return characters.values.map { ch -> EquivPosRecord(EquivType.UNIT, ch.at, ch.team) }.plus(doors)
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
                    board.square[ch.at.idx].role == SquareRole.COMMAND_SEAT
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
            val ords = arrayOf(
                    ch.at.add(-1.0,0.0),
                    ch.at.add(1.0,0.0),
                    ch.at.add(0.0,-1.0),
                    ch.at.add(0.0,1.0)
            ).filter { ord -> ord.idx >= 0 && ord.idx < board.dimX * board.dimY }
            if (ch.team == team)
            {
                var score = ch.health
                if (board.square[ch.at.idx].role == SquareRole.COMMAND_SEAT) {
                    score += 1000
                }
                if (ords.any { ord ->
                    board.square[ord.idx].role == SquareRole.WORK_STATION }) {
                    score += 100
                }
                score
            } else {
                var score = -ch.health
                if (board.square[ch.at.idx].role == SquareRole.COMMAND_SEAT) {
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

    fun isPassable(at : Ord) : Boolean {
        return board.isPassable(at) && !(characters.any { ch -> ch.value.at == at })
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

fun bitsToNeighbors(bits : Int, pt : Ord) : Iterable<Ord> {
    val res : ArrayList<Ord> = arrayListOf()
    if (bits.and(1) != 0) {
        res.add(pt.add(-1.0, 0.0))
    }
    if (bits.and(2) != 0) {
        res.add(pt.add(0.0, -1.0))
    }
    if (bits.and(4) != 0) {
        res.add(pt.add(1.0, 0.0))
    }
    if (bits.and(8) != 0) {
        res.add(pt.add(0.0, 1.0))
    }
    return res
}

fun directionOf(a : Ord, b : Ord) : CharacterDirection {
    if (Math.floor(a.x) == Math.floor(b.x)) {
        if (a.idx < b.idx) { return CharacterDirection.SOUTH } else { return CharacterDirection.NORTH }
    } else {
        if (a.idx < b.idx) { return CharacterDirection.EAST } else { return CharacterDirection.WEST }
    }
}


data class PathComponent(val prev: PathComponent?, val open: Boolean, val me: Ord) {}

fun addIfPassable(board : GameBoard, target : Ord, first: PathComponent, visited: ArrayList<PathComponent>) {
    if (board.isPassable(target)) {
        visited.add(PathComponent(first, false, target))
    }
}

fun pathfind(board : GameBoard, from : Ord, to : Ord): ArrayList<Ord>? {
    val at = from
    val want = to
    val visited: ArrayList<PathComponent> = arrayListOf()
    visited.add(PathComponent(null, false, at))
    while (visited.count() > 0) {
        val first = visited[0]
        visited.removeAt(0)
        if (first.me == want) {
            val al: ArrayList<Ord> = arrayListOf()
            var f: PathComponent? = first
            while (f != null) {
                al.add(0, f.me)
                f = f.prev
            }
            return al
        }
        addIfPassable(board, first.me.add(-1.0, 0.0), first, visited)
        addIfPassable(board, first.me.add(1.0, 0.0), first, visited)
        addIfPassable(board, first.me.add(0.0, -1.0), first, visited)
        addIfPassable(board, first.me.add(0.0, 1.0), first, visited)
    }
    return null
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
            return GameState(logical.copy(characters = logical.characters.plus(Pair(chid, ch.copy(doing = RoutedCommand(logical.hints, ch.at, cmd))))), display)
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

        val characterLocations = logical.characters.values.map { ch -> Pair(ch.at, ch) }.toMap()

        var tookDamage : Map<String,Pair<Int,Double>> = mapOf()
        var roleSwaps : Map<String, String> = mapOf()

        // For each character that has a pending action, try to move the character closer to where it's going
        var updatedCharacters = logical.characters.values.map { kv ->
            if (kv.doing.path != null && kv.doing.path.size > 0) {
                var toward = kv.doing.path[0]
                var makeNew = { kv: Character -> kv.at }
                if (kv.at.x < toward.x) {
                    makeNew = { kv: Character -> kv.at.set(Math.min(kv.at.x + t / TILE_WALK_TIME, toward.x), kv.at.y) }
                } else if (kv.at.x > toward.x) {
                    makeNew = { kv: Character -> kv.at.set(Math.max(kv.at.x - t / TILE_WALK_TIME, toward.x), kv.at.y) }
                } else if (kv.at.y < toward.y) {
                    makeNew = { kv: Character -> kv.at.set(kv.at.x, Math.min(kv.at.y + t / TILE_WALK_TIME, toward.y)) }
                } else if (kv.at.y > toward.y) {
                    makeNew = { kv: Character -> kv.at.set(kv.at.x, Math.max(kv.at.y - t / TILE_WALK_TIME, toward.y)) }
                }
                var newDoing = kv.doing
                var newDir = kv.dir
                val newAt = makeNew(kv)
                val door = updatedDoors[newAt.idx]
                var replaceDoor : DoorState? = null
                var canPass = true
                var mustFight : Character? = characterLocations.get(newAt)
                if (door != null && !door.locked) {
                    replaceDoor = door.copy(wantState = true, openTime = 0.0)
                    updatedDoors = updatedDoors.plus(Pair(newAt.idx, replaceDoor))
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
                    if (Math.floor(newAt.x) == Math.floor(toward.x) && Math.floor(newAt.y) == Math.floor(toward.y)) {
                        val newPath = ArrayList<Ord>()
                        newPath.plusAssign(kv.doing.path.drop(1))
                        newDoing = kv.doing.copy(path = newPath)
                    } else {
                        newDir = directionOf(kv.at, toward)
                    }
                    kv.copy(
                            dir = newDir,
                            at = newAt,
                            lastAt = kv.at,
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
                            at = newAt,
                            lastAt = kv.at,
                            doing = newDoing
                    )
                } else if (mustFight != null) {
                    // Swap roles
                    roleSwaps = roleSwaps.plus(Pair(kv.id, mustFight.id))
                    kv
                } else {
                    kv
                }
            } else {
                kv.copy(dir = CharacterDirection.SOUTH, lastAt = kv.at)
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
                            if (ch.second.lastAt != ch.second.at) {
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
