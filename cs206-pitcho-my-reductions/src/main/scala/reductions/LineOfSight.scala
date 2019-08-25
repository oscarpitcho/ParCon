package reductions

import org.scalameter._

object LineOfSightRunner {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 100,
    Key.verbose -> false
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val length = 10000000
    val input = (0 until length).map(_ % 100 * 1.0f).toArray
    val output = new Array[Float](length + 1)
    val seqtime = standardConfig measure {
      LineOfSight.lineOfSight(input, output)
    }
    println(s"sequential time: $seqtime")

    val partime = standardConfig measure {
      LineOfSight.parLineOfSight(input, output, 10000)
    }
    println(s"parallel time: $partime")
    println(s"speedup: ${seqtime.value / partime.value}")
  }
}

object LineOfSight {

  def max(a: Float, b: Float): Float = if (a > b) a else b

  def lineOfSight(input: Array[Float], output: Array[Float]): Unit = {
    var i = 0
    var maxV: Float = 0
    while(i < input.size) {
      maxV = max(input(i)/i, maxV)
      output(i) = if(i == 0) 0 else maxV
      i += 1
    }
  }

  sealed abstract class Tree {
    def maxPrevious: Float
    override def toString: String = {
      this match {
        case Leaf(f, u, m) => " |||from : " ++ f.toString ++ " until: " ++ u.toString ++ " max : " ++ m.toString ++ "||| "
        case Node(l, r) => l.toString ++ "  " ++ maxPrevious.toString ++ "  " ++ r.toString
      }
    }
  }

  case class Node(left: Tree, right: Tree) extends Tree {
    val maxPrevious = max(left.maxPrevious, right.maxPrevious)
  }

  case class Leaf(from: Int, until: Int, maxPrevious: Float) extends Tree

  /** Traverses the specified part of the array and returns the maximum angle.
   */
  def upsweepSequential(input: Array[Float], from: Int, until: Int): Float = {
    var i = from
    var maxAngle = 0f
    //println("Befor while : Max angle = " + maxAngle.toString + "  from = " + from + " until = "  + until)
    while(i < until) {
      maxAngle = max(if(i == 0) 0f else input(i)/i, maxAngle)
      i += 1 
    }
    //println("After while : Max angle = " + maxAngle.toString + "  from = " + from + " until = "  + until)
    maxAngle
  }

  /** Traverses the part of the array starting at `from` and until `end`, and
   *  returns the reduction tree for that part of the array.
   *
   *  The reduction tree is a `Leaf` if the length of the specified part of the
   *  array is smaller or equal to `threshold`, and a `Node` otherwise.
   *  If the specified part of the array is longer than `threshold`, then the
   *  work is divided and done recursively in parallel.
   */
  def upsweep(input: Array[Float], from: Int, end: Int,
    threshold: Int): Tree = {
      if(end - from <= threshold) {
        //println("Entering upsweep sequential, from = " + from + " end = " + end)
        Leaf(from, end, upsweepSequential(input, from, end))
      } else {
        val mid = from + (end - from)/2
        val a = parallel(upsweep(input,from, mid, threshold), upsweep(input, mid, end, threshold))
        Node(a._1, a._2)
      }
  }

  /** Traverses the part of the `input` array starting at `from` and until
   *  `until`, and computes the maximum angle for each entry of the output array,
   *  given the `startingAngle`.
   */
  def downsweepSequential(input: Array[Float], output: Array[Float],
    startingAngle: Float, from: Int, until: Int): Unit = {
      var i = from
      var angle = startingAngle
      while(i < until) {
        angle = max(input(i)/i, angle)
        output(i) = if(i == 0) 0 else angle
        i += 1 
      }

  }

  /** Pushes the maximum angle in the prefix of the array to each leaf of the
   *  reduction `tree` in parallel, and then calls `downsweepSequential` to write
   *  the `output` angles.
   */
  def downsweep(input: Array[Float], output: Array[Float], startingAngle: Float,
    tree: Tree): Unit = {
      tree match {
        case Node(l, r) => parallel(downsweep(input, output, startingAngle, l), downsweep(input, output, max(startingAngle, l.maxPrevious) , r))
        case Leaf(from, until, _) => downsweepSequential(input , output, startingAngle, from, until)
      }
  }

  /** Compute the line-of-sight in parallel. */
  def parLineOfSight(input: Array[Float], output: Array[Float],
    threshold: Int): Unit = {
      val tree = upsweep(input, 1, input.size, threshold)
      downsweep(input, output, 0, tree)
  }
}
