package com.manoelcampos.gossipsimulator;

import ch.qos.logback.classic.Level;
import org.apache.commons.math3.distribution.RealDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 *
 * @param <T> the type of the data the node shares
 */
public class GossipSimulator<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(GossipSimulator.class.getSimpleName());

    private final GossipConfig config;
    private final RealDistribution random;
    private final List<GossipNode<T>> nodes;
    private int cycles;

    public GossipSimulator(final GossipConfig config, final RealDistribution random) {
        this.config = Objects.requireNonNull(config);
        this.random = Objects.requireNonNull(random);
        this.nodes = new LinkedList<>();
    }

    /**
     * Gets the list of all known {@link GossipNode}s.
     * @return
     */
    public List<GossipNode<T>> getNodes(){
        return Collections.unmodifiableList(nodes);
    }

    void addNode(final GossipNode<T> neighbour) {
        nodes.add(Objects.requireNonNull(neighbour));
    }

    public long getInfectedNodesNumber(){
        return nodes.stream().filter(GossipNode::isInfected).count();
    }

    public GossipConfig getConfig() {
        return config;
    }

    /**
     * Runs a cycle of the Gossip transmissions,
     * making all infected nodes to send the data
     * to their neighbours.
     * @param message the message to spread to neighbours
     * @throws IllegalStateException
     */
    public void run(final T message) {
        if(cycles == 0){
            if(nodes.size() <= config.getMaxNeighbours()) {
                throw new IllegalStateException(
                        String.format(
                            "The number of existing nodes (%d) must be higher than the number of neighbours by node (%d).",
                            nodes.size(), config.getMaxNeighbours()));
            }

            nodes.forEach(this::addRandomNeighbours);
        }

        cycles++;
        LOGGER.info("Running simulation cycle {}", cycles);
        nodes.forEach(node -> node.sendMessage(message));
        LOGGER.info(
                "Number of infected nodes 🐞 after {} {}: {} of {}",
                cycles, cycles > 1 ? "cycles" : "cycle", getInfectedNodesNumber(), nodes.size());
    }

    /**
     * Adds randomly selected neighbours to a source node,
     * according to the {@link GossipConfig#getMaxNeighbours()}.
     *
     * @param source the node to add neighbours to
     */
    private void addRandomNeighbours(final GossipNode<T> source) {
        final int iterations = rand(config.getMaxNeighbours()+1);
        IntStream.range(0, iterations)
                 .mapToObj(i -> randomNode(nodes))
                 .forEach(source::addNeighbour);
        LOGGER.debug(
                "Added {} neighbours to {} from the max of {} configured.",
                source.getNeighbourhoodSize(), source, config.getMaxNeighbours());
    }

    GossipNode<T> randomNode(final List<GossipNode<T>> list) {
        final int i = rand(list.size());
        return list.get(i);
    }

    /**
     * Returns a random double value between [0..1[.
     * @return
     */
    public double rand() {
        return random.sample();
    }

    /**
     * Returns a random int value between [0..max[.
     * @return
     */
    public int rand(final int max) {
        if(max <= 0){
            throw new IllegalArgumentException("Max must be greater than 0.");
        }

        return (int)Math.floor(random.sample() * max);
    }

    public static void setLoggerLevel(final Level level){
        final Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ((ch.qos.logback.classic.Logger) root).setLevel(level);
    }

    /**
     * Gets the number of cycles up to now.
     * @return
     * @see #run(Object)
     */
    public int getCycles() {
        return cycles;
    }

}
