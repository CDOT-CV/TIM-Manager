package com.trihydro.loggerkafkaconsumer.app.services;

import com.trihydro.library.helpers.DateTimeHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.trihydro.library.helpers.SQLNullHandler;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.ActiveTimHolding;
import com.trihydro.library.model.CertExpirationModel;
import com.trihydro.library.model.ItisCode;
import com.trihydro.library.model.RegionNameElementCollection;
import com.trihydro.library.model.SecurityResultCodeType;
import com.trihydro.library.model.TimType;
import com.trihydro.library.model.WydotRsu;
import com.trihydro.library.tables.TimDbTables;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.ode.model.OdeData;
import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeLogMetadata.SecurityResultCode;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.OdeRequestMsgMetadata;
import us.dot.its.jpo.ode.model.OdeTimPayload;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.Geometry;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame.Region.Path;

@Component
@Slf4j
public class TimService extends BaseService {

    public Gson gson = new Gson();
    private ActiveTimService activeTimService;
    private TimDbTables timDbTables;
    private SQLNullHandler sqlNullHandler;
    private PathService pathService;
    private RegionService regionService;
    private DataFrameService dataFrameService;
    private RsuService rsuService;
    private TimTypeService timTypeService;
    private ItisCodeService itisCodeService;
    private TimRsuService timRsuService;
    private DataFrameItisCodeService dataFrameItisCodeService;
    private PathNodeXYService pathNodeXYService;
    private NodeXYService nodeXYService;
    private Utility utility;
    private ActiveTimHoldingService activeTimHoldingService;
    private PathNodeLLService pathNodeLLService;
    private NodeLLService nodeLLService;
    private DateTimeHelper dateTimeHelper;

    @Autowired
    public void InjectDependencies(ActiveTimService _ats, TimDbTables _timDbTables,
                                   SQLNullHandler _sqlNullHandler, PathService _pathService, RegionService _regionService,
                                   DataFrameService _dataFrameService, RsuService _rsuService, TimTypeService _tts,
                                   ItisCodeService _itisCodesService, TimRsuService _timRsuService,
                                   DataFrameItisCodeService _dataFrameItisCodeService, PathNodeXYService _pathNodeXYService,
                                   NodeXYService _nodeXYService, Utility _utility, ActiveTimHoldingService _athService,
                                   PathNodeLLService _pathNodeLLService,
                                   NodeLLService _nodeLLService, DateTimeHelper dateTimeHelper) { // TODO: use constructor instead of InjectDependencies
        activeTimService = _ats;
        timDbTables = _timDbTables;
        sqlNullHandler = _sqlNullHandler;
        pathService = _pathService;
        regionService = _regionService;
        dataFrameService = _dataFrameService;
        rsuService = _rsuService;
        timTypeService = _tts;
        itisCodeService = _itisCodesService;
        timRsuService = _timRsuService;
        dataFrameItisCodeService = _dataFrameItisCodeService;
        pathNodeXYService = _pathNodeXYService;
        nodeXYService = _nodeXYService;
        utility = _utility;
        activeTimHoldingService = _athService;
        pathNodeLLService = _pathNodeLLService;
        nodeLLService = _nodeLLService;
        this.dateTimeHelper = dateTimeHelper;
    }

