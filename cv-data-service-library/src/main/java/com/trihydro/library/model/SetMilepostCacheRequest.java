package com.trihydro.library.model;

import java.util.List;

public class SetMilepostCacheRequest {
    private final List<Milepost> mileposts;
    private final String timID;
    
    public SetMilepostCacheRequest(List<Milepost> mileposts, String timID) {
        this.mileposts = mileposts;
        this.timID = timID;
    }
    public List<Milepost> getMileposts() {
        return mileposts;
    }
    public String getTimID() {
        return timID;
    }
}