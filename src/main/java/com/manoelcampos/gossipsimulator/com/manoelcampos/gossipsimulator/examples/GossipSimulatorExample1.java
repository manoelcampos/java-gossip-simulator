package com.manoelcampos.gossipsimulator.com.manoelcampos.gossipsimulator.examples;

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
    private static final int MAX_NEIGHBOURS = 20;
    private final GossipSimulator<String> simulator;
    private final List<GossipNode<String>> nodes;

    private GossipSimulatorExample1(){
        //Comment this line to use the default logging level
        GossipSimulator.setLoggerLevel(ch.qos.logback.classic.Level.INFO);

        final GossipConfig config = new GossipConfig(FANOUT, MAX_NEIGHBOURS);
        simulator = new GossipSimulator<>(config, new UniformRealDistribution());

        nodes = createNodes();
        System.out.println();
        for (int i = 0; i < CYCLES; i++) {
            System.out.println();
            simulator.run("Msg "+simulator.rand());
        }
    }

    private List<GossipNode<String>> createNodes() {
        return IntStream.range(0, NODES_COUNT)
                        .mapToObj(id -> new GossipNodeSimple<>(id, simulator))
                        .collect(toList());
    }

    public static void main(String[] args) {
        new GossipSimulatorExample1();
    }
}
