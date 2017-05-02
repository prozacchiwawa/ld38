/**
 * Created by arty on 4/26/17.
 */

package ldjam.prozacchiwawa

import kotlin.js.Math

val RECOMPUTE_TIME = 1.0

enum class Intention {
    Guard, Recruit, Command, Special
}

data class PlanInfo(val intention : Intention, val posTarget : Ord, val charTarget : String?) {
}

data class EnemyPlan(val team : Int, val elapsed : Double = 0.0, val unitPlans : Map<String,PlanInfo> = mapOf()) {
    fun step(t : Double, state : GameState) : Pair<GameState, EnemyPlan> {
        // Update the plan
        val nextTime = elapsed + t
        if (nextTime > RECOMPUTE_TIME) {
            // Figure out how many recruits we now have
            val recruits = state.logical.getCharacters().values.filter { ch -> ch.team == team }
            var newPlans = unitPlans
            val newRecruits = recruits.filter { ch -> !unitPlans.containsKey(ch.id) }
            var state = state

            if (recruits.count() < 6) {
                // Our focus is recruiting
                // Check whether any are new to us
                val followed = unitPlans.values.map { p -> p.charTarget }.toSet()
                val unfollowed = ArrayList <Character>(state.logical.getCharacters().values.filter { ch -> ch.team != team && !followed.contains(ch.id) }.sortedBy { ch -> ch.team })
                for (r in newRecruits) {
                    // Pick a target nobody else is following yet.
                    val randomToFollow = Math.floor(rand() * unfollowed.size)
                    var ch = unfollowed[randomToFollow]
                    console.log("recruit ${ch}")
                    newPlans = newPlans.plus(Pair(r.id, PlanInfo(Intention.Recruit, posTarget = ch.at, charTarget = ch.id)))
                    console.log("recruit with ${r}")
                    state = state.useCommand(r.id, Command(CommandType.ATTACK, ch.at, ch.at))
                    unfollowed.removeAt(randomToFollow)
                }
            } else {
                // Use 1 in 3 recruits for command, should win in the absence of anything else happening
                val commandUsers = newPlans.entries.filter { p -> p.value.intention == Intention.Command }.map { e -> Pair(e.key,e.value) }.toMap()
                val nonCommandUsers = ArrayList<Pair<String,PlanInfo>>(newPlans.entries.filter { p -> p.value.intention != Intention.Command }.map { e -> Pair(e.key,e.value) })
                if (commandUsers.count() < 3) {
                    // Select new command users.
                    for (i in 0..(3 - commandUsers.count())) {
                        val randomNonCommandUser = Math.floor(rand() * nonCommandUsers.count())
                        val randomNonCommandPicked = nonCommandUsers[randomNonCommandUser]
                        nonCommandUsers.removeAt(randomNonCommandUser)
                        // find the closest command chair
                        val whereAmI = state.logical.getCharacters().get(randomNonCommandPicked.first)
                        if (whereAmI != null) {
                            val closeChair = state.logical.chairs.values.sortedBy { k ->
                                val coords = Pair(k.x, k.y)
                                distance(coords.first, coords.second, whereAmI.at.x, whereAmI.at.y)
                            }.firstOrNull()
                            if (closeChair != null) {
                                val coords = Pair(closeChair.x, closeChair.y)
                                newPlans = newPlans.plus(
                                        Pair(randomNonCommandPicked.first, PlanInfo(Intention.Command, charTarget = null, posTarget = state.logical.board.ordOfCoords(coords)))
                                )
                                state = state.useCommand(randomNonCommandPicked.first, Command(CommandType.IDLE, whereAmI.at, whereAmI.at))
                            }
                        }
                    }
                    // Move recruits near our claimed chairs
                    for (n in newRecruits) {
                        // Find neighbors of a chair we want
                        val chairsWeWant = ArrayList<Ord>(newPlans.values.filter { p -> p.intention == Intention.Command }.flatMap { p ->
                            if (p.posTarget != null) {
                                listOf(p.posTarget)
                            } else {
                                listOf()
                            }
                        })
                        val randomChairNum = Math.floor(rand() * chairsWeWant.size)
                        val randomChair = chairsWeWant[randomChairNum]
                        val neighbors = ArrayList<Ord>(bitsToNeighbors(state.logical.board.getNeighbors(randomChair).xor(15), randomChair).toList())
                        val chosenPt = neighbors[Math.floor(rand() * neighbors.size)]
                        newPlans = newPlans.plus(Pair(n.id, PlanInfo(Intention.Guard, chosenPt, null)))
                        state = state.useCommand(n.id, Command(CommandType.ATTACK, chosenPt, chosenPt))
                    }
                }
            }
            return Pair(state, EnemyPlan(team, 0.0, newPlans))
        } else {
            return Pair(state, EnemyPlan(team, nextTime, unitPlans))
        }
    }
}