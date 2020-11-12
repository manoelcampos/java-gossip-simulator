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
    private final Set<GossipNode<T>> neighbours;
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
        this.neighbours = new HashSet<>();
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

        if(neighbours.isEmpty()){
            LOGGER.warn("{} has no neighbours to send messages to", this);
            return false;
        }

        /*If the number of known neighbours is lower than the number of random neighbours to select,
        * it doesn't make sense to select neighbours randomly.
        * If we have 2 neighbours and the fanout is 4, we just select all the existing neighbours to
        * send messages to.*/
        final boolean sendToAllNeighbours = neighbours.size() < config().getFanout();
        final Collection<GossipNode<T>> selectedNeighbours = sendToAllNeighbours ? neighbours : getRandomNeighbours();

        LOGGER.info(
                "{} is going to send a message to {} {} {}{}",
                this, formatCount(selectedNeighbours.size()), sendToAllNeighbours ? "existing" : "randomly selected",
                selectedNeighbours.size() > 1 ? "neighbours" : "neighbour",
                sendToAllNeighbours ? "" : " from total of " + formatCount(neighbours.size()));
        selectedNeighbours.forEach(neighbour -> ((GossipNodeSimple<T>)neighbour).receiveMessage(this, this.message));
        return true;
    }

    private String formatCount(final int count) {
        final int digits = String.valueOf(simulator.getNodesCount()).length();
        return String.format("%" + digits + "d", count);
    }

    /**
     * Gets a collection of random nodes from the neighbourhood.
     * The max number of nodes to select is defined by {@link GossipConfig#getFanout()}.
     * @return
     */
    public Collection<GossipNode<T>> getRandomNeighbours() {
        return simulator.getRandomNodes(neighbours, config().getFanout());
    }

    /**
     * Receives a message from a source node and updates the list of know neighbours.
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
        //Updates the set of neighbour nodes
        neighbours.add(source);

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
    public boolean addNeighbour(final GossipNode<T> neighbour) {
        if(this.equals(neighbour))
            return false;

        return neighbours.add(requireNonNull(neighbour));
    }

    @Override
    public boolean addNeighbours(final Collection<GossipNode<T>> newNeighbours) {
        /*If this node is inside the newNeighbours collection, removes it.
          A node cannot exchange messages with itself.
          If we try to remove this node from newNeighbours and
          no removal is performed or there is more than one element
          in the newNeighbours, at least one element was added to the neighbourhood.
          The remove() call must be placed first to ensure we always remove
          this node from its neighborhood (if it's in there).
        */
        if(neighbours.addAll(requireNonNull(newNeighbours))){
            return !neighbours.remove(this) || newNeighbours.size() > 1;
        }

        return false;
    }

    @Override
    public void addRandomNeighbors() {
        final int prevSize = getNeighbourhoodSize();
        final var config = simulator.getConfig();
        final int count = simulator.rand(config.getMaxNeighbors())+config.getMinNeighbors();
        addNeighbours(simulator.getRandomNodes(count));
        LOGGER.debug(
                "Added {} neighbours to {} from the max of {} configured.",
                getNeighbourhoodSize()-prevSize, this, config.getMaxNeighbors());
    }

    @Override
    public Set<GossipNode<T>> getNeighbours() {
        return Collections.unmodifiableSet(neighbours);
    }

    @Override
    public int getNeighbourhoodSize() {
        return neighbours.size();
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
