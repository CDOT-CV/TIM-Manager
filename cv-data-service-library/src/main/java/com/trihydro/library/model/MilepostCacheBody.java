package com.trihydro.library.model;

import java.util.List;

public class MilepostCacheBody {
    private List<Milepost> mileposts;
    private String timID;
    
    public MilepostCacheBody(List<Milepost> mileposts, String timID) {
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