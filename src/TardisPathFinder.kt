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
    var m: GameStateData? = null
    var parent: TimeCoordNode? = null
    var action : Pair<Character, Command>? = null
    var targetScore : Double = 0.0

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

    var team: Int = 0

    public constructor(team: Int, targetScore : Double, m: GameStateData, estimate: Double) {
        this.parent = null;
        this.targetScore = targetScore
        G = 0;
        H = estimate;
        this.m = m
        this.team = team
        this.depth = 0
    }

    var viable: Boolean = true
    var win: Boolean = false

    public constructor(team : Int, parent: TimeCoordNode, action: Pair<Character,Command>, backDistance: Int, estimate: Double) {
        this.team = team
        this.parent = parent;
        this.action = action
        this.depth = parent.depth + 1
        G = backDistance
        var mm = parent.m
        if (mm != null) {
            m = GameState(mm).executeCommand(action.first, action.second.type, action.second.location.first, action.second.location.second).logical
            if (mm.isBetterScore(team, targetScore)) {
                H = 1e15;
            } else {
                H = estimate;
            }
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
    var targetScore : Double = 0.0

    val openNodes = ArrayList<TimeCoordNode>()
    val inOpenNodes: MutableMap<GameStateData, TimeCoordNode> = mutableMapOf()
    val closedNodes: MutableMap<GameStateData, TimeCoordNode> = mutableMapOf()
    var m: GameStateData? = null
    var finalMine: GameStateData? = null
    var e: ITardisPathFinderEstimate? = null

    public constructor(m: GameStateData, targetScore : Double, e: ITardisPathFinderEstimate) {
        this.m = m;
        this.e = e;
        this.targetScore = targetScore
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
        return result;
    }

    fun compute(team: Int, maxDepth : Int): ArrayList<Pair<Character,Command>>? {
        val mm = m
        var ee = e
        if (mm != null && ee != null) {
            var iterCount: Int = 0;
            openNodes.clear();
            inOpenNodes.clear()
            closedNodes.clear()
            var cn: TimeCoordNode = TimeCoordNode(team, targetScore, mm, ee.estimateDistance(mm));
            openNodes.add(cn);
            inOpenNodes.put(mm, cn);
            while (iterCount++ < 100000 && openNodes.size != 0) {
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
                if (mm.isBetterScore(team, targetScore)) {
                    return FoundPath(openNodes);
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
                    var neighbors : List<Pair<Character,Command>> = emptyList()
                    if (node.depth < maxDepth) {
                        neighbors = nodeM.neighbors(emptySet())
                    }
                    for (action in neighbors) {
                        var inClosedList = false;
                        var inOpenList = false;
                        var g = node.g() + 1;
                        var tn = TimeCoordNode(team, node, action, g, ee.estimateDistance(mm));

                        if (!tn.viable) {
                            continue;
                        }

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

                return null; // No way to find it!
            }
        }
        return null; // No way to find it!
    }
}