    public void addTimToDatabase(OdeData<?, OdeTimPayload> odeData) { // TODO: identify if this method can be removed

        try {
            log.info("Called addTimToDatabase");

            ReceivedMessageDetails rxMsgDet = null;
            RecordType recType = null;
            String logFileName = null;
            SecurityResultCode secResCode = null;
            if (odeData.getMetadata() instanceof OdeLogMetadata) {
                var odeLogMetadata = (OdeLogMetadata) odeData.getMetadata();
                rxMsgDet = odeLogMetadata.getReceivedMessageDetails();
                recType = odeLogMetadata.getRecordType();
                logFileName = odeLogMetadata.getLogFileName();
                secResCode = odeLogMetadata.getSecurityResultCode();
            }

            Long timId = AddTim(odeData.getMetadata(), rxMsgDet, getTim((OdeTimPayload) odeData.getPayload()),
                recType, logFileName, secResCode, null, null);

            // return if TIM is not inserted
            if (timId == null) {
                return;
            }

            DataFrame[] dFrames = getTim((OdeTimPayload) odeData.getPayload()).getDataframes();
            if (dFrames.length == 0) {
                log.info("addTimToDatabase - No dataframes found in TIM (tim_id: {})", timId);
                return;
            }
            DataFrame firstDataFrame = dFrames[0];
            Long dataFrameId = dataFrameService.AddDataFrame(firstDataFrame, timId);

            Region[] regions = firstDataFrame.getRegions();
            addRegions(firstDataFrame, dataFrameId);

            String firstRegionName = regions[0].getName(); // all regions have the same name
            ActiveTim activeTim = setActiveTimByRegionName(firstRegionName);

            // if this is an RSU TIM
            if (activeTim != null && activeTim.getRsuTarget() != null) {
                // save TIM RSU in DB
                rsuService.getRsus().stream()
                    .filter(x -> x.getRsuTarget().equals(activeTim.getRsuTarget())).findFirst()
                    .ifPresent(rsu -> timRsuService.AddTimRsu(timId, rsu.getRsuId(), rsu.getRsuIndex()));
            }

            addDataFrameItis(firstDataFrame, dataFrameId);

        } catch (NullPointerException e) {
            log.info("Null pointer exception encountered in TimService.addTimToDatabase() method: {}", e.getMessage());
        }
    }

