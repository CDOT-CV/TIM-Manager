package com.trihydro.library.model;


import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;

public class WydotOdeTravelerInformationMessage extends TravelerInformation {

    private static final long serialVersionUID = 1L;
    private Integer rsuIndex;

    public Integer getRsuIndex() {
        return rsuIndex;
    }

    public void setRsuIndex(Integer rsuIndex) {
        this.rsuIndex = rsuIndex;
    }
}
