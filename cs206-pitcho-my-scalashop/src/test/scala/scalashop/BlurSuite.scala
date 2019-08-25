package scalashop

import java.util.concurrent._
import scala.collection._
import org.junit._
import org.junit.Assert.assertEquals


class BlurSuite {

  @Rule def individualTestTimeout = new org.junit.rules.Timeout(10 * 1000)

  @Test def `Box blur on uniform 1x1` (): Unit = {
    var src = new Img(1, 1)
    src update(0,0, 1)
    var dest = new Img(1,1)
    VerticalBoxBlur.parBlur(src, dest ,1, 1)
    assertEquals(dest apply(0,0), 1)
  } 

   @Test def `Box blur on uniform 4x4`(): Unit = {
    var src = new Img(4,4)
    var dest = new Img(4,4)
    for(i <- 0 until 2) {
      for(j <- 0 until 4) {
        src update(i, j, 0 )
      }
    }

    for(i <- 2 until 4) {
      for(j <- 0 until 4) {
        src update(i, j, 2)
      }
    }
    println("Contents of image")
    println(src.toString)
    VerticalBoxBlur.parBlur(src, dest, 1, 4)
    println("In testing phase")
    for(i <- 0 until 1) {
      for(j <- 0 until 4) {
      println("(i, j) = ("+ i +", " + j +")")
      assertEquals(1, dest.apply(i,j))
      }
     }
    for(i <- 3 until 4) {
      for(j <- 0 until 4) {
        println("(i, j) = ("+i +", " + j +")")
        assertEquals(1, dest.apply(i,j))
      }
    }
    for(i <- 1 until 2) {
      for(j <- 0 until 4) {
        println("(i, j) = ("+i +", " + j +")")
        assertEquals(1, dest.apply(i,j))
      }
    }
    for(i <- 2 until 3) {
      for(j <- 0 until 4) {
        println("(i, j) = ("+i +", " + j +")")
        assertEquals(1, dest.apply(i,j))
      }
    } 
   }
  
  /* @Test def `boxBlurKernel should correctly handle radius 0`(): Unit =  {
    val src = new Img(5, 5)
    val dst = new Img(5,5)
    for {x <- 0 until 5
         y <- 0 until 5} {
      src.update(x,y, rgba(x, y, x + y, math.abs(x - y)))
         }
    HorizontalBoxBlur.parBlur(src, dst, 1, 0)
    for {x <- 0 until 5
         y <- 0 until 5} 
      assertEquals(dst.apply(x,y), rgba(x, y, x + y, math.abs(x - y)))
        //"boxBlurKernel(_,_,0) should be identity."
  }  */
  }


