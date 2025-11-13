package com.trihydro.library.service;

import com.trihydro.library.model.TimeToLive;

import java.math.BigDecimal;

public interface TimGenerationProps {
    public Double getPointIncidentBufferMiles();
    public Double getPathDistanceLimit(); 
    public String[] getRsuRoutes();
	public TimeToLive getSdwTtl();
	public BigDecimal getDefaultLaneWidth();
}
