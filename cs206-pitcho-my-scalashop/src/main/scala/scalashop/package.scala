import java.util.concurrent._
import scala.util.DynamicVariable

import org.scalameter._

package object scalashop {

  /** The value of every pixel is represented as a 32 bit integer. */
  type RGBA = Int

  /** Returns the red component. */
  def red(c: RGBA): Int = (0xff000000 & c) >>> 24

  /** Returns the green component. */
  def green(c: RGBA): Int = (0x00ff0000 & c) >>> 16

  /** Returns the blue component. */
  def blue(c: RGBA): Int = (0x0000ff00 & c) >>> 8

  /** Returns the alpha component. */
  def alpha(c: RGBA): Int = (0x000000ff & c) >>> 0

  /** Used to create an RGBA value from separate components. */
  def rgba(r: Int, g: Int, b: Int, a: Int): RGBA = {
    (r << 24) | (g << 16) | (b << 8) | (a << 0)
  }

  /** Restricts the integer into the specified range. */
  def clamp(v: Int, min: Int, max: Int): Int = {
    if (v < min) min
    else if (v > max) max
    else v
  }

  /** Image is a two-dimensional matrix of pixel values. */
   class Img(val width: Int, val height: Int, private val data: Array[RGBA]) {
    def this(w: Int, h: Int) = this(w, h, new Array(w * h))
    def apply(x: Int, y: Int): RGBA = data(y * width + x)
    def update(x: Int, y: Int, c: RGBA): Unit = data(y * width + x) = c

    override def toString(): String = {
      var string = ""
      for(j <- 0 until height) {
        for (i <- 0 until width) {
          string += this.apply(i,j)
        }
        string += "\n"
      }
      string
    }
  }

  /** Computes the blurred RGBA value of a single pixel of the input image. */
   def boxBlurKernel(src: Img, x: Int, y: Int, radius: Int): RGBA = {
    val xMin = clamp(x - radius, 0 , src.width - 1)
    val xMax = clamp(x + radius, 0 , src.width - 1)
    val yMin = clamp(y - radius, 0 , src.height - 1)
    val yMax = clamp(y + radius, 0 , src.height - 1)
    var xPos = xMin
    var yPos = yMin
    var sums = new Array[RGBA](4)
    var count = 0
    while(xPos <= xMax) {
      while(yPos <= yMax) {
        val content = src apply(xPos, yPos)
        sums(0) += red(content)
        sums(1) += green(content)
        sums(2) += blue(content)
        sums(3) += alpha(content)
        yPos += 1
        count += 1
      }
      yPos = yMin
      xPos += 1 
    }

    sums = sums.map(_ / count)
    println("In box blur kernel")
    println("(i, j) = ("+ x +", " + y +")")
    println("Sum value = " + sums(3))
    println("Pixel value = " + sums(3))
    println()
    rgba(sums(0),sums(1),sums(2),sums(3))
  }

  val forkJoinPool = new ForkJoinPool

  abstract class TaskScheduler {
    def schedule[T](body: => T): ForkJoinTask[T]
    def parallel[A, B](taskA: => A, taskB: => B): (A, B) = {
      val right = task {
        taskB
      }
      val left = taskA
      (left, right.join())
    }
  }

  class DefaultTaskScheduler extends TaskScheduler {
    def schedule[T](body: => T): ForkJoinTask[T] = {
      val t = new RecursiveTask[T] {
        def compute = body
      }
      Thread.currentThread match {
        case wt: ForkJoinWorkerThread =>
          t.fork()
        case _ =>
          forkJoinPool.execute(t)
      }
      t
    }
  }

  val scheduler =
    new DynamicVariable[TaskScheduler](new DefaultTaskScheduler)

  def task[T](body: => T): ForkJoinTask[T] = {
    scheduler.value.schedule(body)
  }

  def parallel[A, B](taskA: => A, taskB: => B): (A, B) = {
    scheduler.value.parallel(taskA, taskB)
  }

  def parallel[A, B, C, D](taskA: => A, taskB: => B, taskC: => C, taskD: => D): (A, B, C, D) = {
    val ta = task { taskA }
    val tb = task { taskB }
    val tc = task { taskC }
    val td = taskD
    (ta.join(), tb.join(), tc.join(), td)
  }

  // Workaround Dotty's handling of the existential type KeyValue
  implicit def keyValueCoerce[T](kv: (Key[T], T)): KeyValue = {
    kv.asInstanceOf[KeyValue]
  }
}


