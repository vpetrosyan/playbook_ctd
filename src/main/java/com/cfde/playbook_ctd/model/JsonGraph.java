package com.cfde.playbook_ctd.model;

import java.util.ArrayList;

public class JsonGraph {
    private ArrayList<GraphNode> nodes;
    private ArrayList<Graphinteraction> interactions;

    public ArrayList<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public ArrayList<Graphinteraction> getInteractions() {
        return interactions;
    }

    public void setInteractions(ArrayList<Graphinteraction> interactions) {
        this.interactions = interactions;
    }
}