    /**
     * Adds an active TIM to the database. This only handles a single TIM at a time.
     */
    public void addActiveTimToDatabase(OdeData<?, OdeTimPayload> odeData) {
        log.info("Processing active TIM to add to database");

        // Initial validation
        OdeTimPayload payload = odeData.getPayload();
        if (payload == null) {
            log.warn("Cannot process active TIM: payload is null");
            return;
        }

        OdeTravelerInformationMessage tim = getTim(payload);
        if (tim == null) {
            log.warn("Cannot process active TIM: TravelerInformationMessage is null");
            return;
        }

        log.trace("Processing TIM with packet ID: {}", tim.getPacketID());

        DataFrame[] dframes = tim.getDataframes();
        if (dframes == null || dframes.length == 0) {
            log.warn("Cannot process active TIM: no dataframes found in TIM with packet ID: {}", tim.getPacketID());
            return;
        }

        Region[] regions = dframes[0].getRegions();
        if (regions == null || regions.length == 0) {
            log.warn("Cannot process active TIM: no regions found in first dataframe of TIM with packet ID: {}", tim.getPacketID());
            return;
        }

        String firstRegionName = regions[0].getName();
        if (StringUtils.isEmpty(firstRegionName) || StringUtils.isBlank(firstRegionName)) {
            log.warn("Cannot process active TIM: empty region name in TIM with packet ID: {}", tim.getPacketID());
            return;
        }

        OdeRequestMsgMetadata metaData = (OdeRequestMsgMetadata) odeData.getMetadata();
        if (metaData == null) {
            log.warn("Cannot process active TIM: metadata is null for TIM with packet ID: {}", tim.getPacketID());
            return;
        }

        // Extract information from region name
        log.debug("Extracting TIM information from region name: '{}'", firstRegionName);
        ActiveTim activeTim = setActiveTimByRegionName(firstRegionName);
        if (activeTim == null) {
            log.warn("Cannot process active TIM: failed to extract information from region name: '{}' for TIM with packet ID: {}", firstRegionName,
                tim.getPacketID());
            return;
        }

        log.debug("Extracted information - Route: {}, Direction: {}, ClientId: {}, TimType: {}", activeTim.getRoute(), activeTim.getDirection(),
            activeTim.getClientId(), activeTim.getTimType());

        String satRecordId = activeTim.getSatRecordId();
        if (satRecordId != null) {
            log.debug("TIM has satellite record ID: {}", satRecordId);
        }

        // Check if TIM already exists in database
        Timestamp ts = null;
        if (StringUtils.isNotEmpty(tim.getTimeStamp()) && StringUtils.isNotBlank(tim.getTimeStamp())) {
            try {
                ts = Timestamp.valueOf(LocalDateTime.parse(tim.getTimeStamp(), DateTimeFormatter.ISO_DATE_TIME));
                log.trace("Parsed timestamp: {} from TIM timestamp: {}", ts, tim.getTimeStamp());
            } catch (Exception e) {
                log.warn("Failed to parse timestamp from TIM: {}", e.getMessage());
            }
        }

        log.debug("Checking if TIM already exists with packet ID: {} and timestamp: {}", tim.getPacketID(), ts);
        Long timId = getTimId(tim.getPacketID(), ts);

        if (timId == null) {
            // TIM doesn't currently exist. Add it.
            log.info("TIM not found in database, adding new TIM with packet ID: {}", tim.getPacketID());
            timId = AddTim(metaData, null, tim, null, null, null, satRecordId, firstRegionName);

            if (timId != null) {
                log.debug("Successfully added TIM with ID: {}", timId);
                // Add additional data
                log.trace("Adding dataframe for TIM ID: {}", timId);
                Long dataFrameId = dataFrameService.AddDataFrame(dframes[0], timId);

                log.trace("Adding regions for dataframe ID: {}", dataFrameId);
                addRegions(dframes[0], dataFrameId);

                log.trace("Adding ITIS codes for dataframe ID: {}", dataFrameId);
                addDataFrameItis(dframes[0], dataFrameId);
            } else {
                log.error("Failed to insert TIM and failed to fetch existing TIM. No data inserted for TIM with packet ID: {}", tim.getPacketID());
                log.debug("OdeData for TIM with packet ID: {}: {}", tim.getPacketID(), gson.toJson(odeData));
                return;
            }
        } else {
            log.info("TIM already exists in database with ID: {} and packet ID: {}", timId, tim.getPacketID());
        }

        // Update satellite record ID if available
        if (satRecordId != null && !satRecordId.isEmpty()) {
            log.debug("Updating TIM ID: {} with satellite record ID: {}", timId, satRecordId);
            boolean updated = updateTimSatRecordId(timId, satRecordId);
            if (updated) {
                log.debug("Successfully updated satellite record ID for TIM ID: {}", timId);
            } else {
                log.warn("Failed to update satellite record ID for TIM ID: {}", timId);
            }
        }

        // Handle RSU information
        RSU firstRsu = null; // TODO: update to handle multiple RSUs if needed
        if (metaData.getRequest() != null && metaData.getRequest().getRsus() != null && metaData.getRequest().getRsus().length > 0) {
            firstRsu = metaData.getRequest().getRsus()[0];
            activeTim.setRsuTarget(firstRsu.getRsuTarget());
            log.debug("Set RSU target: {} for TIM ID: {}", firstRsu.getRsuTarget(), timId);
        }

        // Set satellite record ID from metadata if available
        if (metaData.getRequest() != null && metaData.getRequest().getSdw() != null) {
            String metadataSatRecordId = metaData.getRequest().getSdw().getRecordId();
            activeTim.setSatRecordId(metadataSatRecordId);
            log.debug("Set satellite record ID from metadata: {} for TIM ID: {}", metadataSatRecordId, timId);
        }

        // Set start date and TIM ID
        var stDate = metaData.getOdeTimStartDateTime();
        if (StringUtils.isEmpty(stDate)) {
            stDate = dframes[0].getStartDateTime();
            log.debug("Using dataframe start time: {} (metadata start time was empty) for TIM ID: {}", stDate, timId);
        } else {
            log.debug("Using metadata start time: {} for TIM ID: {}", stDate, timId);
        }
        activeTim.setStartDateTime(stDate);
        activeTim.setTimId(timId);

        // Get active TIM holding record
        ActiveTimHolding ath;
        if (activeTim.getRsuTarget() != null && firstRsu != null) {
            // RSU TIM handling
            log.debug("Processing RSU TIM with target: {} for TIM ID: {}", activeTim.getRsuTarget(), timId);

            // Save TIM-RSU association
            WydotRsu rsu = rsuService.getRsus().stream().filter(x -> x.getRsuTarget().equals(activeTim.getRsuTarget())).findFirst().orElse(null);
            if (rsu != null) {
                log.trace("Associating TIM ID: {} with RSU ID: {} (index: {})", timId, rsu.getRsuId(), rsu.getRsuIndex());
                timRsuService.AddTimRsu(timId, rsu.getRsuId(), rsu.getRsuIndex());
            } else {
                log.warn("RSU with target: {} not found in database for TIM ID: {}", activeTim.getRsuTarget(), timId);
            }

            // Get active TIM holding for RSU
            log.trace("Retrieving active TIM holding for client ID: {}, direction: {}, RSU target: {}", activeTim.getClientId(),
                activeTim.getDirection(), activeTim.getRsuTarget());
            ath = activeTimHoldingService.getRsuActiveTimHolding(activeTim.getClientId(), activeTim.getDirection(), activeTim.getRsuTarget());
        } else {
            // SDX TIM handling
            log.debug("Processing SDX TIM with satellite record ID: {} for TIM ID: {}", activeTim.getSatRecordId(), timId);

            // Get active TIM holding for SDX
            log.trace("Retrieving active TIM holding for client ID: {}, direction: {}, satellite record ID: {}", activeTim.getClientId(),
                activeTim.getDirection(), activeTim.getSatRecordId());
            ath = activeTimHoldingService.getSdxActiveTimHolding(activeTim.getClientId(), activeTim.getDirection(), activeTim.getSatRecordId());
        }

        if (ath == null) {
            if (activeTim.getRsuTarget() != null) {
                log.warn("No active TIM holding found for RSU TIM with client ID: '{}', direction: '{}', RSU target: '{}'", activeTim.getClientId(),
                    activeTim.getDirection(), activeTim.getRsuTarget());
            } else {
                log.warn("No active TIM holding found for SAT TIM with client ID: '{}', direction: '{}', satellite record ID: '{}'", activeTim.getClientId(),
                    activeTim.getDirection(), activeTim.getSatRecordId());
            }
        } else {
            log.debug("Found active TIM holding with ID: {}", ath.getActiveTimHoldingId());
        }

        // Handle duration and end time
        int durationTime = dframes[0].getDurationTime();
        final int INDEFINITE_DURATION = 32000;
        final int SHORT_DURATION_MINUTES = 5;

        if (durationTime != INDEFINITE_DURATION) {
            // Finite duration handling
            log.debug("TIM ID: {} has finite duration: {} minutes", timId, durationTime);

            if (durationTime == SHORT_DURATION_MINUTES) {
                log.info("Short duration TIM detected (5 minutes) for TIM ID: {} - likely an expiry TIM", timId);
            }

            try {
                ZonedDateTime zdt = ZonedDateTime.parse(dframes[0].getStartDateTime()).plusMinutes(durationTime);
                String endDateTime = dateTimeHelper.convertZonedDateTimeToISO8601Format(zdt);
                log.debug("Calculated end time: {} for TIM ID: {}", endDateTime, timId);
                activeTim.setEndDateTime(endDateTime);
            } catch (Exception e) {
                log.error("Failed to calculate end time for TIM ID: {} - {}", timId, e.getMessage());
            }
        } else {
            // Indefinite duration handling
            log.debug("TIM ID: {} has indefinite duration (32000 minutes)", timId);

            if (ath != null && ath.getDesiredEndDateTime() != null) {
                log.debug("Using end time from active TIM holding: {} for TIM ID: {}", ath.getDesiredEndDateTime(), timId);
                activeTim.setEndDateTime(ath.getDesiredEndDateTime());
            } else {
                log.debug("No desired end time found in active TIM holding for TIM ID: {}, setting to null", timId);
                activeTim.setEndDateTime(null);
            }
        }

        // Set additional fields from active TIM holding
        if (ath != null) {
            log.trace("Copying data from active TIM holding to active TIM for TIM ID: {}", timId);

            activeTim.setStartPoint(ath.getStartPoint());
            activeTim.setEndPoint(ath.getEndPoint());
            activeTim.setProjectKey(ath.getProjectKey());

            if (StringUtils.isNotBlank(ath.getExpirationDateTime())) {
                log.debug("Setting expiration time: {} for TIM ID: {}", ath.getExpirationDateTime(), timId);
                activeTim.setExpirationDateTime(ath.getExpirationDateTime());
            }
        }

        // Insert or update active TIM record
        if (activeTim.getTimType() != null) {
            // TIM came from WYDOT
            log.debug("Processing WYDOT TIM ID: {} of type: {}", timId, activeTim.getTimType());

            ActiveTim activeTimDb;
            if (activeTim.getRsuTarget() != null) {
                // Look for active RSU TIM
                log.trace("Looking for existing active RSU TIM with client ID: {}, direction: {}, RSU target: {}", activeTim.getClientId(),
                    activeTim.getDirection(), activeTim.getRsuTarget());
                activeTimDb = activeTimService.getActiveRsuTim(activeTim.getClientId(), activeTim.getDirection(), activeTim.getRsuTarget());
            } else {
                // Look for active satellite TIM
                log.trace("Looking for existing active satellite TIM with satellite record ID: {}, direction: {}", activeTim.getSatRecordId(),
                    activeTim.getDirection());
                activeTimDb = activeTimService.getActiveSatTim(activeTim.getSatRecordId(), activeTim.getDirection());
            }

            if (activeTimDb == null) {
                // Insert new active TIM
                log.info("Inserting new active TIM for TIM ID: {}", timId);
                activeTimService.insertActiveTim(activeTim);
            } else {
                // Update existing active TIM
                log.info("Updating existing active TIM with ID: {} for TIM ID: {}", activeTimDb.getActiveTimId(), timId);

                // Preserve existing values if no active TIM holding
                if (ath == null) {
                    log.debug("Preserving existing start/end points and project key from active TIM ID: {}", activeTimDb.getActiveTimId());
                    activeTim.setStartPoint(activeTimDb.getStartPoint());
                    activeTim.setEndPoint(activeTimDb.getEndPoint());
                    activeTim.setProjectKey(activeTimDb.getProjectKey());
                }

                activeTim.setActiveTimId(activeTimDb.getActiveTimId());
                activeTimService.updateActiveTim(activeTim);
            }
        } else {
            // Not from WYDOT application
            log.info("Inserting new active TIM for TIM ID: {} (not from WYDOT application - no TimType found)", timId);
            activeTimService.insertActiveTim(activeTim);
        }

        // Clean up active TIM holding
        if (ath != null) {
            log.debug("Deleting active TIM holding with ID: {} after processing", ath.getActiveTimHoldingId());
            activeTimHoldingService.deleteActiveTimHolding(ath.getActiveTimHoldingId());
        }

        log.info("Successfully processed active TIM with ID: {} (packet ID: {})", timId, tim.getPacketID());
    }

