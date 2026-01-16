package com.trihydro.cvdatacontroller.model.gisResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents the spatial geometry of a GIS feature.
 * Used in GISMilepostImpl to extract coordinate paths (longitude/latitude)
 * when calculating the route segment between two milepost measures.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Geometry {
    private List<List<List<Double>>> paths;
}

