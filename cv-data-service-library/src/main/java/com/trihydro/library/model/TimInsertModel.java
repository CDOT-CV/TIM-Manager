package com.trihydro.library.model;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;

public class TimInsertModel {
    private OdeMsgMetadata odeTimMetadata;
    private ReceivedMessageDetails receivedMessageDetails;
    private TravelerInformation j2735TravelerInformationMessage;
    private RecordType recordType;
    private String logFileName;
    private SecurityResultCode securityResultCode;
    private String satRecordId;
    private String regionName;

    public OdeMsgMetadata getOdeTimMetadata() {
        return odeTimMetadata;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getSatRecordId() {
        return satRecordId;
    }

    public void setSatRecordId(String satRecordId) {
        this.satRecordId = satRecordId;
    }

    public SecurityResultCode getSecurityResultCode() {
        return securityResultCode;
    }

    public void setSecurityResultCode(SecurityResultCode securityResultCode) {
        this.securityResultCode = securityResultCode;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String logFileName) {
        this.logFileName = logFileName;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public TravelerInformation getJ2735TravelerInformationMessage() {
        return j2735TravelerInformationMessage;
    }

    public void setJ2735TravelerInformationMessage(TravelerInformation j2735TravelerInformationMessage) {
        this.j2735TravelerInformationMessage = j2735TravelerInformationMessage;
    }

    public ReceivedMessageDetails getReceivedMessageDetails() {
        return receivedMessageDetails;
    }

    public void setReceivedMessageDetails(ReceivedMessageDetails receivedMessageDetails) {
        this.receivedMessageDetails = receivedMessageDetails;
    }

    public void setOdeTimMetadata(OdeMsgMetadata odeTimMetadata) {
        this.odeTimMetadata = odeTimMetadata;
    }
}