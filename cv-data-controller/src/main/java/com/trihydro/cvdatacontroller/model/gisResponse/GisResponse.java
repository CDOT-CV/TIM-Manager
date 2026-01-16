package com.trihydro.cvdatacontroller.model.gisResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents the top-level response from the GIS service.
 * Used in GISMilepostImpl to unmarshal the root JSON structure containing a list of features.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GisResponse {
    private List<Feature> features;
}