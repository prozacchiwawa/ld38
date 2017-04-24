/**
 * Created by arty on 4/23/17.
 */

package ldjam.prozacchiwawa

import java.util.*;

public interface IPathFinder
{
    abstract fun compute(from : Coord, to : Coord) : ArrayList<Coord>
}

interface ITardisPathFinderEstimate {
    fun estimateDistance(m: GameStateData): Double
}

class TimeCoordNode {
    var depth = 0
    var ch : Character? = null
    var m: GameStateData? = null
    var parent: TimeCoordNode? = null
    var action : Pair<Character, Command>? = null

    fun getParent(): TimeCoordNode? {
        return parent; }

    fun setParent(node: TimeCoordNode, metric: Int) {
        parent = node; G = metric; }

    var G: Int = 0;
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

    public constructor(ch : Character, m: GameStateData, estimate: Double) {
        this.parent = null;
        G = 0;
        H = estimate;
        this.m = m
        this.ch = ch
        this.depth = 0
    }

    public constructor(ch : Character, target : Pair<Int, Int>, parent: TimeCoordNode, action: Pair<Character,Command>, backDistance: Int, estimate: Double) {
        this.parent = parent;
        this.action = action
        this.ch = ch
        this.depth = parent.depth + 1
        G = backDistance
        var mm = parent.m
        if (mm != null) {
            m = GameState(mm).executeCommand(action.first, action.second.type, action.second.location.first, action.second.location.second).logical
            H = estimate
        }
    }

    override fun hashCode(): Int {
        var mm = m
        if (mm != null) {
            return mm.visualize().hashCode();
        } else {
            return 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == this) {
            return true
        }
        when (other) {
            is TimeCoordNode -> {
                val mm = m
                val otherm = other.m
                if (mm != null && otherm != null) {
                    return otherm.equivalent(mm)
                } else {
                    return false
                }
            }
        }
        return false
    }

    override fun toString(): String {
        return "{back(" + G + "), est(" + H + ")}";
    }
}

// A*
// -- I always took you where you needed to go
public class TardisPathFinder {
    val openNodes = ArrayList<TimeCoordNode>()
    val inOpenNodes: MutableMap<GameStateData, TimeCoordNode> = mutableMapOf()
    val closedNodes: MutableMap<GameStateData, TimeCoordNode> = mutableMapOf()
    var m: GameStateData? = null
    var finalMine: GameStateData? = null
    var e: ITardisPathFinderEstimate? = null
    var ch : Character? = null
    var target : Pair<Int,Int>? = null

    public constructor(ch : Character, target : Pair<Int,Int>, m: GameStateData, e: ITardisPathFinderEstimate) {
        this.m = m;
        this.e = e;
        this.ch = ch
        this.target = target
    }

    public fun getFinalState(): GameStateData? {
        return finalMine; }

    fun FoundPath(openNodes: ArrayList<TimeCoordNode>): ArrayList<Pair<Character,Command>> {
        var result = ArrayList<Pair<Character,Command>>();
        var cnn: TimeCoordNode? = openNodes.get(0);
        if (cnn != null) {
            finalMine = cnn.m;
            while (cnn != null) {
                var cnm = cnn.action
                if (cnm != null) {
                    result.add(cnm);
                }
                cnn = cnn?.getParent();
            }
            result.reverse();
        }
        console.log("Found Path ${result} with score ${finalMine}")
        return result;
    }

    var iterCount = 0

    fun compute() : ArrayList<Pair<Character,Command>>? {
        val mm = m
        var ee = e
        var cc = ch
        var tgt = target
        if (mm != null && ee != null && cc != null && tgt != null) {
            openNodes.clear();
            inOpenNodes.clear()
            closedNodes.clear()
            var cn: TimeCoordNode = TimeCoordNode(cc, mm, ee.estimateDistance(mm));
            openNodes.add(cn);
            inOpenNodes.put(mm, cn);
            val movableSet = mm.characters.values.filter { ach -> ach.id == cc.id }.map { ch -> ch.id }.toSet()

            while (openNodes.size != 0) {
                console.log("Remaining ${openNodes.size}")
                openNodes.sortWith(object : Comparator<TimeCoordNode> {
                    override fun compare(a: TimeCoordNode, b: TimeCoordNode): Int {
                        if (a.ff() < b.ff()) {
                            return -1
                        } else if (a.ff() > b.ff()) {
                            return 1
                        } else {
                            return 0
                        }
                    }
                });
                if (cc != null && tgt != null) {
                    if (cc.x == tgt.first && cc.y == tgt.second) {
                        return FoundPath(openNodes)
                    }
                }

                //if (openNodes.get(0).m.getSteps() - m.getSteps() > 3 * to.manhattanDistance(from))
                //return null;

                // Move the current node from the open to the closed set
                val node: TimeCoordNode = openNodes.get(0);
                val nodeM = node.m
                if (nodeM != null) {
                    closedNodes.put(nodeM, node);
                    openNodes.removeAt(0);
                    inOpenNodes.remove(nodeM);

                    // Now, consider each of the current node's neighbours
                    var neighbors: List<Pair<Character, Command>> = emptyList()
                    neighbors = nodeM.neighbors(movableSet)

                    for (action in neighbors) {
                        var inClosedList = false;
                        var inOpenList = false;
                        var g = node.g() + 1;
                        var tn = TimeCoordNode(cc, tgt, node, action, g, ee.estimateDistance(mm));

                        var tnM = tn.m
                        if (tnM != null) {
                            var neighbor_node: TimeCoordNode? = closedNodes.get(tnM);
                            if (neighbor_node != null && g < neighbor_node.g()) {
                                neighbor_node.setParent(node, g);
                            } else {
                                neighbor_node = inOpenNodes.get(tnM);
                                if (neighbor_node != null && g < neighbor_node.g()) {
                                    neighbor_node.setParent(node, g);
                                }
                            }
                            if (neighbor_node == null) {
                                openNodes.add(tn);
                                inOpenNodes.put(tnM, tn);
                            }
                        }
                    }
                }
            }
        }
        return null
    }
}