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

data class RoutedCommand(val hints : Hints, val ch : Character?, val cmd : Command, val path : ArrayList<Ord>? = (if (ch != null) hints.pathfind(ch, cmd.at) else ArrayList<Ord>())) { }

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
        ) : IObjGetPos {
    fun availMoves() : Int {
        var moves = 3
        if (health < CHAR_START_HP * 0.3) {
            moves = 1
        } else if (health < CHAR_START_HP * 0.75) {
            moves = 2
        }
        return moves
    }
    override fun getPos() : ObjPos {
        return ObjPos(at.x,at.y,0.0,0.4)
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
        val board : GameBoard,
        val chairs : Map<SquareAssoc,Ord> = board.square.mapIndexed { i, square -> Pair(board.ordOfIdx(i),square) }.filter { p -> p.second.role == SquareRole.COMMAND_SEAT }.map { p -> Pair(p.second.assoc, p.first) }.toMap(),
        val stations : Map<SquareAssoc,Ord> = board.square.mapIndexed { i, square -> Pair(board.ordOfIdx(i),square) }.filter { p -> p.second.role == SquareRole.WORK_STATION }.map { p -> Pair(p.second.assoc, p.first) }.toMap(),
        val hints : Hints = Hints(board, board.square.mapIndexed { i, square -> Pair(board.ordOfIdx(i),square) }.filter { p -> p.second.role == SquareRole.COMMAND_SEAT }.map { p -> Pair(p.second.assoc, p.first) }.toMap()),
        private val characters : Map<String, Character> = mapOf(),
        private val collision : OctreeNode = OctreeNode(null, Box(arrayOf(arrayOf(0.0,0.0,-10.0),arrayOf(board.dimX.toDouble(),board.dimY.toDouble(),10.0))))
        ) {
    fun getCharacters() : Map<String, Character> { return characters }
    fun getCollision() : OctreeNode { return collision }
    fun moveCharacter(id : String, ch : Character) : GameStateData {
        return copy(characters=characters.plus(Pair(id,ch)), collision=collision.remove(id,ch).insert(id,ch,false))
    }
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

    fun isPassable(ch : Character) : Boolean {
        val collisions = collision.collide(ch.id, ch)
        return board.isPassable(ch.at) && collisions.size == 0
    }

    class CharWrapperLargerRadius(val ch : Character, val newRadius : Double) : IObjGetPos {
        override fun getPos(): ObjPos {
            return ch.getPos().copy(size = newRadius)
        }
    }

    fun getAdjacent(ch : Character) : Set<String> {
        return collision.collide(ch.id, CharWrapperLargerRadius(ch, 0.5))
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
    val charMap : MutableMap<String, CharacterDisplay> = mutableMapOf()
    for (kv in logical.getCharacters()) {
        charMap[kv.key] =
                CharacterDisplay(
                        CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.IDLE),
                        0.0,
                        0.0
                )
    }
    return charMap
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

fun addIfPassable(state : GameStateData, who : Character, first: PathComponent, visited: ArrayList<PathComponent>, used : Set<Int>) {
    if (state.isPassable(who) && !used.contains(who.at.idx)) {
        visited.add(PathComponent(first, false, who.at))
    }
}

fun pathfind(state : GameStateData, who : Character, to : Ord): ArrayList<Ord>? {
    val at = who.at
    val want = to
    val visited: ArrayList<PathComponent> = arrayListOf()
    var used : Set<Int> = setOf()
    visited.add(PathComponent(null, false, at))
    while (visited.count() > 0) {
        val first = visited[0]
        visited.removeAt(0)
        used = used.plus(first.me.idx)
        if (first.me.idx == want.idx) {
            val al: ArrayList<Ord> = arrayListOf()
            var f: PathComponent? = first
            while (f != null) {
                al.add(0, f.me.add(0.5,0.5))
                f = f.prev
            }
            return al
        }
        addIfPassable(state, who.copy(at=first.me.add(-1.0, 0.0)), first, visited, used)
        addIfPassable(state, who.copy(at=first.me.add(1.0, 0.0)), first, visited, used)
        addIfPassable(state, who.copy(at=first.me.add(0.0, -1.0)), first, visited, used)
        addIfPassable(state, who.copy(at=first.me.add(0.0, 1.0)), first, visited, used)
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
        val ch = logical.getCharacters()[chid]
        if (ch != null) {
            return GameState(logical.moveCharacter(chid, ch.copy(doing = RoutedCommand(logical.hints, ch, cmd))), display)
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

        var tookDamage : Map<String,Pair<Int,Double>> = mapOf()
        var roleSwaps : Map<String, String> = mapOf()
        var fighting : Set<String> = setOf()

        // For each character that has a pending action, try to move the character closer to where it's going
        var updatedCharacters = logical.getCharacters().values.map { kv ->
            if (kv.team == 0) {
                console.log(kv)
            }
            if (kv.doing.path != null && kv.doing.path.size > 0) {
                var toward = kv.doing.path[0]
                var makeNew = { kv: Character -> kv }
                if (kv.at.x < toward.x) {
                    makeNew = { kv: Character -> kv.copy(at=kv.at.set(Math.min(kv.at.x + t / TILE_WALK_TIME, toward.x), kv.at.y)) }
                } else if (kv.at.x > toward.x) {
                    makeNew = { kv: Character -> kv.copy(at=kv.at.set(Math.max(kv.at.x - t / TILE_WALK_TIME, toward.x), kv.at.y)) }
                } else if (kv.at.y < toward.y) {
                    makeNew = { kv: Character -> kv.copy(at=kv.at.set(kv.at.x, Math.min(kv.at.y + t / TILE_WALK_TIME, toward.y))) }
                } else if (kv.at.y > toward.y) {
                    makeNew = { kv: Character -> kv.copy(at=kv.at.set(kv.at.x, Math.max(kv.at.y - t / TILE_WALK_TIME, toward.y))) }
                }
                var newDoing = kv.doing
                var newDir = kv.dir
                val newAt = makeNew(kv)
                val door = updatedDoors[newAt.at.idx]
                var replaceDoor : DoorState? = null
                var canPass = true
                var mustFightChars : Set<String> = logical.getCollision().collide(kv.id, newAt)
                if (mustFightChars.size > 0) {
                    console.log("$kv oppose $mustFightChars")
                }
                if (door != null && !door.locked) {
                    replaceDoor = door.copy(wantState = true, openTime = 0.0)
                    updatedDoors = updatedDoors.plus(Pair(newAt.at.idx, replaceDoor))
                    canPass = door.amtOpen >= 0.75
                }
                if (mustFightChars.size > 0) {
                    canPass = false
                }
                val passMyTeam = mustFightChars.filter { id ->
                    val ch = logical.getCharacters()[id]
                    ch != null && ch.team == kv.team
                }
                mustFightChars = mustFightChars.minus(passMyTeam)

                console.log("canPass ${canPass}")

                if (canPass) {
                    if (distance(newAt.at.x, newAt.at.y, toward.x, toward.y) < 0.25) {
                        val newPath = ArrayList<Ord>()
                        newPath.plusAssign(kv.doing.path.drop(1))
                        newDoing = kv.doing.copy(path = newPath)
                    } else {
                        newDir = directionOf(kv.at, toward)
                    }
                    console.log("Return with at=${newAt.at} last=${kv.at}")
                    newAt.copy(
                            dir = newDir,
                            lastAt = kv.at,
                            doing = newDoing
                    )
                } else if (mustFightChars.size > 0) {
                    if (kv.doing.path.size == 1) {
                        newDoing = kv.doing.copy(path=arrayListOf())
                    }
                    val randomCharsToFight : List<Character> = mustFightChars.toList().flatMap { id ->
                        val ch = logical.getCharacters()[id]
                        if (ch != null) {
                            listOf(ch)
                        } else {
                            listOf()
                        }
                    }
                    val randomCharToFight = randomCharsToFight[Math.floor(rand() * randomCharsToFight.size)]
                    val whatStats = classStats[kv.charclass]
                    val otherStats = classStats[randomCharToFight.charclass]
                    if (whatStats != null && otherStats != null) {
                        val hpDrain = BASE_DPS * whatStats.attack / otherStats.defense
                        tookDamage = tookDamage.plus(Pair(randomCharToFight.id, Pair(kv.team, hpDrain)))
                        fighting = fighting.plus(kv.id)
                    }
                    newAt.copy(
                            dir = newDir,
                            lastAt = kv.at,
                            doing = newDoing
                    )
                } else if (passMyTeam.size == 1) {
                    // Swap roles
                    val mustPass = passMyTeam.firstOrNull()
                    if (mustPass != null) {
                        roleSwaps = roleSwaps.plus(Pair(kv.id, mustPass))
                    }
                    kv
                } else {
                    kv
                }
            } else {
                val adjacent = logical.getAdjacent(kv)
                val adjacentToFight = adjacent.filter { id ->
                    val ch = logical.getCharacters()[id]
                    ch != null && ch.team != kv.team && kv.team != -1
                }
                val randomCharsToFight : List<Character> = adjacentToFight.toList().flatMap { id ->
                    val ch = logical.getCharacters()[id]
                    if (ch != null) {
                        listOf(ch)
                    } else {
                        listOf()
                    }
                }
                if (randomCharsToFight.size > 0) {
                    val randomCharToFight = randomCharsToFight[Math.floor(rand() * randomCharsToFight.size)]
                    val whatStats = classStats[kv.charclass]
                    val otherStats = classStats[randomCharToFight.charclass]
                    if (whatStats != null && otherStats != null) {
                        val hpDrain = BASE_DPS * whatStats.attack / otherStats.defense
                        tookDamage = tookDamage.plus(Pair(randomCharToFight.id, Pair(kv.team, hpDrain)))
                        fighting = fighting.plus(kv.id)
                    }
                }
                kv
            }
        }.map { ch -> Pair(ch.id, ch) }
        var newCharacters = logical.getCharacters().plus(updatedCharacters)
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
                            } else if (fighting.contains(ch.second.id)) {
                                listOf(Pair(ch.second.id, disp.copy(animation = CharacterAnim(ch.second.dir, CharacterAnimType.FIGHT))))
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
        var newlog = logical.copy(board = logical.board.copy(doors = updatedDoors))
        for (ch in newCharacters.values) {
            newlog = newlog.moveCharacter(ch.id, ch)
        }
        return GameState(
                logical = newlog,
                display = display.copy(
                        logical = newlog,
                        characters = display.characters.plus(charDispUpdates)
                        )
        )
    }
}
