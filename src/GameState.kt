/**
 * Created by arty on 4/21/17.
 */

package ldjam.prozacchiwawa

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
        val doors : Map<Int, DoorState>
        ) {
    fun isPassable(x : Int, y : Int) : Boolean {
        if (y < 0 || x < 0 || y >= dimY || x >= dimX) {
            return false
        } else {
            val idx = (y * dimX) + x
            val theSquare = square[idx]
            val theDoor = doors.get(idx)
            if (theDoor != null) {
                return theDoor.open
            } else {
                return theSquare.role != SquareRole.WALL
            }
        }
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
            val door = board.doors.get(i)
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
}
