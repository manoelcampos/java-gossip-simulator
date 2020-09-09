package com.manoelcampos.gossipsimulator;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class GossipNodeSimple<T> implements GossipNode<T> {
    private final GossipSimulator<T> simulator;
    private final Set<GossipNode<T>> neighbours;
    private T latestData;
    private long id;

    public GossipNodeSimple(final long id, final GossipSimulator<T> simulator) {
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
    public void sendMessage(final T data) {
        if(neighbours.isEmpty()){
            LOGGER.warn("{} has no neighbours to send messages to", this);
            return;
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
        selected.forEach(node -> node.receiveMessage(this, data));
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
        this.latestData = data;
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
    public T getLatestData() {
        return latestData;
    }

    @Override
    public boolean isInfected() {
        return latestData != null;
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
}
