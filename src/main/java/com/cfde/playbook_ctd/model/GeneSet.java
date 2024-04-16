package com.cfde.playbook_ctd.model;

import java.util.ArrayList;

public class GeneSet {
    private String graphType;
    private ArrayList<String> geneList;

    public String getGraphType() {
        return graphType;
    }

    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }

    public ArrayList<String> getGeneList() {
        return geneList;
    }

    public void setGeneList(ArrayList<String> geneList) {
        this.geneList = geneList;
    }
}
