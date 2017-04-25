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
        val health : Int,
        val path : ArrayList<Pair<Character,Command>>
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

enum class EquivType {
    OPEN_DOOR, CLOSED_DOOR, UNIT
}

data class EquivPosRecord(var type : EquivType, val x : Int, val y : Int, val team : Int) {
}

public data class GameStateData(
        val characters : Map<String, Character>,
        val board : GameBoard
        ) {
    fun distinctState() : List<EquivPosRecord> {
        val doors = board.doors.values.map { d ->
            if (d.open) {
                EquivPosRecord(EquivType.OPEN_DOOR, d.x, d.y, 0)
            } else {
                EquivPosRecord(EquivType.CLOSED_DOOR, d.x, d.y, 0)
            }
        }
        return characters.values.map { ch -> EquivPosRecord(EquivType.UNIT, ch.x, ch.y, ch.team) }.plus(doors)
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
                    val ord = board.ordOfCoords(ch.x, ch.y)
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
            val ord = board.ordOfCoords(ch.x, ch.y)
            val ords = arrayOf(
                    board.ordOfCoords(ch.x-1,ch.y),
                    board.ordOfCoords(ch.x+1,ch.y),
                    board.ordOfCoords(ch.x,ch.y-1),
                    board.ordOfCoords(ch.x,ch.y+1)
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
        return board.isPassable(x, y) && !(characters.any { ch -> ch.value.x == x && ch.value.y == y })
    }

    fun nonMoveNeighbors(ch : Character) : List<Pair<Character,Command>> {
        val neighbors = ArrayList<Pair<Character,Command>>()
        val ords = getMoves(board, ch)
        for (ord in ords) {
            var door = board.doors.get(ord)
            if (door != null) {
                if (door.locked) {
                    neighbors.add(Pair(ch, Command(CommandType.ATTACK, board.coordsOfOrd(ord))))
                } else if (!door.open) {
                    neighbors.add(Pair(ch, Command(CommandType.OPEN, board.coordsOfOrd(ord))))
                } else {
                    neighbors.add(Pair(ch, Command(CommandType.CLOSE, board.coordsOfOrd(ord))))
                }
            }
        }
        for (ot in characters.values) {
            if (ords.any { ord ->
                val coord = board.coordsOfOrd(ord)
                ot.x == coord.first && ot.y == coord.second && ch.team != ot.team
            }) {
                neighbors.add(Pair(ch, Command(CommandType.ATTACK, Pair(ch.x, ch.y))))
            }
        }
        neighbors.add(Pair(ch, Command(CommandType.WAIT, Pair(ch.x, ch.y))))
        return neighbors
    }

    fun neighbors(exclude : Set<String>) : List<Pair<Character,Command>> {
        val neighbors = ArrayList<Pair<Character,Command>>()
        for (ch in characters.values.filter { ch -> !exclude.contains(ch.id) }) {
            if (exclude.contains(ch.id)) {
                continue
            }
            if (isPassable(ch.x-1,ch.y)) {
                neighbors.plusAssign(nonMoveNeighbors(ch.copy(x = ch.x-1, y = ch.y)))
            }
            if (isPassable(ch.x+1,ch.y)) {
                neighbors.plusAssign(nonMoveNeighbors(ch.copy(x = ch.x+1, y = ch.y)))
            }
            if (isPassable(ch.x,ch.y-1)) {
                neighbors.plusAssign(nonMoveNeighbors(ch.copy(x = ch.x, y = ch.y-1)))
            }
            if (isPassable(ch.x,ch.y+1)) {
                neighbors.plusAssign(nonMoveNeighbors(ch.copy(x = ch.x, y = ch.y+1)))
            }
            //neighbors.add(Pair(ch, Command(CommandType.WAIT, Pair(ch.x, ch.y))))
        }
        return neighbors
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

data class PathCommand(val x : Int, val y : Int, val open : Boolean) {
}

public class GameState(logical : GameStateData) {
    val logical: GameStateData = logical;
    var display: GameDisplay = computeDisplay(logical)
    var sel: Pair<Int, Int>? = null

    fun computeDisplay(logical: GameStateData): GameDisplay {
        return GameDisplay(logical);
    }

    data class PathComponent(val prev: PathComponent?, val open: Boolean, val me: Pair<Int, Int>) {}

    fun addIfPassable(targetX: Int, targetY: Int, first: PathComponent, visited: ArrayList<PathComponent>) {
        if (logical.board.isPassable(targetX, targetY)) {
            visited.add(PathComponent(first, false, Pair(targetX, targetY)))
        }
    }

    fun pathfind(fromX: Double, fromY: Double, toX: Double, toY: Double): ArrayList<Pair<Int, Int>>? {
        val atX: Int = Math.round(fromX)
        val atY: Int = Math.round(fromY)
        val wantX: Int = Math.round(toX)
        val wantY: Int = Math.round(toY)
        val wantIdx = wantY * logical.board.dimX + wantX
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
            addIfPassable(first.me.first - 1, first.me.second, first, visited)
            addIfPassable(first.me.first + 1, first.me.second, first, visited)
            addIfPassable(first.me.first, first.me.second - 1, first, visited)
            addIfPassable(first.me.first, first.me.second + 1, first, visited)
        }
        return null
    }

    fun addIfPassableWithOpen(targetX: Int, targetY: Int, first: PathComponent, visited: ArrayList<PathComponent>) {
        if (logical.board.isPassable(targetX, targetY)) {
            visited.add(PathComponent(first, false, Pair(targetX, targetY)))
        } else if (logical.board.doors.containsKey(logical.board.ordOfCoords(targetX, targetY))) {
            visited.add(PathComponent(first, true, Pair(targetX, targetY)))
        }
    }

    fun pathfindWithDoors(fromX: Double, fromY: Double, toX: Double, toY: Double, lastChance : Boolean): ArrayList<PathCommand>? {
        val atX: Int = Math.round(fromX)
        val atY: Int = Math.round(fromY)
        val wantX: Int = Math.round(toX)
        val wantY: Int = Math.round(toY)
        val wantIdx = wantY * logical.board.dimX + wantX
        val visited: ArrayList<PathComponent> = arrayListOf()
        visited.add(PathComponent(null, false, Pair(atX, atY)))
        var wh = getCurTime()
        while (visited.count() > 0 && wh < lastTime + 0.2) {
            val first = visited[0]
            visited.removeAt(0)
            if (first.me.first == wantX && first.me.second == wantY) {
                val al: ArrayList<PathCommand> = arrayListOf()
                var f: PathComponent? = first
                while (f != null) {
                    al.add(0, PathCommand(f.me.first, f.me.second, f.open))
                    f = f.prev
                }
                return al
            }
            addIfPassableWithOpen(first.me.first - 1, first.me.second, first, visited)
            addIfPassableWithOpen(first.me.first + 1, first.me.second, first, visited)
            addIfPassableWithOpen(first.me.first, first.me.second - 1, first, visited)
            addIfPassableWithOpen(first.me.first, first.me.second + 1, first, visited)
            wh = getCurTime()
        }
        // Try to find the nearest closed door and open it if nothing else
        if (lastChance) {
            // Exhaustively find a door
            val visited : MutableSet<Pair<Int,Int>> = mutableSetOf()
            val nextPoint : ArrayList<PathComponent> = arrayListOf()
            val gonext =
                    { pathcomp : PathComponent ->
                        if (!visited.contains(pathcomp.me)) {
                            visited.add(pathcomp.me)
                            nextPoint.add(pathcomp)
                        }
                    }
            gonext(PathComponent(null, false, Pair(atX, atY)))
            while (!nextPoint.isEmpty()) {
                val pt = nextPoint.first()
                nextPoint.remove(pt)
                var idx = (pt.me.second * logical.board.dimX) + pt.me.first
                val sq = logical.board.square[idx]
                val door = logical.board.doors.get(Ord(idx))
                if (door != null) {
                    var res = PathComponent(pt, true, Pair(door.x, door.y))
                    val al: ArrayList<PathCommand> = arrayListOf()
                    var f: PathComponent? = res
                    while (f != null) {
                        al.add(0, PathCommand(f.me.first, f.me.second, f.open))
                        f = f.prev
                    }
                    return al
                } else if (sq.role != SquareRole.WALL && sq.role != SquareRole.WORK_STATION) {
                    gonext(PathComponent(pt, false, Pair(pt.me.first - 1, pt.me.second)))
                    gonext(PathComponent(pt, false, Pair(pt.me.first + 1, pt.me.second)))
                    gonext(PathComponent(pt, false, Pair(pt.me.first, pt.me.second - 1)))
                    gonext(PathComponent(pt, false, Pair(pt.me.first, pt.me.second + 1)))
                }
            }
        }
        return null
    }

    fun executeCommand(ch_: Character, cmd: CommandType, x: Int, y: Int): GameState {
        var ch = ch_
        val newPosition = Pair(ch.x, ch.y)
        val cold = logical.characters.get(ch.id)
        val cdisp = display.characters.get(ch.id)
        if (cold != null && cdisp != null) {
            var wold = Pair(cold.x, cold.y)
            if (!wold.equals(cold)) {
                if (logical.characters.values.filter { ot -> ot.x == ch.x && ot.y == ch.y }.count() > 0) {
                    ch = ch.copy(x = cold.x, y = cold.y)
                }
            }
            var logical = logical.copy(characters = logical.characters.plus(Pair(ch.id, ch)))
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
                val ot = logical.characters.filter { kv -> kv.value.x == x && kv.value.y == y }.toList().firstOrNull()
                if (ot != null) {
                    var targetStats = classStats.getOrElse(ot.second.charclass, { ClassStats(2.0, 2.0) })
                    if (ot.second.team == -1) {
                        targetStats = stats.copy(defense = stats.defense / 5.0)
                    }
                    val damage = (rand() * 5.0) + (rand() * 5.0 * (stats.attack / targetStats.defense))
                    val cdisp = display.characters.get(ch.id)
                    if (cdisp != null) {
                        display.characters.put(ch.id, cdisp.copy(lastDamage = damage, animation = CharacterAnim(cdisp.animation.dir, CharacterAnimType.FIGHT)))
                    }
                    val health = Math.max(ot.second.health - damage, 0.0).toInt()
                    var team = ot.second.team
                    if (health == 0) {
                        team = ch.team
                    }
                    val gs = GameState(logical.copy(characters = logical.characters.plus(Pair(ot.first, ot.second.copy(health = health, team = team)))))
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

    fun doPostTurn(): GameState {
        val gs = GameState(logical.copy(characters = logical.characters.mapValues { ch ->
            val ord = logical.board.ordOfCoords(ch.value.x, ch.value.y)
            if (logical.board.square[ord.idx].role == SquareRole.HEALING_BED) {
                ch.value.copy(health = CHAR_START_HP)
            } else if (ch.value.health < CHAR_START_HP) {
                ch.value.copy(health = Math.min(ch.value.health + 1.0, CHAR_START_HP.toDouble()).toInt())
            } else {
                ch.value
            }
        }))
        gs.display = display
        return gs
    }

    data class ToFight(val ours: Character, val theirs: Character, val dist: Int) {}

    fun findAWayForward(ch: Character): ArrayList<Pair<Character, Command>> {
        if (ch.path.size > 0) {
            return ch.path
        }
        var currentScore = logical.score(ch.team)

        val takePath: ArrayList<Pair<Character, Command>> = arrayListOf()
        val recruitCount = logical.characters.values.filter { och -> och.team == ch.team }.count()
        val lowHealth = logical.characters.values.filter { ch -> ch.health < 10 }
        val controlledPoints = logical.characters.values.filter { ch ->
            val ord = logical.board.ordOfCoords(ch.x, ch.y)
            logical.board.square[ord.idx].role == SquareRole.COMMAND_SEAT
        }
        val closestPoint = (0..(logical.board.square.size - 1)).filter { idx ->
            logical.board.square[idx].role == SquareRole.COMMAND_SEAT
        }.map { idx ->
            val ord = Ord(idx)
            val crd = logical.board.coordsOfOrd(ord)
            Pair(ord, Math.abs((crd.first - ch.x).toDouble()) + Math.abs((crd.second - ch.y).toDouble()))
        }.sortedBy { x -> x.second }.first()

        val controlledSpecials = logical.characters.values.filter { ch ->
            val ords = getMoves(logical.board, ch)
            ords.filter { ord -> logical.board.square[ord.idx].role == SquareRole.WORK_STATION }.count() > 0
        }
        val charpositions = logical.characters.values.map { ch -> Pair(logical.board.ordOfCoords(ch.x, ch.y), ch) }.toMap()

        if (rand().toInt() % 3 == 0) {
            if (recruitCount < logical.characters.size / 5) {
                // Recruit
                console.log("Try to recruit")
                var closestchars = logical.characters.values.filter { och -> och.team != ch.team }.map { och ->
                    ToFight(ch, och, (Math.abs((ch.x - och.x).toDouble()) + Math.abs((ch.y - och.y).toDouble())).toInt())
                }.sortedBy { e -> e.dist }.take(1)
                for (cc in closestchars) {
                    console.log("${cc.ours.name} ${cc.ours.x},${cc.ours.y} trying to recruit ${cc.theirs.name} ${cc.theirs.x},${cc.theirs.y}")
                    val pf = pathfindWithDoors(cc.ours.x.toDouble(), cc.ours.y.toDouble(), cc.theirs.x.toDouble(), cc.theirs.y.toDouble(), true)
                    if (pf != null) {



                        takePath.plusAssign(pf.flatMap { p ->
                            if (p.open) {
                                arrayOf(Pair(ch, Command(CommandType.OPEN, Pair(p.x, p.y))), Pair(ch, Command(CommandType.WAIT, Pair(p.x, p.y)))).asIterable()
                            } else {
                                arrayOf(Pair(ch.copy(x = p.x, y = p.y), Command(CommandType.WAIT, Pair(p.x, p.y)))).asIterable()
                            }
                        })
                        return takePath
                    }
                }
            }

            if (controlledPoints.count() < 3) {
                var coords = logical.board.coordsOfOrd(closestPoint.first)
                console.log("${ch.name} trying to get control point ${closestPoint}")
                var pf = pathfindWithDoors(ch.x.toDouble(), ch.y.toDouble(), coords.first.toDouble(), coords.second.toDouble(), true)
                if (pf != null) {
                    takePath.plusAssign(pf.flatMap { p ->
                        if (p.open) {
                            arrayOf(Pair(ch, Command(CommandType.OPEN, Pair(p.x, p.y))), Pair(ch, Command(CommandType.WAIT, Pair(p.x, p.y)))).asIterable()
                        } else {
                            arrayOf(Pair(ch.copy(x = p.x, y = p.y), Command(CommandType.WAIT, Pair(p.x, p.y)))).asIterable()
                        }
                    })
                    return takePath
                }
            } else if (controlledSpecials.count() < 2) {
            }
        } else {
            // Fallback
            val excludes = logical.characters.keys.toMutableSet()
            excludes.remove(ch.id)
            val commandList = logical.neighbors(excludes).toList()
            val choice = Math.floor(rand() * commandList.size)
            takePath.add(commandList[choice])
            excludes.add(ch.id)
            return takePath
        }

        takePath.clear()
        return takePath
    }
}