    public Long getTimId(String packetId, Timestamp timeStamp) {
        Long id = null;

        try (
            Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement preparedStatement = connection
                .prepareStatement("select tim_id from tim where packet_id = ? and time_stamp = ?");
        ) {
            preparedStatement.setString(1, packetId);
            preparedStatement.setTimestamp(2, timeStamp);

            try (
                ResultSet rs = preparedStatement.executeQuery();
            ) {
                if (rs.next()) {
                    id = rs.getLong("tim_id");
                }
            }

        } catch (Exception e) {
            log.error("Failed to get tim_id from database", e);
        }
        return id;
    }

    public Long AddTim(OdeMsgMetadata odeTimMetadata, ReceivedMessageDetails receivedMessageDetails,
                       OdeTravelerInformationMessage j2735TravelerInformationMessage, RecordType recordType, String logFileName,
                       SecurityResultCode securityResultCode, String satRecordId, String regionName) {
        String insertQueryStatement = timDbTables.buildInsertQueryStatement("tim",
            timDbTables.getTimTable());

        try (
            Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement preparedStatement = connection.prepareStatement(insertQueryStatement, new String[] {"tim_id"});
        ) {
            int fieldNum = 1;

            for (String col : timDbTables.getTimTable()) {
                // default to null
                preparedStatement.setString(fieldNum, null);
                if (j2735TravelerInformationMessage != null) {
                    if (col.equals("MSG_CNT")) {
                        sqlNullHandler.setIntegerOrNull(preparedStatement, fieldNum,
                            j2735TravelerInformationMessage.getMsgCnt());
                    } else if (col.equals("PACKET_ID")) {
                        sqlNullHandler.setStringOrNull(preparedStatement, fieldNum,
                            j2735TravelerInformationMessage.getPacketID());
                    } else if (col.equals("URL_B")) {
                        sqlNullHandler.setStringOrNull(preparedStatement, fieldNum,
                            j2735TravelerInformationMessage.getUrlB());
                    } else if (col.equals("TIME_STAMP")) {
                        String timeStamp = j2735TravelerInformationMessage.getTimeStamp();
                        Timestamp ts = null;
                        if (StringUtils.isNotEmpty(timeStamp) && StringUtils.isNotBlank(timeStamp)) {
                            ts = Timestamp
                                .valueOf(LocalDateTime.parse(timeStamp, DateTimeFormatter.ISO_DATE_TIME));
                        }
                        sqlNullHandler.setTimestampOrNull(preparedStatement, fieldNum, ts);
                    }
                }
                if (odeTimMetadata != null) {
                    if (col.equals("RECORD_GENERATED_BY")) {
                        if (odeTimMetadata.getRecordGeneratedBy() != null) {
                            sqlNullHandler.setStringOrNull(preparedStatement, fieldNum,
                                odeTimMetadata.getRecordGeneratedBy().toString());
                        } else {
                            preparedStatement.setString(fieldNum, null);
                        }
                    } else if (col.equals("RECORD_GENERATED_AT")) {
                        if (odeTimMetadata.getRecordGeneratedAt() != null) {
                            Date recordGeneratedAtDate = dateTimeHelper.convertDate(odeTimMetadata.getRecordGeneratedAt());
                            Timestamp ts = new Timestamp(recordGeneratedAtDate.getTime());
                            sqlNullHandler.setTimestampOrNull(preparedStatement, fieldNum, ts);
                        } else {
                            preparedStatement.setString(fieldNum, null);
                        }
                    } else if (col.equals("SCHEMA_VERSION")) {
                        sqlNullHandler.setIntegerOrNull(preparedStatement, fieldNum, odeTimMetadata.getSchemaVersion());
                    } else if (col.equals("SANITIZED")) {
                        if (odeTimMetadata.isSanitized()) {
                            preparedStatement.setInt(fieldNum, 1);
                        } else {
                            preparedStatement.setInt(fieldNum, 0);
                        }
                    } else if (col.equals("PAYLOAD_TYPE")) {
                        sqlNullHandler.setStringOrNull(preparedStatement, fieldNum, odeTimMetadata.getPayloadType());
                    } else if (col.equals("ODE_RECEIVED_AT")) {
                        if (odeTimMetadata.getOdeReceivedAt() != null) {
                            Date receivedAtDate = dateTimeHelper.convertDate(odeTimMetadata.getOdeReceivedAt());
                            Timestamp ts = new Timestamp(receivedAtDate.getTime());
                            sqlNullHandler.setTimestampOrNull(preparedStatement, fieldNum, ts);
                        } else {
                            preparedStatement.setTimestamp(fieldNum, null);
                        }
                    }

                    if (odeTimMetadata.getSerialId() != null) {
                        if (col.equals("SERIAL_ID_STREAM_ID")) {
                            sqlNullHandler.setStringOrNull(preparedStatement, fieldNum,
                                odeTimMetadata.getSerialId().getStreamId());
                        } else if (col.equals("SERIAL_ID_BUNDLE_SIZE")) {
                            sqlNullHandler.setIntegerOrNull(preparedStatement, fieldNum,
                                odeTimMetadata.getSerialId().getBundleSize());
                        } else if (col.equals("SERIAL_ID_BUNDLE_ID")) {
                            sqlNullHandler.setLongOrNull(preparedStatement, fieldNum,
                                odeTimMetadata.getSerialId().getBundleId());
                        } else if (col.equals("SERIAL_ID_RECORD_ID")) {
                            sqlNullHandler.setIntegerOrNull(preparedStatement, fieldNum,
                                odeTimMetadata.getSerialId().getRecordId());
                        } else if (col.equals("SERIAL_ID_SERIAL_NUMBER")) {
                            sqlNullHandler.setLongOrNull(preparedStatement, fieldNum,
                                odeTimMetadata.getSerialId().getSerialNumber());
                        }
                    }
                }
                if (receivedMessageDetails != null) {
                    if (receivedMessageDetails.getLocationData() != null) {
                        if (col.equals("RMD_LD_ELEVATION")) {
                            sqlNullHandler.setDoubleOrNull(preparedStatement, fieldNum,
                                Double.parseDouble(receivedMessageDetails.getLocationData().getElevation()));
                        } else if (col.equals("RMD_LD_HEADING")) {
                            sqlNullHandler.setDoubleOrNull(preparedStatement, fieldNum,
                                Double.parseDouble(receivedMessageDetails.getLocationData().getHeading()));
                        } else if (col.equals("RMD_LD_LATITUDE")) {
                            sqlNullHandler.setDoubleOrNull(preparedStatement, fieldNum,
                                Double.parseDouble(receivedMessageDetails.getLocationData().getLatitude()));
                        } else if (col.equals("RMD_LD_LONGITUDE")) {
                            sqlNullHandler.setDoubleOrNull(preparedStatement, fieldNum,
                                Double.parseDouble(receivedMessageDetails.getLocationData().getLongitude()));
                        } else if (col.equals("RMD_LD_SPEED")) {
                            sqlNullHandler.setDoubleOrNull(preparedStatement, fieldNum,
                                Double.parseDouble(receivedMessageDetails.getLocationData().getSpeed()));
                        }
                    } else {
                        // location data is null, set all to null (with correct type)
                        if (col.equals("RMD_LD_ELEVATION") || col.equals("RMD_LD_HEADING") || col.equals("RMD_LD_LATITUDE")
                            || col.equals("RMD_LD_LONGITUDE") || col.equals("RMD_LD_SPEED")) {
                            preparedStatement.setNull(fieldNum, Types.NUMERIC);
                        }
                    }
                    if (col.equals("RMD_RX_SOURCE") && receivedMessageDetails.getRxSource() != null) {
                        sqlNullHandler.setStringOrNull(preparedStatement, fieldNum,
                            receivedMessageDetails.getRxSource().toString());
                    } else if (col.equals("SECURITY_RESULT_CODE")) {
                        SecurityResultCodeType securityResultCodeType = GetSecurityResultCodeTypes().stream()
                            .filter(x -> x.getSecurityResultCodeType().equals(securityResultCode.toString()))
                            .findFirst().orElse(null);
                        if (securityResultCodeType != null) {
                            preparedStatement.setInt(fieldNum, securityResultCodeType.getSecurityResultCodeTypeId());
                        } else {
                            preparedStatement.setNull(fieldNum, Types.INTEGER);
                        }
                    }
                } else {
                    // message details are null, set all to null (with correct type)
                    if (col.equals("RMD_LD_ELEVATION") || col.equals("RMD_LD_HEADING") || col.equals("RMD_LD_LATITUDE")
                        || col.equals("RMD_LD_LONGITUDE") || col.equals("RMD_LD_SPEED")) {
                        preparedStatement.setNull(fieldNum, Types.NUMERIC);
                    } else if (col.equals("RMD_RX_SOURCE")) {
                        preparedStatement.setString(fieldNum, null);
                    } else if (col.equals("SECURITY_RESULT_CODE")) {
                        preparedStatement.setNull(fieldNum, Types.INTEGER);
                    }
                }

                if (col.equals("SAT_RECORD_ID")) {
                    sqlNullHandler.setStringOrNull(preparedStatement, fieldNum, satRecordId);
                } else if (col.equals("TIM_NAME")) {
                    sqlNullHandler.setStringOrNull(preparedStatement, fieldNum, regionName);
                } else if (col.equals("LOG_FILE_NAME")) {
                    sqlNullHandler.setStringOrNull(preparedStatement, fieldNum, logFileName);
                } else if (col.equals("RECORD_TYPE") && recordType != null) {
                    sqlNullHandler.setStringOrNull(preparedStatement, fieldNum, recordType.toString());
                }
                fieldNum++;
            }
            // execute insert statement
            return dbInteractions.executeAndLog(preparedStatement, "timID");
        } catch (SQLException e) {
            log.error("Failed to insert tim into database", e);
        }
        return 0L;
    }

