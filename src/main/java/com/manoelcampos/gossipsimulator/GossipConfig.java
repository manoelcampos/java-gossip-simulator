package com.manoelcampos.gossipsimulator;

public class GossipConfig {
    private final int fanout;
    private final int maxNeighbours;

    /**
     * Instantiates a gossip config.
     * @param fanout the number of nodes to randomly spread information to
     * @param maxNeighbours the maximum number of neighbours a node will be linked to.
     */
    public GossipConfig(final int fanout, final int maxNeighbours) {
        if(fanout <= 0){
            throw new IllegalArgumentException("Fanout must be greater than 0.");
        }

        if(maxNeighbours <= 0){
            throw new IllegalArgumentException("Max number of neighbours must be greater than 0.");
        }

        if(maxNeighbours <= fanout){
            throw new IllegalArgumentException("Max number of neighbours must be greater than the fanout.");
        }

        this.fanout = fanout;
        this.maxNeighbours = maxNeighbours;
    }

    /**
     * Gets the number of nodes to randomly spread information to
     * @return
     */
    public int getFanout() {
        return fanout;
    }

    /**
     * Gets the max number of neighbours a node will be linked to.
     * @return
     */
    public int getMaxNeighbours() {
        return maxNeighbours;
    }

}
