/**
 * Created by arty on 4/26/17.
 */

package ldjam.prozacchiwawa

import java.util.*

val RECOMPUTE_TIME = 1.0

enum class Intention {
    Guard, Recruit, Command, Special
}

data class PlanInfo(val intention : Intention, val posTarget : Pair<Double,Double>?, val charTarget : String?) {
}

data class EnemyPlan(val team : Int, val state : GameState, val elapsed : Double, val unitPlans : Map<String,PlanInfo>) {
    fun step(t : Double, state : GameState) : Pair<GameState, EnemyPlan> {
        // Update the plan
        val nextTime = elapsed + t
        if (nextTime > RECOMPUTE_TIME) {
            console.log("Recompute plans for ${team}")
            // Figure out how many recruits we now have
            val recruits = state.logical.characters.values.filter { ch -> ch.team == team }
            var newPlans = unitPlans
            val newRecruits = recruits.filter { ch -> !unitPlans.containsKey(ch.id) }
            var state = state

            console.log("New Recruits: ${newRecruits}")

            if (recruits.count() < 6) {
                // Our focus is recruiting
                // Check whether any are new to us
                val followed = unitPlans.values.map { p -> p.charTarget }.toSet()
                val unfollowed = ArrayList <Character>(state.logical.characters.values.filter { ch -> ch.team != team && !followed.contains(ch.id) }.sortedBy { ch -> ch.team })
                for (r in newRecruits) {
                    // Pick a target nobody else is following yet.
                    val randomToFollow = Math.floor(rand() * unfollowed.size)
                    var ch = unfollowed[randomToFollow]
                    console.log("recruit ${ch}")
                    newPlans = newPlans.plus(Pair(r.id, PlanInfo(Intention.Recruit, posTarget = Pair(ch.x, ch.y), charTarget = ch.id)))
                    console.log("recruit with ${r}")
                    state = state.useCommand(r.id, Command(CommandType.ATTACK, Pair(ch.x.toInt(), ch.y.toInt()), Pair(ch.x.toInt(), ch.y.toInt())))
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
                        val whereAmI = state.logical.characters.get(randomNonCommandPicked.first)
                        if (whereAmI != null) {
                            val closeChair = state.logical.chairs.values.sortedBy { k ->
                                val coords = state.logical.board.coordsOfOrd(k)
                                distance(coords.first.toDouble(), coords.second.toDouble(), whereAmI.x, whereAmI.y)
                            }.firstOrNull()
                            if (closeChair != null) {
                                val coords = state.logical.board.coordsOfOrd(closeChair)
                                newPlans = newPlans.plus(Pair(randomNonCommandPicked.first, PlanInfo(Intention.Command, charTarget = null, posTarget = Pair(coords.first.toDouble(), coords.second.toDouble()))))
                                state = state.useCommand(randomNonCommandPicked.first, Command(CommandType.IDLE, Pair(whereAmI.x.toInt(), whereAmI.y.toInt()), Pair(whereAmI.x.toInt(), whereAmI.y.toInt())))
                            }
                        }
                    }
                    // Move recruits near our claimed chairs
                    for (n in newRecruits) {
                        // Find neighbors of a chair we want
                        val chairsWeWant = ArrayList<Pair<Int, Int>>(newPlans.values.filter { p -> p.intention == Intention.Command }.flatMap { p ->
                            if (p.posTarget != null) {
                                listOf(Pair(p.posTarget.first.toInt(), p.posTarget.second.toInt()))
                            } else {
                                listOf()
                            }
                        })
                        val randomChairNum = Math.floor(rand() * chairsWeWant.size)
                        val randomChair = chairsWeWant[randomChairNum]
                        val neighbors = ArrayList<Pair<Int, Int>>(bitsToNeighbors(state.logical.board.getNeighbors(randomChair.first, randomChair.second).xor(15), randomChair).toList())
                        val chosenPt = neighbors[Math.floor(rand() * neighbors.size)]
                        newPlans = newPlans.plus(Pair(n.id, PlanInfo(Intention.Guard, Pair(chosenPt.first.toDouble(), chosenPt.second.toDouble()), null)))
                        state = state.useCommand(n.id, Command(CommandType.ATTACK, chosenPt, chosenPt))
                    }
                }
            }
            return Pair(state, EnemyPlan(team, state, 0.0, newPlans))
        } else {
            return Pair(state, EnemyPlan(team, state, nextTime, unitPlans))
        }
    }
}