    /**
     * Adds regions to the database for a given DataFrame.
     *
     * @param dataFrame   The DataFrame containing the regions to be added.
     * @param dataFrameId The ID of the DataFrame.
     */
    public void addRegions(DataFrame dataFrame, Long dataFrameId) {
        for (Region region : dataFrame.getRegions()) {
            Path path = region.getPath();
            Geometry geometry = region.getGeometry();

            if (path != null) {
                Long pathId = pathService.InsertPath();
                regionService.AddRegion(dataFrameId, pathId, region);

                Long nodeXYId;
                Long nodeLLId;
                for (OdeTravelerInformationMessage.NodeXY nodeXY : path.getNodes()) {
                    if (nodeXY.getDelta().toLowerCase().contains("xy")) {
                        nodeXYId = nodeXYService.AddNodeXY(nodeXY);
                        pathNodeXYService.insertPathNodeXY(nodeXYId, pathId);
                    } else {
                        // node-LL
                        nodeLLId = nodeLLService.AddNodeLL(nodeXY);
                        pathNodeLLService.insertPathNodeLL(nodeLLId, pathId);
                    }
                }
            } else if (geometry != null) {
                regionService.AddRegion(dataFrameId, null, region);
            } else {
                log.warn("addActiveTimToDatabase - Unable to insert region, no path or geometry found (data_frame_id: {})",
                    dataFrameId);
            }
        }
    }

