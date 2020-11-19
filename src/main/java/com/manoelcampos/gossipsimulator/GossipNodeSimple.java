package com.manoelcampos.gossipsimulator;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * A basic implementation of a {@link GossipNode}.
 * @param <T> the type of the data the node shares
 */
public class GossipNodeSimple<T> implements GossipNode<T> {
    private final GossipSimulator<T> simulator;
    private final Set<GossipNode<T>> neighbors;
    private T message;
    private long id;
    private BiFunction<GossipNode<T>, T, Boolean> messageAcceptanceFunction;

    /**
     * Instantiates a GossipNode that always accepts and stores received messages.
     * It provides an automatic ID for the node.
     * @param simulator the Gossip simulator this node will be attached to
     * @see #setMessageAcceptanceFunction(BiFunction)
     */
    public GossipNodeSimple(final GossipSimulator<T> simulator) {
        this(simulator, simulator.nextNodeId());
    }

    /**
     * Instantiates a GossipNode with a given ID, that always accepts and stores received messages.
     * @param simulator the Gossip simulator this node will be attached to
     * @param id the node ID to set
     * @see #setMessageAcceptanceFunction(BiFunction)
     */
    public GossipNodeSimple(final GossipSimulator<T> simulator, final long id) {
        this.simulator = requireNonNull(simulator);
        this.id = id;
        this.neighbors = new HashSet<>();
        this.messageAcceptanceFunction = (node, data) -> true;
        simulator.addNode(this);
    }

    @Override
    public long getId() {
        return id;
    }

    private GossipConfig config(){
        return simulator.getConfig();
    }

    @Override
    public boolean sendMessage() {
        if(message == null){
            LOGGER.warn("{} has no stored message to send", this);
            return false;
        }

        if(neighbors.isEmpty()){
            LOGGER.warn("{} has no neighbors to send messages to", this);
            return false;
        }

        /*If the number of known neighbors is lower than the number of random neighbors to select,
        * it doesn't make sense to select neighbors randomly.
        * If we have 2 neighbors and the fanout is 4, we just select all the existing neighbors to
        * send messages to.*/
        final boolean sendToAllNeighbors = neighbors.size() < config().getFanout();
        final Collection<GossipNode<T>> selectedNeighbors = sendToAllNeighbors ? neighbors : getRandomNeighbors();

        LOGGER.info(
                "{} is going to send a message to {} {} {}{}",
                this, formatCount(selectedNeighbors.size()), sendToAllNeighbors ? "existing" : "randomly selected",
                selectedNeighbors.size() > 1 ? "neighbors" : "neighbor",
                sendToAllNeighbors ? "" : " from total of " + formatCount(neighbors.size()));
        selectedNeighbors.forEach(neighbor -> ((GossipNodeSimple<T>)neighbor).receiveMessage(this, this.message));
        return true;
    }

    private String formatCount(final int count) {
        final int digits = String.valueOf(simulator.getNodesCount()).length();
        return String.format("%" + digits + "d", count);
    }

    /**
     * Gets a collection of random nodes from the neighborhood.
     * The max number of nodes to select is defined by {@link GossipConfig#getFanout()}.
     * @return
     */
    public Collection<GossipNode<T>> getRandomNeighbors() {
        return simulator.getRandomNodes(neighbors, config().getFanout());
    }

    /**
     * Receives a message from a source node and updates the list of know neighbors.
     * By default, the received message is stored in the message attribute.
     * <p>If you want to accept or not the message, provide a {@link java.util.function.BiFunction}
     * where you can assess the acceptance of the message.
     * Check {@link #setMessageAcceptanceFunction(BiFunction)}.
     * </p>
     *
     * @param source the node sending the message
     * @param data the data sent
     */
    void receiveMessage(final GossipNode<T> source, final T data) {
        //Updates the set of neighbor nodes
        neighbors.add(source);

        if(messageAcceptanceFunction.apply(source, data)) {
            this.message = data;
            LOGGER.debug("{} received message from {} accepted", this, source);
        } else LOGGER.debug("{} received message from {} wasn't accepted", this, source);
    }

    @Override
    public final void setMessageAcceptanceFunction(final BiFunction<GossipNode<T>, T, Boolean> function) {
        this.messageAcceptanceFunction = Objects.requireNonNull(function);
    }

    @Override
    public boolean addNeighbor(final GossipNode<T> neighbor) {
        if(this.equals(neighbor))
            return false;

        return neighbors.add(requireNonNull(neighbor));
    }

    @Override
    public boolean addNeighbors(final Collection<GossipNode<T>> newNeighbors) {
        /*If this node is inside the newNeighbors collection, removes it.
          A node cannot exchange messages with itself.
          If we try to remove this node from newNeighbors and
          no removal is performed or there is more than one element
          in the newNeighbors, at least one element was added to the neighborhood.
          The remove() call must be placed first to ensure we always remove
          this node from its neighborhood (if it's in there).
        */
        if(neighbors.addAll(requireNonNull(newNeighbors))){
            return !neighbors.remove(this) || newNeighbors.size() > 1;
        }

        return false;
    }

    @Override
    public void addRandomNeighbors() {
        final int prevSize = getNeighborhoodSize();
        final var config = simulator.getConfig();
        final int count = simulator.rand(config.getMaxNeighbors())+config.getMinNeighbors();
        addNeighbors(simulator.getRandomNodes(count));
        LOGGER.debug(
                "Added {} neighbors to {} from the max of {} configured.",
                getNeighborhoodSize()-prevSize, this, config.getMaxNeighbors());
    }

    @Override
    public Set<GossipNode<T>> getNeighbors() {
        return Collections.unmodifiableSet(neighbors);
    }

    @Override
    public int getNeighborhoodSize() {
        return neighbors.size();
    }

    @Override
    public T getMessage() {
        return message;
    }

    @Override
    public void setMessage(final T message) {
        this.message = message;
    }

    @Override
    public boolean isInfected() {
        return message != null;
    }

    @Override
    public String toString() {
        final int len = String.valueOf(simulator.getNodesCount()).length();
        return String.format("GossipNode %"+len+"d%s", id, isInfected() ? " 🐞" : " 💚");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        final GossipNodeSimple<?> that = (GossipNodeSimple<?>) o;
        return id == that.id && simulator.equals(that.simulator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(simulator, id);
    }

    @Override
    public int compareTo(final GossipNode<T> o) {
        return Long.compare(this.id, o.getId());
    }

    @Override
    public GossipSimulator<T> getSimulator() {
        return simulator;
    }
}
