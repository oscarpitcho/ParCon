/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = { 
    case o: Operation => root ! o
    case GC => {
      val newRoot = createRoot
      root ! CopyTo(newRoot)
      context become(garbageCollecting(newRoot))
    }
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    //Giving the newRoot allows copying and forwarding operations when we are done

    case op: Operation => pendingQueue = pendingQueue enqueue op //Save operations to do (and in their correct order: FIFO)
    case CopyFinished => {
      pendingQueue.foreach(newRoot ! _) //Processing all pending operations on the new root
      root ! PoisonPill
      root = newRoot //Setting the root to newRoot
      pendingQueue = Queue.empty //Removing all awaiting operations
      context become normal //Popping the GC state
    }
  }
}


object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode],  elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  def nextNode(e: Int): Position = if(e > elem) Right else Left

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = { 
    case Insert(r, id, e) => { 
      if(e != elem || (elem == e && removed)) {
        subtrees get nextNode(e) match {
          case None => {
              val newNode = props(e, false)
              subtrees += (nextNode(e) -> context.actorOf(newNode))
              r ! OperationFinished(id)
          }
          case Some(node) => node ! Insert(r, id, e)
      }
     } else r ! OperationFinished(id)
    }


    case Contains(r, id, e) =>
      if (e != elem || (e == elem && removed)) subtrees get nextNode(e) match {
        case Some(subtree) => subtree ! Contains(r, id, e)
        case None => r ! ContainsResult(id, result = false)
      } else r ! ContainsResult(id, result = true)

    case Remove(r, id, e) => {
        if(e != elem || (elem == e && removed) ) { 
         subtrees get nextNode(e) match {
            case None => r ! OperationFinished(id)
            case Some(node) => node ! Remove(r, id, e)
          }
        } else {
          removed = true
          r ! OperationFinished(id)

        }
    }
    case CopyTo(treeNode) => {
      if(!removed) treeNode ! Insert(self, elem, elem)
      val children = subtrees.values
      children.foreach(_ ! CopyTo(treeNode))
      if (removed && subtrees.isEmpty) {
        context.parent ! CopyFinished
      }
      else context become copying(subtrees.values.toSet, insertConfirmed = removed)
    }
  }
  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(_) =>
      if (expected.isEmpty) {
        context.parent ! CopyFinished
      } else context become copying(expected, insertConfirmed = true)

    case CopyFinished =>{
      val waitingFor: Set[ActorRef] = expected - sender
      if (insertConfirmed && waitingFor.isEmpty) {
        context.parent ! CopyFinished
      } else context become copying(waitingFor, insertConfirmed)
    }
  }
}