    public void addDataFrameItis(DataFrame dataFrame, Long dataFrameId) {
        // save DataFrame ITIS codes
        String[] items = dataFrame.getItems();
        if (items == null || items.length == 0) {
            log.warn("No itis codes found to associate with data_frame {}", dataFrameId);
            return;
        }
        for (var i = 0; i < items.length; i++) {
            var timItisCode = items[i];

            if (StringUtils.isNumeric(timItisCode)) {
                String itisCodeId = getItisCodeId(timItisCode);
                if (itisCodeId != null) {
                    dataFrameItisCodeService.insertDataFrameItisCode(dataFrameId, itisCodeId, i);
                } else {
                    log.warn("Could not find corresponding itis code id for {}", timItisCode);
                }
            } else {
                dataFrameItisCodeService.insertDataFrameItisCode(dataFrameId, timItisCode, i);
            }
        }
    }

    public boolean updateTimSatRecordId(Long timId, String satRecordId) {
        try (
            Connection connection = dbInteractions.getConnectionPool();
            PreparedStatement preparedStatement = connection.prepareStatement("update tim set sat_record_id = ? where tim_id = ?");
        ) {
            preparedStatement.setString(1, satRecordId);
            preparedStatement.setLong(2, timId);
            return dbInteractions.updateOrDelete(preparedStatement);
        } catch (Exception ex) {
            return false;
        }
    }

