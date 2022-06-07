package com.boeing.cas.supa.ground.pojos;

import java.util.List;

import org.springframework.http.HttpStatus;

public class AirlineStatusChecklistItem {

    private String item;
    private List<Object> content;
    private HttpStatus status;
    
    public AirlineStatusChecklistItem(String item, List<Object> content, HttpStatus status) {
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

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
    
}
