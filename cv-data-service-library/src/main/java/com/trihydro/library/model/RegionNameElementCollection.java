package com.trihydro.library.model;

public class RegionNameElementCollection {
    public String direction;
    public String route;
    public String rsuOrSat;
    public String timType;
    public String timId;
    public String pk;

    public RegionNameElementCollection() {

    }

    public RegionNameElementCollection(String regionName) {
        String[] splitName = regionName.split("_");
        if (splitName.length == 0) {
            return;
        }
        this.direction = splitName[0];

        if (splitName.length > 1) {
            this.timType = splitName[1];
        }
        else {
            return;
        }

        if (splitName.length > 2) {
            this.timId = splitName[2];
        }
        else {
            return;
        }
    }
    
    public RegionNameElementCollection (String direction, String route, String rsuOrSat, String timType, String timId) {
        this.direction = direction;
        this.route = route;
        this.rsuOrSat = rsuOrSat;
        this.timType = timType;
        this.timId = timId;
    }
}