    public ActiveTim setActiveTimByRegionName(String regionName) {

        if (StringUtils.isBlank(regionName) || StringUtils.isEmpty(regionName)) {
            return null;
        }

        ActiveTim activeTim = new ActiveTim();
        RegionNameElementCollection elements = new RegionNameElementCollection(regionName);

        activeTim.setDirection(elements.direction);

        if (elements.rsuOrSat != null) {
            // if this is an RSU TIM
            String[] hyphen_array = elements.rsuOrSat.split("-");
            if (hyphen_array.length > 1) {
                if (hyphen_array[0].equals("SAT")) {
                    activeTim.setSatRecordId(hyphen_array[1]);
                } else {
                    activeTim.setRsuTarget(hyphen_array[1]);
                }
            }
        } else {
            return activeTim;
        }
        if (elements.timType != null) {
            TimType timType = getTimType(elements.timType);
            if (timType != null) {
                activeTim.setTimType(timType.getType());
                activeTim.setTimTypeId(timType.getTimTypeId());
            }
        } else {
            return activeTim;
        }

        if (elements.timId != null) {
            activeTim.setClientId(elements.timId);
        } else {
            return activeTim;
        }

        if (elements.pk != null) {
            try {
                Integer pk = Integer.valueOf(elements.pk);
                activeTim.setPk(pk);
            } catch (NumberFormatException ex) {
                // the pk won't get set here
            }
        }

        return activeTim;
    }

