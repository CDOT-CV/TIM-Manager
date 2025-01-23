package com.trihydro.library.model;

import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;

public class WydotTim {

	@ApiModelProperty(value = "Expected values are I, D, B", required = true)
	private String direction;
	@ApiModelProperty(required = false)
	private Coordinate startPoint;
	@ApiModelProperty(required = false)
	private Coordinate endPoint;
	@ApiModelProperty(value = "The common name for the selected route", required = true)
	private String route;
	@ApiModelProperty(required = true)
	private List<String> itisCodes;
	@ApiModelProperty(required = true)
	private String clientId;
	@ApiModelProperty(required = false)
    protected List<Coordinate> geometry;
	@ApiModelProperty(required = false)
	private Integer bearing;

	public WydotTim() {

	}

	public WydotTim(WydotTim o) {
		this.direction = o.direction;
		if (o.startPoint != null)
			this.startPoint = new Coordinate(o.startPoint.getLatitude(), o.startPoint.getLongitude());
		if (o.endPoint != null)
			this.endPoint = new Coordinate(o.endPoint.getLatitude(), o.endPoint.getLongitude());
		this.route = o.route;
		if (o.itisCodes != null)
			this.itisCodes = new ArrayList<>(o.itisCodes);
		this.clientId = o.clientId;
		if (o.geometry != null) {
			this.geometry = new ArrayList<>(o.geometry);
			// If geometry is present and has more than one point, set start and end points
			if (o.geometry.size() > 1) {
				this.startPoint = new Coordinate(o.geometry.get(0).getLatitude(), o.geometry.get(0).getLongitude());
				this.endPoint = new Coordinate(o.geometry.get(o.geometry.size() - 1).getLatitude(), o.geometry.get(o.geometry.size() - 1).getLongitude());
			}
		}
		if (o.bearing != null)
		    this.bearing = o.bearing;
	}

	public WydotTim copy() {
		return new WydotTim(this);
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public List<String> getItisCodes() {
		return this.itisCodes;
	}

	public void setItisCodes(List<String> itisCodes) {
		this.itisCodes = itisCodes;
	}

	public String getDirection() {
		return this.direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public Coordinate getStartPoint() {
		return this.startPoint;
	}

	public void setStartPoint(Coordinate startPoint) {
		this.startPoint = startPoint;
	}

	public Coordinate getEndPoint() {
		return this.endPoint;
	}

	public void setEndPoint(Coordinate endPoint) {
		this.endPoint = endPoint;
	}

	public String getRoute() {
		return this.route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public List<Coordinate> getGeometry() {
        return this.geometry;
    }

	public void setGeometry(List<Coordinate> geometry) {
        this.geometry = geometry;
    }

	public Integer getBearing() {
		return this.bearing;
	}

	public void setBearing(Integer bearing) {
		this.bearing = bearing;
	}

	public boolean isGeometryValid() {
		if (this.geometry != null && this.geometry.size() > 0) {
			for (Coordinate coord : this.geometry) {
				if (!coord.isValid()) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public String getGeometryString() {
		if (this.geometry!= null && this.geometry.size() > 0) {
            StringBuilder sb = new StringBuilder();
			sb.append("[");
            for (Coordinate coord : this.geometry) {
                sb.append("{\"latitude\": ").append(coord.getLatitude()).append(", ").append("\"longitude\": ").append(coord.getLongitude()).append("}, ");
            }
            return sb.toString().trim();
        } else {
            return "";
        }
	}
}