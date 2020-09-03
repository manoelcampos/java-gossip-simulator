package com.manoelcampos.gossipsimulator;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class GossipNodeSimple<T> implements GossipNode<T> {
    private final GossipSimulator<T> simulator;
    private final List<GossipNode<T>> neighbours;
    private T latestData;
    private long id;

    public GossipNodeSimple(final long id, final GossipSimulator<T> simulator) {
        this.simulator = Objects.requireNonNull(simulator);
        simulator.addNode(this);
        this.neighbours = new LinkedList<>();
        this.id = id;
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
        final List<GossipNode<T>> selected = sendToAllNeighbours ? neighbours : getRandomNodes();

        LOGGER.info(
                "{} is going to send a message to {} {} {}{}",
                this, selected.size(), sendToAllNeighbours ? "existing" : "randomly selected",
                selected.size() > 1 ? "neighbours" : "neighbour",
                sendToAllNeighbours ? "" : String.format(" from total of %d", neighbours.size()));
        selected.forEach(node -> node.receiveMessage(this, data));
    }

    private List<GossipNode<T>> getRandomNodes() {
        final int iterations = Math.min(config().getFanout(), neighbours.size());
        return IntStream.range(0, iterations).mapToObj(i -> simulator.randomNode(neighbours)).collect(toList());
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

        return neighbours.add(Objects.requireNonNull(neighbour));
    }

    @Override
    public List<GossipNode<T>> getNeighbours() {
        return Collections.unmodifiableList(neighbours);
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
        return String.format("GossipNode %d%s", id, isInfected() ? " 🐞" : "");
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
