package com.trihydro.cvdatacontroller.model.gisResponse;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an individual feature within a GIS response.
 * In GISMilepostImpl, the root JSON structure is a list of features that contain an Attribute and sometimes a Geometry.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feature {
    private Attributes attributes;
    private Geometry geometry;
}

