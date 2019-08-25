package lockfree

import scala.annotation.tailrec

class SortedList extends AbstractSortedList {

  // The sentinel node at the head.
  private val _head: Node = createNode(0, None, isHead = true)

  // The first logical node is referenced by the head.
  def firstNode: Option[Node] = _head.next

  // Finds the first node whose value satisfies the predicate.
  // Returns the predecessor of the node and the node.
  def findNodeWithPrev(pred: Int => Boolean): (Node, Option[Node]) = {
    def findNodeWithPrevAux(prev: Node, node: Option[Node]): (Node, Option[Node]) = (prev, node) match {
      case (n, None) => (n, None)
      case (n, Some(m)) => 
        if(m deleted) {
          n.atomicState.compareAndSet((Some(m), false), (m.next, false))
          findNodeWithPrev(pred) 
        } else if (pred(m.value)) (n,Some(m))
        else findNodeWithPrevAux(m,m.next)
  }
    findNodeWithPrevAux(_head, firstNode)
  }

  // Insert an element in the list.
  def insert(e: Int): Unit = {
    val (prev, node) = findNodeWithPrev(_ >= e)
    val newNode = createNode(e, node, false)
    if(!prev.atomicState.compareAndSet((node, false), (Some(newNode) ,false))) insert(e)
  }

  // Checks if the list contains an element.
  def contains(e: Int): Boolean = findNodeWithPrev(_ == e) match {
    case (_, None) => false
    case (prev, Some(n)) => !n.deleted
  }

  // Delete an element from the list.
  // Should only delete one element when multiple occurences are present.
  def delete(e: Int): Boolean =  findNodeWithPrev(_ == e)._2 match {
    case Some(n) => if(n mark) true else delete(e) //It is the first time we find this node to delete
    case None => false
  }
  }

