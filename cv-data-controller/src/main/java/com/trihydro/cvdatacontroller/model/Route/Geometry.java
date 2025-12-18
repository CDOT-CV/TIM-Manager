package com.trihydro.cvdatacontroller.model.Route;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Geometry {
    private List<List<List<Double>>> paths;
}

