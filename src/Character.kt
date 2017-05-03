/**
 * Created by arty on 5/2/17.
 */

package ldjam.prozacchiwawa

public enum class CharClass {
    OFFICER, LIFESUPPORT, ENGINEER, DOCTOR, SECURITY
}

public enum class CharacterDirection {
    NORTH, EAST, SOUTH, WEST
}

public enum class CharacterAnimType {
    IDLE, HURT, WALK, CRAWL, FIGHT, OPERATE
}

public data class CharacterAnim(
        val dir : CharacterDirection,
        val type : CharacterAnimType
) {
}

public data class Character(
        val id : String,
        val name : String,
        val at : Ord,
        val lastAt : Ord,
        val charclass : CharClass,
        val team : Int,
        val health : Double,
        val dir : CharacterDirection,
        val doing : RoutedCommand,
        val cool : Double,
        val swapping : Pair<Ord,Double>?,
        val moving : Pair<Ord,Double>?
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
        return ObjPos(at.x,at.y,0.0,0.5)
    }
}
