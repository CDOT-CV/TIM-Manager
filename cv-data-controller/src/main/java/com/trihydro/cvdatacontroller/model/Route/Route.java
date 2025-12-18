package com.trihydro.cvdatacontroller.model.Route;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trihydro.cvdatacontroller.model.Route.Feature;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Route {
    private List<Feature> features;

    public boolean hasRoutes() {
        return features != null && !features.isEmpty();
    }
}
