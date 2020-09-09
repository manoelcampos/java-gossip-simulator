package com.manoelcampos.gossipsimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 *
 * @param <T> the type of the data the node shares
 */
public interface GossipNode<T> extends Comparable<GossipNode<T>>{
    Logger LOGGER = LoggerFactory.getLogger(GossipNode.class.getSimpleName());

    long getId();

    /**
     * Sends a stored message to {@link GossipConfig#getFanout() N (fanout)} randomly selected nodes
     * in the {@link #getNeighbours() neighbourhood}.
     * @see #getMessage()
     * @return true if the node has some {@link #getMessage() message} to send and it was sent,
     *         false otherwise
     */
    boolean sendMessage();

    /**
     * Stores a message to be sent to nodes in the neighbourhood.
     * @param message
     * @see #sendMessage()
     */
    void setMessage(T message);

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

    /**
     * Adds a collection of nodes as neighbours
     * @param newNeighbours the nodes to add
     * @return true if the nodes were added, false if the given nodes area already in the neighbourhood
     */
    boolean addNeighbours(Collection<GossipNode<T>> newNeighbours);

    /**
     * Gets an unmodifiable Set of neighbours.
     * @return
     */
    Set<GossipNode<T>> getNeighbours();

    int getNeighbourhoodSize();

    /**
     * Gets the latest message the node is storing.
     * This data may have been generated for this node or received
     * by other nodes, in order to spread such a data through the neighbourhood.
     * @return the latest message stored or null if the node was never infected (if a message or
     *         never received from another node or manually set.
     * @see #sendMessage()
     */
    T getMessage();

    /**
     * Indicates if the node has received any data already.
     * @return
     */
    boolean isInfected();
}
