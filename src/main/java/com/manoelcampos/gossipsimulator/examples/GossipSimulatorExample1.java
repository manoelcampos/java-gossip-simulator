package com.manoelcampos.gossipsimulator.examples;

import com.manoelcampos.gossipsimulator.GossipConfig;
import com.manoelcampos.gossipsimulator.GossipNode;
import com.manoelcampos.gossipsimulator.GossipNodeSimple;
import com.manoelcampos.gossipsimulator.GossipSimulator;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class GossipSimulatorExample1 {
    private static final int NODES_COUNT = 40;
    private static final int CYCLES = 10;
    private static final int FANOUT = 4;
    private static final int MAX_NEIGHBORS = 20;
    private final GossipSimulator<String> simulator;
    private final List<GossipNode<String>> nodes;

    private GossipSimulatorExample1(){
        //Comment this line to use the default logging level
        GossipSimulator.setLoggerLevel(ch.qos.logback.classic.Level.INFO);

        final GossipConfig config = new GossipConfig(FANOUT, MAX_NEIGHBORS);
        simulator = new GossipSimulator<>(config, new UniformRealDistribution());

        nodes = createNodes();
        infectRandomNode("Msg " + simulator.rand());

        nodes.forEach(GossipNode::addRandomNeighbors);

        System.out.println();
        for (int i = 0; i < CYCLES; i++) {
            System.out.println();
            simulator.run();
        }
    }

    /**
     * Randomly selects a node to infect, that is, to store a message
     * to be disseminated.
     * @param message the message to be disseminated
     */
    private void infectRandomNode(final String message) {
        final int randIndex = simulator.rand(NODES_COUNT);
        final GossipNode<String> node = nodes.get(randIndex);
        node.setMessage(message);
    }

    private List<GossipNode<String>> createNodes() {
        return IntStream.range(0, NODES_COUNT)
                        .mapToObj(id -> new GossipNodeSimple<>(simulator, id))
                        .collect(toList());
    }

    public static void main(String[] args) {
        new GossipSimulatorExample1();
    }
}
