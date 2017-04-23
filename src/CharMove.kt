/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

fun moveCharactersCloserToTargets(state : GameState, delta : Double) : Boolean {
    var moreRequred = false
    val kvlist = state.display.characters
    for (kv in kvlist) {
        if (kv.value.dispx != kv.value.targetx || kv.value.dispy != kv.value.targety) {
            moreRequred = true
            val path = state.pathfind(kv.value.dispx, kv.value.dispy, kv.value.targetx, kv.value.targety)
            console.log("Path from ${kv.value.dispx},${kv.value.dispy} to ${kv.value.targetx},${kv.value.targety} via ${path}")
            if (path != null) {
                var goTo = Pair(kv.value.targetx, kv.value.targety)
                if (path.size > 1) {
                    goTo = Pair(path[1].first.toDouble(),path[1].second.toDouble())
                }
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
    return !moreRequred
}
