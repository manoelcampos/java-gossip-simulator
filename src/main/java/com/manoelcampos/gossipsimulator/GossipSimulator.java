package com.manoelcampos.gossipsimulator;

import ch.qos.logback.classic.Level;
import org.apache.commons.math3.distribution.RealDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

/**
 *
 * @param <T> the type of the data the node shares
 */
public class GossipSimulator<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(GossipSimulator.class.getSimpleName());

    private final GossipConfig config;
    private final RealDistribution random;
    private final Set<GossipNode<T>> nodes;
    private int cycles;

    public GossipSimulator(final GossipConfig config, final RealDistribution random) {
        this.config = Objects.requireNonNull(config);
        this.random = Objects.requireNonNull(random);
        this.nodes = new HashSet<>();
    }

    /**
     * Gets the set of all known {@link GossipNode}s.
     * @return
     */
    public Set<GossipNode<T>> getNodes(){
        return Collections.unmodifiableSet(nodes);
    }

    public int getNodesCount(){
        return nodes.size();
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
        final int prevSize = source.getNeighbourhoodSize();
        final int count = rand(config.getMaxNeighbours()+1);
        source.addNeighbours(randomNodes(nodes, count));
        LOGGER.debug(
                "Added {} neighbours to {} from the max of {} configured.",
                source.getNeighbourhoodSize()-prevSize, source, config.getMaxNeighbours());
    }

    /**
     * Randomly selects a given number of nodes from a set.
     * If the requested number is greater or equal to the number of available nodes,
     * there is not need to randomly select them and all available nodes are returned.
     *
     * @param availableNodes the set to randomly select nodes from
     * @param count the number of random nodes to select
     * @return the collection of randomly selected nodes
     */
    Collection<GossipNode<T>> randomNodes(final Set<GossipNode<T>> availableNodes, final int count) {
        if(count >= availableNodes.size()){
            LOGGER.debug(
                    "It was requested the selection of {} random nodes but there are only {} available. Selecting all available ones.",
                    count, availableNodes.size());
            return availableNodes;
        }

        /*An ordered set containing the indexes of the nodes selected randomly
        * from the collection of available nodes. */
        final Set<Integer> orderedIndexSet = IntStream.range(0, count)
                                                     .mapToObj(i -> rand(availableNodes.size()))
                                                     .collect(toCollection(TreeSet::new));

        int i = 0;
        final List<GossipNode<T>> selectedNodes = new ArrayList<>(count);
        final Iterator<GossipNode<T>> it = availableNodes.iterator();
        /* Since the availableNodes set doesn't allow direct (indexed) access,
         * it's used an additional loop to get the next item
         * until we reach the i'th item inside that set. */
        for (final int selectedIndex : orderedIndexSet) {
            while(it.hasNext()){
                final GossipNode<T> node = it.next();
                if(selectedIndex == i++) {
                    selectedNodes.add(node);
                    break;
                }
            }

            if(!it.hasNext()){
                return selectedNodes;
            }
        }

        return selectedNodes;
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
