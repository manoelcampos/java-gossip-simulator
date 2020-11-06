package com.manoelcampos.gossipsimulator;

import ch.qos.logback.classic.Level;
import org.apache.commons.math3.distribution.RealDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

/**
 * Simulates the dissemination of data across a
 * network of Gossip nodes.
 *
 * @param <T> the type of the data the node shares
 * @see #run()
 */
public class GossipSimulator<T> {
    public static final Logger LOGGER = LoggerFactory.getLogger(GossipSimulator.class.getSimpleName());

    private final GossipConfig config;
    private final RealDistribution random;
    private final List<GossipNode<T>> nodes;
    private int cycles;
    private int lastNodeId;

    public GossipSimulator(final GossipConfig config, final RealDistribution random) {
        this.config = Objects.requireNonNull(config);
        this.random = Objects.requireNonNull(random);
        this.nodes = new LinkedList<>();
    }

    /**
     * Gets the last node id and increments its value.
     * @return
     */
    int nextNodeId(){
        return lastNodeId++;
    }

    /**
     * Gets the list of all known {@link GossipNode}s.
     * It's not ensured the list has only unique nodes.
     * You have to ensure that by not creating nodes
     * with the same id.
     *
     * <p>
     * A {@link Set} is not used here to reduce time
     * complexity if you want to randomly select nodes
     * from the {@link #getNodes() list}.
     * Selecting random nodes from that list can be used,
     * for instance, if you want to infect multiple
     * nodes randomly before simulation startup.
     * </p>
     * @return
     */
    public List<GossipNode<T>> getNodes(){
        return Collections.unmodifiableList(nodes);
    }

    public int getNodesCount(){
        return nodes.size();
    }

    /**
     * Adds a node to the simulator.
     * You have to ensure not duplicated node is inserted.
     * @param node the node to add
     */
    final void addNode(final GossipNode<T> node) {
        nodes.add(Objects.requireNonNull(node));
    }

    public long getInfectedNodesNumber(){
        return nodes.stream().filter(GossipNode::isInfected).count();
    }

    public GossipConfig getConfig() {
        return config;
    }

    /**
     * Runs a {@link #getCycles() cycle} of the Gossip transmissions,
     * making all infected nodes to send the data to their neighbours.
     * If you want to run multiple cycles, you need to call this method
     * inside a loop with the stop condition you want.
     * For instance, you may want to run a fixed number of cycles
     * or while there are non-infected nodes.
     * @throws IllegalStateException
     * @see #getInfectedNodesNumber()
     */
    public void run() {
        if(cycles++ == 0){
            if(nodes.size() <= config.getMaxNeighbours()) {
                throw new IllegalStateException(
                        String.format(
                            "The number of existing nodes (%d) must be higher than the number of neighbours by node (%d).",
                            nodes.size(), config.getMaxNeighbours()));
            }

            nodes.forEach(this::addRandomNeighbours);
        }

        LOGGER.info("Running simulation cycle {}", cycles);
        final long messagesSent = nodes.stream()
                                       .filter(GossipNode::isInfected)
                                       .filter(GossipNode::sendMessage)
                                       .count();
        if(messagesSent == 0) {
            LOGGER.warn(
                    "No message was sent by the {} nodes because there is no infected node or their neighbourhood is empty.",
                    nodes.size());
        } else LOGGER.info(
                "Number of infected nodes 🐞 after sending messages to {} nodes: {} of {} (cycle {})",
                messagesSent, getInfectedNodesNumber(), nodes.size(), cycles);
    }

    /**
     * Adds randomly selected neighbours to a source node,
     * according to the {@link GossipConfig#getMaxNeighbours()},
     * to create the initial neighborhood.
     *
     * @param source the node to add neighbours to
     */
    private void addRandomNeighbours(final GossipNode<T> source) {
        final int prevSize = source.getNeighbourhoodSize();
        final int count = rand(config.getMaxNeighbours()+1);
        source.addNeighbours(getRandomNodes(count));
        LOGGER.debug(
                "Added {} neighbours to {} from the max of {} configured.",
                source.getNeighbourhoodSize()-prevSize, source, config.getMaxNeighbours());
    }

    /**
     * Randomly selects a given number of nodes from the
     * list of all available nodes.
     * If the requested number is greater or equal to the number of available nodes,
     * there is not need to randomly select them and all available nodes are returned.
     *
     * @param count the number of random nodes to select
     * @return the collection of randomly selected nodes
     */
    public Collection<GossipNode<T>> getRandomNodes(final int count) {
        return getRandomNodes(nodes, count);
    }

    /**
     * Randomly selects a given number of nodes from a collection.
     * If the requested number is greater or equal to the number of available nodes,
     * there is not need to randomly select them and all available nodes are returned.
     *
     * @param sourceNodes the collection to randomly select nodes from
     * @param count the number of random nodes to select
     * @return the collection of randomly selected nodes
     */
    public Collection<GossipNode<T>> getRandomNodes(final Collection<GossipNode<T>> sourceNodes, final int count) {
        if(count >= sourceNodes.size()){
            LOGGER.debug(
                    "It was requested the selection of {} random nodes but there are only {} available. Selecting all available ones.",
                    count, sourceNodes.size());
            return sourceNodes;
        }

        if(sourceNodes instanceof List<GossipNode<T>> list){
            return randomNodesFromList(list, count);
        }

        return randomNodesFromCollection(sourceNodes, count);
    }

    private List<GossipNode<T>> randomNodesFromList(final List<GossipNode<T>> list, final int count) {
        return IntStream.range(0, count)
                  .map(i -> rand(list.size()))
                  .mapToObj(list::get)
                  .collect(toList());
    }

    private List<GossipNode<T>> randomNodesFromCollection(final Collection<GossipNode<T>> availableNodes, final int count) {
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
     * @see #run()
     */
    public int getCycles() {
        return cycles;
    }

    /**
     * Checks if the simulation has already started,
     * that is: if it has already run any cycle.
     * @return
     */
    public boolean isStarted(){
        return cycles > 0;
    }

}