    public TimType getTimType(String timTypeName) {

        return timTypeService.getTimTypes().stream().filter(x -> x.getType().equals(timTypeName)).findFirst()
            .orElse(null);
    }

    public String getItisCodeId(String item) {

        String itisCodeId = null;

        try {
            ItisCode itisCode = itisCodeService.selectAllItisCodes().stream()
                .filter(x -> x.getItisCode().equals(Integer.parseInt(item))).findFirst().orElse(null);
            if (itisCode != null) {
                itisCodeId = itisCode.getItisCodeId().toString();
            }
        } catch (Exception ex) {
            // on rare occasions we see an unparsable Integer
            log.error("Failed to parse ITIS integer({}): {}", item, ex.getMessage());
        }

        return itisCodeId;
    }

    public boolean updateActiveTimExpiration(CertExpirationModel cem) throws ParseException {
        var minExp = activeTimService.getMinExpiration(cem.getPacketID(), cem.getExpirationDate());

        return activeTimService.updateActiveTimExpiration(cem.getPacketID(), minExp);
    }

    /**
     * Helper method to get an OdeTravelerInformationMessage object given an OdeTimPayload.
     */
    private OdeTravelerInformationMessage getTim(OdeTimPayload odeTimPayload) {
        return (OdeTravelerInformationMessage) odeTimPayload.getData();
    }
}