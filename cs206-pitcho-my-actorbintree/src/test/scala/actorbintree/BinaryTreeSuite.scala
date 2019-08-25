package actorbintree

import akka.actor.{ Props, ActorRef, ActorSystem }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import scala.util.Random
import scala.concurrent.duration._
import actorbintree.BinaryTreeSet._
import org.junit._
import org.junit.Assert.{ assertEquals, fail }

class BinaryTreeSuite {
  def testReceiveN(requester: TestProbe, ops: Seq[Operation], expectedReplies: Seq[OperationReply]): Unit =
    requester.within(5.seconds) {
      val repliesUnsorted = for (i <- 1 to ops.size) yield {
        try {
          requester.expectMsgType[OperationReply]
        } catch {
          case ex: Throwable if ops.size > 10 =>
            println(s"failure to receive confirmation $i/${ops.size}")
            throw ex
          case ex: Throwable =>
            println(s"failure to receive confirmation $i/${ops.size}\nRequests:" + ops.mkString("\n    ", "\n     ", ""))
            throw ex
        }
      }
      val replies = repliesUnsorted.sortBy(_.id)
      if (replies != expectedReplies) {
        val pairs = (replies zip expectedReplies).zipWithIndex filter (x => x._1._1 != x._1._2)
        fail("unexpected replies:" + pairs.map(x => s"at index ${x._2}: got ${x._1._1}, expected ${x._1._2}").mkString("\n    ", "\n    ", ""))
      }
    }

  def verify(probe: TestProbe, ops: Seq[Operation], expected: Seq[OperationReply]): Unit = {
    val system = ActorSystem.create()
    new TestKit(system) with ImplicitSender {
      val topNode = system.actorOf(Props[BinaryTreeSet])

      ops foreach { op =>
        topNode ! op
      }

      testReceiveN(probe, ops, expected)
      // the grader also verifies that enough actors are created
    }
    TestKit.shutdownActorSystem(system)
  }

  @Test def `proper inserts and lookups`: Unit = {
    val system = ActorSystem.create()
    new TestKit(system) with ImplicitSender {
      val topNode = system.actorOf(Props[BinaryTreeSet])

      topNode ! Contains(testActor, id = 1, 1)
      expectMsg(ContainsResult(1, false))

      topNode ! Insert(testActor, id = 2, 1)
      topNode ! Contains(testActor, id = 3, 1)

      expectMsg(OperationFinished(2))
      expectMsg(ContainsResult(3, true))
      ()
    }
    TestKit.shutdownActorSystem(system)
  }

  @Test def `instruction example`: Unit = {
    val system = ActorSystem.create()
    new TestKit(system) with ImplicitSender {
      val requester = TestProbe()
      val requesterRef = requester.ref
      val ops = List(
        Insert(requesterRef, id=100, 1),
        Contains(requesterRef, id=50, 2),
        Remove(requesterRef, id=10, 1),
        Insert(requesterRef, id=20, 2),
        Contains(requesterRef, id=80, 1),
        Contains(requesterRef, id=70, 2)
        )

      val expectedReplies = List(
        OperationFinished(id=10),
        OperationFinished(id=20),
        ContainsResult(id=50, false),
        ContainsResult(id=70, true),
        ContainsResult(id=80, false),
        OperationFinished(id=100)
        )
      verify(requester, ops, expectedReplies)
    }
    TestKit.shutdownActorSystem(system)
  }

  @Test def `behave identically to built-in set (includes GC) (test 1)`: Unit = behavesLikeBuiltIn()
  @Test def `behave identically to built-in set (includes GC) (test 2)`: Unit = behavesLikeBuiltIn()
  @Test def `behave identically to built-in set (includes GC) (test 3)`: Unit = behavesLikeBuiltIn()
  @Test def `behave identically to built-in set (includes GC) (test 4)`: Unit = behavesLikeBuiltIn()

  private def behavesLikeBuiltIn(): Unit = {
    val system = ActorSystem.create()
    new TestKit(system) with ImplicitSender {
      val rnd = new Random()
      def randomOperations(requester: ActorRef, count: Int): Seq[Operation] = {
        def randomElement: Int = rnd.nextInt(100)
        def randomOperation(requester: ActorRef, id: Int): Operation = rnd.nextInt(4) match {
          case 0 => Insert(requester, id, randomElement)
          case 1 => Insert(requester, id, randomElement)
          case 2 => Contains(requester, id, randomElement)
          case 3 => Remove(requester, id, randomElement)
        }

        for (seq <- 0 until count) yield randomOperation(requester, seq)
      }

      def referenceReplies(operations: Seq[Operation]): Seq[OperationReply] = {
        var referenceSet = Set.empty[Int]
        def replyFor(op: Operation): OperationReply = op match {
          case Insert(_, seq, elem) =>
            referenceSet = referenceSet + elem
            OperationFinished(seq)
          case Remove(_, seq, elem) =>
            referenceSet = referenceSet - elem
            OperationFinished(seq)
          case Contains(_, seq, elem) =>
            ContainsResult(seq, referenceSet(elem))
        }

        for (op <- operations) yield replyFor(op)
      }

      val requester = TestProbe()
      val topNode = system.actorOf(Props[BinaryTreeSet])
      val count = 1000

      val ops = randomOperations(requester.ref, count)
      val expectedReplies = referenceReplies(ops)

      ops foreach { op =>
        topNode ! op
        if (rnd.nextDouble() < 0.1) topNode ! GC
      }
      testReceiveN(requester, ops, expectedReplies)
    }
    TestKit.shutdownActorSystem(system)
  }

  @Rule def individualTestTimeout = new org.junit.rules.Timeout(10 * 1000)
}

