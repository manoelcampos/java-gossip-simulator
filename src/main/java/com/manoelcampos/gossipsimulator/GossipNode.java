package com.manoelcampos.gossipsimulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A node that shares {@link #getMessage() data}
 * across its {@link #getNeighbors() neighborhood} using the Gossip Protocol.
 *
 * @param <T> the type of the data the node shares
 */
public interface GossipNode<T> extends Comparable<GossipNode<T>>{
    Logger LOGGER = LoggerFactory.getLogger(GossipNode.class.getSimpleName());

    long getId();

    /**
     * Gets the latest message the node is storing.
     * This data may have been generated for this node or received
     * by other nodes, in order to spread such a data through the neighborhood.
     * @return the latest message stored or null if the node was never infected (if a message or
     *         never received from another node or manually set.
     * @see #sendMessage()
     */
    T getMessage();

    /**
     * Stores a message to be sent to nodes in the neighborhood.
     * @param message
     * @see #sendMessage()
     */
    void setMessage(T message);

    /**
     * Sends a {@link #getMessage() stored message} to {@link GossipConfig#getFanout() N (fanout)} randomly selected nodes
     * in the {@link #getNeighbors() neighborhood}.
     * @return true if the node has some message to send and it was sent,
     *         false otherwise
     * @see #getMessage()
     */
    boolean sendMessage();

    /**
     * A {@link BiFunction} where you can assess the acceptance of the message.
     * That function will be called every time a message is received,
     * where the sender node and the received message is provided.
     * If you want to accept the message and let it be stored in the message attribute,
     * just make this function return true.
     * If you want to reject it, return false.
     * @param function the function to assess the acceptance of received messages
     */
    void setMessageAcceptanceFunction(BiFunction<GossipNode<T>, T, Boolean> function);

    /**
     * Adds a node as a neighbor
     * @param neighbor the node to add
     * @return true if the node was added, false it the given node is this one
     */
    boolean addNeighbor(GossipNode<T> neighbor);

    /**
     * Adds a collection of nodes as neighbors
     * @param newNeighbors the nodes to add
     * @return true if the nodes were added, false if the given nodes area already in the neighborhood
     */
    boolean addNeighbors(Collection<GossipNode<T>> newNeighbors);

    /**
     * Adds randomly selected neighbors to this node,
     * according to the {@link GossipConfig#getMaxNeighbors()},
     * to create the initial neighborhood.
     */
    void addRandomNeighbors();

    /**
     * Gets an unmodifiable Set of neighbors.
     * @return
     */
    Set<GossipNode<T>> getNeighbors();

    int getNeighborhoodSize();

    /**
     * Indicates if the node has received any data already.
     * @return
     */
    boolean isInfected();

    /**
     * Gets the Gossip simulator this node belongs to.
     * @return
     */
    GossipSimulator<T> getSimulator();
}
