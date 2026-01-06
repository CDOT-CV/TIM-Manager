package com.trihydro.cvdatacontroller.model.GisResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GisResponse {
    private List<Feature> features;
}