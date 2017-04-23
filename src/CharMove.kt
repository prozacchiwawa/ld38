/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

fun moveCharactersCloserToTargets(state : GameState, delta : Double) {
    val kvlist = state.display.characters
    for (kv in kvlist) {
        if (kv.value.dispx != kv.value.targetx || kv.value.dispy != kv.value.targety) {
            val path = state.pathfind(kv.value.targetx, kv.value.targety, kv.value.dispx, kv.value.dispy)
            if (path != null) {
                if (path.size > 1) {
                    var goTo = path[1]
                    val prevAnim = kv.value.animation
                    var newX = kv.value.dispx
                    var newY = kv.value.dispy
                    var newAnim = CharacterAnim(CharacterDirection.SOUTH, CharacterAnimType.WALK)
                    if (goTo.first < kv.value.dispx) {
                        newX = Math.max(kv.value.targetx, newX - delta * (1.0 / TILE_WALK_TIME))
                        newAnim = CharacterAnim(CharacterDirection.WEST, CharacterAnimType.WALK)
                    } else if (goTo.first > kv.value.dispx) {
                        newX = Math.min(kv.value.targetx, newX + delta * (1.0 / TILE_WALK_TIME))
                        newAnim = CharacterAnim(CharacterDirection.EAST, CharacterAnimType.WALK)
                    } else if (goTo.second < kv.value.dispy) {
                        newY = Math.max(kv.value.targety, newY - delta * (1.0 / TILE_WALK_TIME))
                        newAnim = CharacterAnim(CharacterDirection.NORTH, CharacterAnimType.WALK)
                    } else {
                        newY = Math.min(kv.value.targety, newY + delta * (1.0 / TILE_WALK_TIME))
                    }
                    var animStart = kv.value.animstart
                    if (prevAnim != newAnim) {
                        animStart = lastTime
                    }
                    state.display.characters.put(kv.key, kv.value.copy(animation = newAnim, animstart = animStart, dispx = newX, dispy = newY))
                }
            }
        }
    }
}
