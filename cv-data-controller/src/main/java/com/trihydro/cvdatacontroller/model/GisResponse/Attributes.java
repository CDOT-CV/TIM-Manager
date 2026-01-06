package com.trihydro.cvdatacontroller.model.GisResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attributes {
    @JsonProperty("Route")
    private String Route;

    @JsonProperty("Measure")
    private Double Measure;

    @JsonProperty("MMin")
    private Double MMin;

    @JsonProperty("MMax")
    private Double MMax;
}
