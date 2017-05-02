package ldjam.prozacchiwawa

val BRANCH_FACTOR = 8

fun objbox(x : Double, y : Double, z : Double, size : Double = 0.0) : Box {
    return Box(
            arrayOf(
                arrayOf(x - size, y - size, z - size),
                arrayOf(x + size, y + size, z + size)
        )
    )
}

data class Fit(val lxo : Boolean, val lyo : Boolean, val lzo : Boolean, val hxo : Boolean, val hyo : Boolean, val hzo : Boolean, val outside : Boolean) { }

fun ptavg(ptlist : Array<Array<Double>>) : Array<Double> {
    var res = Array(ptlist[0].size, { _ -> 0.0 })
    for (i in 0..(ptlist.size - 1)) {
        for (j in 0..(ptlist[i].size - 1)) {
            res[j] = res[j] + ptlist[i][j]
        }
    }
    for (i in (0..(ptlist.size - 1))) {
        res[i] /= ptlist.size.toDouble()
    }
    return res
}

data class Box(val boxdim : Array<Array<Double>>) {
    fun dim(axis : Int) : Double {
        return (boxdim[1][axis] - boxdim[0][axis]);
    }
    fun dim() : Array<Double> {
        return Array<Double>(boxdim.size, { i -> dim(i) })
    }
    fun exp(axisMask : Int) : Array<Array<Double>> {
        var low = boxdim[0];
        var high = boxdim[1];
        var res = arrayOf(Array(low.size, {x -> 0.0}),Array(high.size, {x -> 0.0}))
        for (i in 0..(boxdim[0].size - 1)) {
            var l = low[i];
            var h = high[i];
            var d = h - l;
            if (axisMask.and(1.shl(i)) != 0) {
                res[0][i] = h - (2 * d);
                res[1][i] = h;
            } else {
                res[0][i] = l;
                res[1][i] = l + (2 * d);
            }
        }
        return res;
    }
    fun sub(axisMask : Int) : Array<Array<Double>> {
        var center = ptavg(boxdim)
        var res = arrayOf(Array(center.size, {x->0.0}), Array(center.size, {x->0.0}))
        for (i in 0..(boxdim[0].size - 1)) {
            if (axisMask.and(1.shl(i)) != 0) {
                res[0][i] = boxdim[0][i]
                res[1][i] = center[i]
            } else {
                res[0][i] = center[i]
                res[1][i] = boxdim[1][i]
            }
        }
        return res
    }
    fun fit(x : Double, y : Double, z : Double, size : Double) : Fit {
        var lx = x - size
        var ly = y - size
        var lz = z - size
        var hx = x + size
        var hy = y + size
        var hz = z + size
        var lxo = lx < boxdim[0][0]
        var lyo = ly < boxdim[0][1]
        var lzo = lz < boxdim[0][2]
        var hxo = hx > boxdim[1][0]
        var hyo = hy > boxdim[1][1]
        var hzo = hz > boxdim[1][2]
        return Fit(lxo, lyo, lzo, hxo, hyo, hzo, lxo || lyo || lzo || hxo || hyo || hzo)
    }
}

data class ObjPos(val x : Double, val y : Double, val z : Double, val size : Double) {
}

interface IObjGetPos {
    abstract fun getPos() : ObjPos
}

data class OctreeNode(val parent : OctreeNode? = null, val box : Box, val depth : Int = 0, val nodes : Map<Int, OctreeNode> = mapOf(), val contents : Int = 0, val total : Int = 0, val objects : Map<String,IObjGetPos> = mapOf()) {
    fun expand(obj : IObjGetPos) : OctreeNode? {
        if (parent != null) {
            throw Exception("Expanding but we're not the root")
        }
        val pos = obj.getPos()
        var fit = box.fit(pos.x, pos.y, pos.z, pos.size)
        if (!fit.outside)
            return null
        // Expand to negative coordinates
        var axisMask = (if (fit.lxo) 1 else 0).or(if (fit.lyo) 2 else 0).or(if (fit.lzo) 4 else 0)
        var box = box.exp(axisMask)
        var newNode = OctreeNode(null,Box(box),this.depth-1,nodes=this.nodes.plus(Pair(axisMask,this)))
        this.copy(parent=newNode)
        return newNode
    }

    fun createSubnode(axisMask : Int) : OctreeNode {
        var newbox = box.sub(axisMask)
        var newnode = OctreeNode(this,Box(newbox),depth+1)
        return copy(nodes=nodes.plus(Pair(axisMask,newnode)))
    }

    fun insert(id : String, obj : IObjGetPos, stop : Boolean) : OctreeNode {
        val pos = obj.getPos()
        var center = ptavg(box.boxdim)
        var axisMask =
            (if (pos.x < center[0]) 1 else 0).or(if (pos.y < center[1]) 2 else 0).or(if (pos.z < center[2]) 4 else 0)
        var subnode = nodes[axisMask]
        if (subnode == null) {
            val p = createSubnode(axisMask)
            return p.insert(id, obj, true)
        }
        var contains = subnode.box.fit(pos.x, pos.y, pos.z, pos.size)
        if (stop || contains.outside) {
            return copy(total = this.total + 1, contents = this.contents + 1, objects = objects.plus(Pair(id, obj)))
        } else {
            subnode = subnode.insert(id, obj, contents < nodes.size)
            return copy(total = this.total + 1, nodes = nodes.plus(Pair(axisMask, subnode)))
        }
    }

    fun collapse() : OctreeNode {
        var objs = objects
        for (i in 0..(nodes.size - 1)) {
            val ni = nodes[i]
            if (ni != null) {
                ni.collapse();
                objs = objs.plus(ni.objects)
            }
        }
        return copy(objects = objs, nodes = mapOf())
    }

    fun remove(id : String, obj : IObjGetPos) : OctreeNode {
        val _objects = objects
        val theobj = objects[id]
        var res : OctreeNode = this
        if (theobj != null) {
            return copy(contents=this.contents-1, total=this.total-1, objects=this.objects.minus(id))
        } else {
            val center = ptavg(box.boxdim)
            val pos = obj.getPos()
            val axisMask = (if (pos.x < center[0]) 1 else 0).or(if (pos.y < center[1]) 2 else 0).or(if (pos.z < center[2]) 4 else 0)
            var subnode = nodes[axisMask]
            if (subnode != null) {
                val prevTotal = subnode.total
                val putback = subnode.remove(id, obj)
                res = copy(total=this.total - (prevTotal - putback.total), nodes = nodes.plus(Pair(axisMask, putback)))
            }
        }
        if (res.total < BRANCH_FACTOR) {
            return res.collapse()
        } else {
            return res
        }
    }

    fun collide(id : String, o1 : IObjGetPos) : Set<String> {
        val res : MutableSet<String> = mutableSetOf()
        for (o2id in objects) {
            if (o2id.key == id) {
                continue
            }
            val p1 = o1.getPos()
            val p2 = o2id.value.getPos()
            val d = distance(p1.x, p1.y, p2.x, p2.y)
            val nd = p1.size + p2.size
            if (nd > d) {
                res.add(o2id.key)
            }
        }
        return res
    }
}
