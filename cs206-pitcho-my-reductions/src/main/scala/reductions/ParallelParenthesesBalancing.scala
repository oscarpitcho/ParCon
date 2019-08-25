package reductions

import scala.annotation._
import org.scalameter._

object ParallelParenthesesBalancingRunner {

  @volatile var seqResult = false

  @volatile var parResult = false

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 40,
    Key.exec.maxWarmupRuns -> 80,
    Key.exec.benchRuns -> 120,
    Key.verbose -> false
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val length = 100000000
    val chars = new Array[Char](length)
    val threshold = 10000
    val seqtime = standardConfig measure {
      seqResult = ParallelParenthesesBalancing.balance(chars)
    }
    println(s"sequential result = $seqResult")
    println(s"sequential balancing time: $seqtime")

    val fjtime = standardConfig measure {
      parResult = ParallelParenthesesBalancing.parBalance(chars, threshold)
    }
    println(s"parallel result = $parResult")
    println(s"parallel balancing time: $fjtime")
    println(s"speedup: ${seqtime.value / fjtime.value}")
  }
}

object ParallelParenthesesBalancing {

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def balance(chars: Array[Char]): Boolean = {
  
    def balanceRecursion(lOpen: Int, chars: List[Char]): Boolean = {
      if (lOpen < 0) false 
      else chars match {
        case x::xs => x match {
          case '(' => balanceRecursion(lOpen + 1, chars.tail)
          case ')' => balanceRecursion(lOpen - 1, chars.tail)
          case _ =>  balanceRecursion(lOpen, chars.tail)
        }
        case Nil => if(lOpen > 0) false else true
      }
  }
  balanceRecursion(0, chars.toList)
  }

  /** Returns `true` iff the parentheses in the input `chars` are balanced.
   */
  def parBalance(chars: Array[Char], threshold: Int): Boolean = {

    def traverse(idx: Int, until: Int, o: Int, c: Int): (Int, Int) = {
      if(idx == until) {(o, c)} //Amount of excess parenthesis in the interval
      else {
         chars(idx) match {
          case '(' => traverse(idx + 1, until, o + 1,  c)
          case ')' => 
              if (o > 0) traverse(idx + 1, until, o - 1, c) //There are open parenthesis to close
              else traverse(idx + 1, until, o, c + 1) //No open parenthesis to close
          case _ => traverse(idx + 1, until, o, c)  //Character is not a parenthesis and can be ignored
      }
    }
    }

    def reduce(from: Int, until: Int): (Int, Int) = {
      if(until - from <=  threshold) traverse(from, until, 0, 0)
      else {
        val mid = from + (until - from)/2
        val ((a1, a2), (b1, b2)) = parallel(reduce(from, mid), reduce(mid, until))
        if(a1 > b2)  // more excess opening parenthesis in first half than excess closing parenthesis in second half
          (a1 - b2 + b1, a2) 
        else 
          (b1, b2 - a1 + a2)//Similarly we match the outer parenthesis 1st remaining are excess parenthesis in the second half
      } 
    }

  reduce(0, chars.length) == (0, 0)
  }
}

  // For those who want more:
  // Prove that your reduction operator is associative!


