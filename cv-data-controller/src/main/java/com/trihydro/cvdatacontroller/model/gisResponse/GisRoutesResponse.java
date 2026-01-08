package com.trihydro.cvdatacontroller.model.gisResponse;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents the specific data structure returned by the GIS service routes endpoint.
 */
@Getter
@Setter
public class GisRoutesResponse {
    private List<Attributes> routes;
}