package com.manoelcampos.gossipsimulator;

import java.util.Objects;

/**
 * Send messages stored in infected nodes and
 * keep track of statistics about the transmission of those messages in a simulation round.
 * @param <T> the type of the data the node shares, the must be the same of the {@link GossipSimulator}.
 *
 * @author Manoel Campos da Silva Filho
 */
class InfectionTracker<T> {
    private final GossipSimulator<T> simulator;
    private long sentMsgNumber = 0;
    private long infectedNodesNumber = 0;
    private long nodesWithNeighborsNumber = 0;

    InfectionTracker(final GossipSimulator<T> simulator) {
        this.simulator = Objects.requireNonNull(simulator);
    }

    /**
     * Sends the message stored in each available node,
     * storing some statistics.
     * @return true if any message was sent, false otherwise
     */
    boolean sendMessages(){
        for (final GossipNode<T> node : simulator.getNodes()) {
            if (node.isInfected()) {
                infectedNodesNumber++;
                if(node.hasNeighbors()) {
                    nodesWithNeighborsNumber++;
                }

                if (node.sendMessage()) {
                    sentMsgNumber++;
                }
            }
        }

        return sentMsgNumber > 0;
    }

    /**
     * Gets the number of messages sent.
     * @return
     */
    public long getSentMsgNumber() {
        return sentMsgNumber;
    }

    /**
     * Gets the number of infected nodes.
     * @return
     */
    public long getInfectedNumber() {
        return infectedNodesNumber;
    }

    /**
     * Gets the number of nodes that have any neighbors.
     * @return
     */
    public long getNodesWithNeighborsNumber() {
        return nodesWithNeighborsNumber;
    }

    /**
     * Gets a string indicating the reason no message was sent.
     * @return
     */
    String getNoMsgSentReason() {
        var reason = getInfectedNumber() == 0 ? "there is no infected node" : "";
        if(getNodesWithNeighborsNumber() == 0) {
            reason += reason.isEmpty() ? "" : " and ";
            reason += "their neighborhood is empty";
        }

        return reason;
    }
}
