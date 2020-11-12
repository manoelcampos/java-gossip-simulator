package com.manoelcampos.gossipsimulator;

/**
 * Gossip Protocol configuration.
 * It defines a set of parameters used to run a simulation.
 */
public class GossipConfig {
    private final int fanout;
    private int minNeighbors;
    private int maxNeighbors;

    /**
     * Instantiates a gossip config.
     * @param fanout the number of nodes to randomly spread information to
     * @param maxNeighbors the maximum number of neighbours a node will be linked to.
     */
    public GossipConfig(final int fanout, final int maxNeighbors) {
        if(fanout <= 0){
            throw new IllegalArgumentException("Fanout must be greater than 0.");
        }

        if(maxNeighbors <= 0){
            throw new IllegalArgumentException("Max number of neighbours must be greater than 0.");
        }

        if(maxNeighbors <= fanout){
            throw new IllegalArgumentException(
                String.format(
                        "Max number of neighbours (%d) must be greater than the fanout (%d).",
                        maxNeighbors, fanout));
        }

        this.fanout = fanout;
        this.maxNeighbors = maxNeighbors;
    }

    /**
     * Gets the number of nodes to randomly spread information to
     * @return
     */
    public int getFanout() {
        return fanout;
    }

    /**
     * Gets the maximum number of neighbours a node will be linked to.
     * @return
     */
    public int getMaxNeighbors() {
        return maxNeighbors;
    }

    final void setMaxNeighbors(final int maxNeighbors) {
        this.maxNeighbors = maxNeighbors;
    }

    /**
     * Gets the minimum number of neighbours
     * that can randomly be selected to a node.
     * @return
     * @see GossipNode#addRandomNeighbors()
     */
    public int getMinNeighbors() {
        return minNeighbors;
    }

    /**
     * Sets the minimum number of neighbours
     * that can randomly be selected to a node.
     * @see GossipNode#addRandomNeighbors()
     */
    public void setMinNeighbors(final int minNeighbors) {
        if(minNeighbors < 0)
            throw new IllegalArgumentException("Min number of neighbors must not be negative.");

        this.minNeighbors = minNeighbors;
    }
}
