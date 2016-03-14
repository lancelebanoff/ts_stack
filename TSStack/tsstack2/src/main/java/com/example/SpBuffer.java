package com.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * SpBuffer is a single producer, multiple consumer linked list of nodes.
 * Each thread is a producer for one SpBuffer, and all threads are consumers.
 */
public class SpBuffer {

    Node top;
    private int id;

    //Initialize the buffer with one node which points to itself
    public SpBuffer() {
        id = -1;
        Node sentinel = new Node(null, true);
        sentinel.next = sentinel;
        top = sentinel;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    //Inserts a node into the SpBuffer
    public void insSp(TimestampedItem item) {
        Node newNode = new Node(item, false);
        Node topMost = top;
        while(topMost.next != topMost && topMost.taken.get()) {
            topMost = topMost.next;
        }
        newNode.next = topMost;
        top = newNode;
    }

    //Finds the topmost node in this buffer that has not been taken
    public NodePair getSp() {
        Node oldTop = top;
        Node result = oldTop;
        while(true) {
            if(result == null) {
                //There are no available nodes in this buffer
                return new NodePair(null, oldTop);
            }
            if(!result.taken.get()) {
                //We found an item in the buffer that has not yet been taken
                return new NodePair(result, oldTop);
            }
            else if(result.next == result) {
                //We have reached the sentinel node, so this buffer is empty
                return new NodePair(null, oldTop);
            }
            else {
                result = result.next;
            }
        }
    }

    //Try to remove the node from this buffer
    public boolean tryRemSP(Node oldTop, Node node) {
        if(node.taken.compareAndSet(false, true)) {
            synchronized (this) {
                //Set the top reference to the node being removed, since all nodes above it have been taken.
                AtomicReference<Node> topRef = new AtomicReference<>(top);
                topRef.compareAndSet(oldTop, node);
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String s = "";
        Node node = top;
        while(node.next != node) {
            s += node + ", ";
            node = node.next;
        }
        return s;
    }
}


class Node {
    Node next;
    TimestampedItem item;
    AtomicBoolean taken;

    public Node(TimestampedItem item, boolean taken) {
        this.item = item;
        this.taken = new AtomicBoolean(taken);
    }

    @Override
    public String toString() {
        String s = taken.get() ? "T" : "";
        return s + item.data.toString();
    }
}

class NodePair {
    Node result;
    Node oldTop;

    public NodePair(Node result, Node oldTop) {
        this.result = result;
        this.oldTop = oldTop;
    }
}