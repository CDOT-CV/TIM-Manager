package com.trihydro.library.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class ActiveTimHolding {
    private Long activeTimHoldingId;
    private String direction;
    private String clientId;
    private String satRecordId;
    private String rsuTarget;
    private Integer rsuIndex;
    private Coordinate startPoint;
    private Coordinate endPoint;
    private String dateCreated;
    private Integer projectKey;
    private String expirationDateTime;
    private String packetId;
    private String desiredEndDateTime;

    public ActiveTimHolding(WydotTim tim, String rsuTarget, String satRecordId, Coordinate endPt, String desiredEndDateTime) {
        this.clientId = tim.getClientId();
        this.direction = tim.getDirection();
        this.rsuTarget = rsuTarget;
        this.satRecordId = satRecordId;
        this.startPoint = tim.getStartPoint();
        this.endPoint = endPt;
        this.dateCreated = Instant.now().toString();
        this.desiredEndDateTime = desiredEndDateTime;
    }
}