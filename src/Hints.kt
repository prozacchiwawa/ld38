/**
 * Created by arty on 4/29/17.
 */

package ldjam.prozacchiwawa

class Hints(val board : GameBoard, chairs : Map<SquareAssoc, Ord>) {
    fun createTowardCommandGradient(chair : Ord) : Array<CharacterDirection?> {
        console.log("find ways to $chair")
        val arr = Array<CharacterDirection?>(board.dimX * board.dimY, { idx -> null });
        if (chair != null) {
            val queue = arrayListOf(Pair(chair, 0))
            while (queue.size > 0) {
                val qe = queue[0]
                queue.removeAt(0)
                val neighborsBits = board.getNeighbors(qe.first).xor(15)
                val newNeighbors = bitsToNeighbors(neighborsBits, qe.first).map { pt ->
                    Pair(pt, qe.second + 1)
                }
                for (o in newNeighbors) {
                    if (arr[o.first.idx] == null) {
                        val c1 = qe.first
                        val c2 = o.first
                        arr[o.first.idx] = directionOf(c1, c2)
                        queue.add(o)
                    }
                }
            }
            arr[chair.idx] = null
        }
        console.log(showgradient(arr))
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
    val towardDoor : Map<Int,Array<CharacterDirection?>> = board.doors.map { x ->
        Pair(x.key, createTowardCommandGradient(board.ordOfIdx(x.key)))
    }.toMap()

    fun followDirection(dir : CharacterDirection, at : Ord) : Ord {
        if (dir == CharacterDirection.SOUTH) {
            return at.add(0.0, -1.0)
        } else if (dir == CharacterDirection.NORTH) {
            return at.add(0.0, 1.0)
        } else if (dir == CharacterDirection.WEST) {
            return at.add(1.0, 0.0)
        } else {
            return at.add(-1.0, 0.0)
        }
    }

    fun followGradient(gradient : Array<CharacterDirection?>, at : Ord) : ArrayList<Ord>? {
        var count = 0
        val res : ArrayList<Ord> = arrayListOf()
        var start = gradient[at.idx]
        var where = at
        if (start == null) {
            console.log("No start!")
            return null
        } else {
            while (start != null) {
                res.add(where)
                where = followDirection(start, where)
                start = gradient[where.idx]
                console.log("where $where")
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

    fun pathfind(a : Character, b : Ord) : ArrayList<Ord>? {
        val state = GameStateData(board)
        if (a.at.idx == b.idx) {
            return arrayListOf(a.at)
        }
        // Find the closest door to each
        console.log("pathfind $a $b")
        val doorA = board.doors.values.sortedBy { door ->
            distance(a.at.x, a.at.y, door.ord.x, door.ord.y)
        }.firstOrNull()
        val doorB = board.doors.values.sortedBy { door ->
            distance(b.x, b.y, door.ord.x, door.ord.y)
        }.firstOrNull()
        // Follow the gradients, stopping at the first door we cross.
        if (doorA != null && doorB != null) {
            val agrad = towardDoor[doorA.ord.idx]
            val bgrad = towardDoor[doorB.ord.idx]
            if (agrad != null && bgrad != null) {
                val pathToDoorA = followGradient(agrad, a.at)
                val pathToDoorB = followGradient(bgrad, b)
                // Truncate each path at the first door it crosses in case it isn't the closest in space distance
                if (pathToDoorA != null && pathToDoorB != null) {
                    val pathToFirstDoorA = ArrayList<Ord>()
                    for (v in pathToDoorA.asIterable()) {
                        if (board.doors.containsKey(v.idx)) {
                            pathToFirstDoorA.add(v.add(0.5,0.5))
                            break
                        } else {
                            pathToFirstDoorA.add(v.add(0.5,0.5))
                        }
                    }
                    val pathToFirstDoorB = ArrayList<Ord>()
                    for (v in pathToDoorB.asIterable()) {
                        if (board.doors.containsKey(v.idx)) {
                            pathToFirstDoorB.add(v.add(0.5,0.5))
                            break
                        } else {
                            pathToFirstDoorB.add(v.add(0.5,0.5))
                        }
                    }
                    if (pathToFirstDoorA.size > 0 && pathToFirstDoorB.size > 0) {
                        val lastA = pathToFirstDoorA.last()
                        val lastB = pathToFirstDoorB.last()
                        if (lastA == lastB) {
                            return pathfind(state, a, b)
                        } else {
                            // pathToFirstDoorA + pathFromDoorAToDoorB + pathToFirstDoorB.reverse()
                            val abgrad = towardDoor[lastB.idx]
                            if (abgrad != null) {
                                val pathAB = followGradient(abgrad, lastA)
                                if (pathAB != null) {
                                    val res : ArrayList<Ord> = ArrayList()
                                    res.plusAssign(pathToFirstDoorA.plus(pathAB.drop(1)).plus(pathToFirstDoorB.reversed().drop(1)))
                                    // Filter duplicates
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
                                    console.log("path find $a -> $b = $res")
                                    return res
                                }
                            }
                        }
                    }
                }
            }
        }
        console.log("path find $a -> $b failed!!")
        return null
    }
}
