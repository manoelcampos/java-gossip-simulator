package com.manoelcampos.gossipsimulator;

import java.util.*;

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

    public GossipNodeSimple(final GossipSimulator<T> simulator) {
        this(simulator, simulator.nextNodeId());
    }

    public GossipNodeSimple(final GossipSimulator<T> simulator, final long id) {
        this.simulator = requireNonNull(simulator);
        this.id = id;
        this.neighbours = new HashSet<>();
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
        final Collection<GossipNode<T>> selected = sendToAllNeighbours ? neighbours : getRandomNodes();

        LOGGER.info(
                "{} is going to send a message to {} {} {}{}",
                this, formatCount(selected.size()), sendToAllNeighbours ? "existing" : "randomly selected",
                selected.size() > 1 ? "neighbours" : "neighbour",
                sendToAllNeighbours ? "" : " from total of " + formatCount(neighbours.size()));
        selected.forEach(node -> node.receiveMessage(this, message));
        return true;
    }

    private String formatCount(final int count) {
        final int digits = String.valueOf(simulator.getNodesCount()).length();
        return String.format("%" + digits + "d", count);
    }

    private Collection<GossipNode<T>> getRandomNodes() {
        return simulator.randomNodes(neighbours, config().getFanout());
    }

    @Override
    public void receiveMessage(final GossipNode<T> source, final T data) {
        //Updates the set of neighbour nodes
        neighbours.add(source);
        this.message = data;
        LOGGER.debug("{} received message from {}", this, source);
    }

    @Override
    public boolean addNeighbour(final GossipNode<T> neighbour) {
        if(this.equals(neighbour))
            return false;

        return neighbours.add(requireNonNull(neighbour));
    }

    @Override
    public boolean addNeighbours(final Collection<GossipNode<T>> newNeighbours) {
        return neighbours.addAll(requireNonNull(newNeighbours));
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
