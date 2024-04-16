package com.cfde.playbook_ctd.model;

import com.cfde.playbook_ctd.utils.constants.Constants;

public class Report {
    private String type = Constants.REPORT_TYPE_OK;
    private String message;

    public Report(String type, String message){
        this.type=type;
        this.message=message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public void appendToMessage(String newContent) {
        if(this.message == null){
            this.message = "";
        }
        this.message += newContent;
    }
}
