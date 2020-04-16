package com.trihydro.library.model.tmdd;

public class EventType {
    private String name;
    private String type;

    public EventType() {

    }

    public EventType(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
