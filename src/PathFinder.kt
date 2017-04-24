/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

// DEDICATED TO ALL PIONEERS
// ICFP2012

import java.util.*

class Coord(var x: Int, var y: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coord) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return x shl 16 + y
    }

    override fun toString(): String {
        return "[$x, $y]"
    }

    fun left(): Coord {
        return Coord(x - 1, y)
    }

    fun below(): Coord {
        return Coord(x, y - 1)
    }

    fun above(): Coord {
        return Coord(x, y + 1)
    }

    fun right(): Coord {
        return Coord(x + 1, y)
    }

    fun neighbors(): Array<Coord> {
        return arrayOf(left(), below(), above(), right())
    }

    fun manhattanDistance(other: Coord): Int {
        return (Math.abs((x - other.x).toDouble()) + Math.abs((y - other.y).toDouble())).toInt()
    }

    companion object {
        // Coords and move commands are interchangable, if you know the original coordinate
        // Wait is the identity. Abort, however, does not fit.
        val ABORT: Coord
        val SHAVE: Coord

        init {
            ABORT = Coord(-1, -1)
            SHAVE = Coord(-2, -1)
        }
    }
}

interface IPathFinderEstimate {
    fun estimateDistance(m: GameStateData, a: Coord, b: Coord): Double
};

// A*
public class PathFinder {
    class CoordNode {
        var parent: CoordNode? = null;
        fun getParent(): CoordNode? {
            return parent; }

        fun setParent(node: CoordNode, metric: Int) {
            parent = node; G = metric; }

        var c: Coord? = null
        fun coord(): Coord? {
            return c; }

        var G: Int = 0
        var H: Double = 0.0
        // The estimated remaining distance
        fun ff(): Double {
            return G + H; }

        // The actual distance from the start
        fun g(): Int {
            return G; }

        // The estimated distance to some node
        fun h(): Double {
            return H; }

        constructor(parent: CoordNode?, c: Coord, backDistance: Int, estimate: Double) {
            this.parent = parent;
            this.c = c;
            G = backDistance;
            H = estimate;
        }

        override fun hashCode(): Int {
            val cc = c
            if (cc != null) {
                return cc.hashCode()
            } else {
                return 0
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other == this) {
                return true; }
            when (other) {
                is Coord -> return other.equals(c)
                is CoordNode -> {
                    val cc = other.c
                    if (cc != null) {
                        return cc.equals(c)
                    } else {
                        return false
                    }
                }
            }
            return false
        }

        override fun toString(): String {
            return "{N " + coord() + ", back(" + G + "), est(" + H + ") }";
        }
    }

    val openNodes = ArrayList<CoordNode>()
    val inOpenNodes: MutableMap<Coord, CoordNode> = mutableMapOf()
    val closedNodes: MutableMap<Coord, CoordNode> = mutableMapOf()

    val m: GameStateData
    val e: IPathFinderEstimate
    var from: Coord? = null

    public constructor(m: GameStateData, e: IPathFinderEstimate) {
        this.m = m;
        this.e = e;
    }

    fun FoundPath(cn: CoordNode): ArrayList<Coord> {
        var cnn: CoordNode? = cn
        val result = ArrayList<Coord>();
        while (cnn != null) {
            val coord = cnn.coord()
            if (coord != null) {
                result.add(coord);
            }
            cnn = cnn.getParent();
        }
        result.reverse()
        return result;
    }

    fun compute(from: Coord, to: Coord): ArrayList<Coord>? {
        var iterCount: Int = 0;
        openNodes.clear()
        inOpenNodes.clear()
        closedNodes.clear()
        var cn: CoordNode = CoordNode(null, from, 0, e.estimateDistance(m, from, to))
        openNodes.add(cn);
        val cnCoord = cn.coord()
        if (cnCoord != null) {
            inOpenNodes.put(cnCoord, cn);
            while (iterCount++ < 1000 && openNodes.size != 0) {
                openNodes.sortWith(object : Comparator<CoordNode> {
                    override fun compare(a: CoordNode, b: CoordNode): Int {
                        if (a.ff() < b.ff()) {
                            return -1
                        } else if (a.ff() > b.ff()) {
                            return 1
                        } else {
                            return 0
                        }
                    }
                });
                if (to.equals(openNodes.get(0).coord())) {
                    var node: CoordNode = openNodes.get(0);
                    return FoundPath(node);
                }
            }

            // Move the current node from the open to the closed set
            var node: CoordNode = openNodes.get(0);
            val nodeCoord = node.coord()
            if (nodeCoord != null) {
                closedNodes.put(nodeCoord, node);
                openNodes.removeAt(0);
                inOpenNodes.remove(node.coord());

                // Now, consider each of the current node's neighbours
                for (neighbor in nodeCoord.neighbors()) // for CoordNode neighbor
                {
                    val ord = m.board.ordOfCoords(neighbor.x, neighbor.y)
                    var characters = m.characters.values.toList().map { ch -> m.board.ordOfCoords(ch.x, ch.y) }.toSet()

                    var material: SquareRole = m.board.square[ord.idx].role
                    if (material == SquareRole.WALL || material == SquareRole.WORK_STATION) {
                        continue;
                    }

                    if (characters.contains(ord)) {
                        continue;
                    }

                    var g : Int = node.g() + 1
                    var neighbor_node = closedNodes.get(neighbor)
                    if (neighbor_node != null && g < neighbor_node.g()) {
                        val ncn = closedNodes . get (neighbor);
                        ncn?.setParent(node, g);
                    } else {
                        neighbor_node = inOpenNodes.get(neighbor);
                        if (neighbor_node != null) {
                            if (g < neighbor_node.g()) {
                                neighbor_node.setParent(node, g);
                            }
                        }
                    }
                    if (neighbor_node == null) {
                        val next_target : Coord = neighbor
                        neighbor_node = CoordNode (node, neighbor, g, e.estimateDistance(m, neighbor, to));
                        if (next_target != neighbor) // reference
                        {
                            closedNodes.put(neighbor, neighbor_node);
                            neighbor_node = CoordNode (neighbor_node, next_target, g, e.estimateDistance(m, next_target, to));
                        }
                        openNodes.add(neighbor_node);
                        inOpenNodes.put(next_target, neighbor_node);
                    }
                }
            }
        }

        return null; // No way to find it!
    }
}
