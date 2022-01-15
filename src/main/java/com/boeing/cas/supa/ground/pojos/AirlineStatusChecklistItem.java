package com.boeing.cas.supa.ground.pojos;

import java.util.List;

public class AirlineStatusChecklistItem {

    private String item;
    private List<Object> content;
    private String status;
    
    public AirlineStatusChecklistItem(String item, List<Object> content, String status) {
        this.item = item;
        this.content = content;
        this.status = status;
    }

    public AirlineStatusChecklistItem(){}

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public List<Object> getContent() {
        return content;
    }

    public void setContent(List<Object> content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
}
