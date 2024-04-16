package com.cfde.playbook_ctd.model;

import org.json.simple.JSONObject;

public class ResponseFormat {
    private String[] highlyConnectedGenes;
    private String[] guiltyByAssociationGenes;
    private JSONObject jsonGraph;
    private Report report;

    public ResponseFormat(Report report){
        this.report = report;
    }

    public ResponseFormat(){}

    public String[] getHighlyConnectedGenes() {
        return highlyConnectedGenes;
    }

    public void setHighlyConnectedGenes(String[] highlyConnectedGenes) {
        this.highlyConnectedGenes = highlyConnectedGenes;
    }

    public String[] getGuiltyByAssociationGenes() {
        return guiltyByAssociationGenes;
    }

    public void setGuiltyByAssociationGenes(String[] guiltyByAssociationGenes) {
        this.guiltyByAssociationGenes = guiltyByAssociationGenes;
    }

    public JSONObject getJsonGraph() {
        return jsonGraph;
    }

    public void setJsonGraph(JSONObject jsonGraph) {
        this.jsonGraph = jsonGraph;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

}
