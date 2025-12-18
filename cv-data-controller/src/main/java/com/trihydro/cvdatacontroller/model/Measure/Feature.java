package com.trihydro.cvdatacontroller.model.Measure;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Feature {
    private Attributes attributes;
}

