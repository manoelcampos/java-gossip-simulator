package com.manoelcampos.gossipsimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * @param <T> the type of the data the node shares
 */
public interface GossipNode<T> extends Comparable<GossipNode<T>>{
    Logger LOGGER = LoggerFactory.getLogger(GossipNode.class.getSimpleName());

    long getId();

    /**
     * Sends a message to {@link GossipConfig#getFanout() N (fanout)} randomly selected nodes
     * in the {@link #getNeighbours() neighbourhood}.
     * @param data
     */
    void sendMessage(T data);

    /**
     * Receives a message from a source node and updates the list of know neighbours.
     * @param source the node sending the message
     * @param data the data sent
     */
    void receiveMessage(GossipNode<T> source, T data);

    /**
     * Adds a node as a neighbour
     * @param neighbour the node to add
     * @return true if the node was added, false it the given node is this one
     */
    boolean addNeighbour(GossipNode<T> neighbour);

    List<GossipNode<T>> getNeighbours();

    int getNeighbourhoodSize();

    T getLatestData();

    /**
     * Indicates if the node has received any data already.
     * @return
     */
    boolean isInfected();
}
