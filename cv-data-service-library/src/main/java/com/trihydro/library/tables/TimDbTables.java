package com.trihydro.library.tables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Getter
public class TimDbTables extends DbTables {

    // All lists are final and initialized in the constructor
    private final List<String> timTable;
    private final List<String> dataFrameTable;
    private final List<String> pathTable;
    private final List<String> regionTable;
    private final List<String> nodeXYTable;
    private final List<String> nodeLLTable;
    private final List<String> pathNodeXYTable;
    private final List<String> pathNodeLLTable;
    private final List<String> timTypeTable;
    private final List<String> activeTimTable;
    private final List<String> activeTimHoldingTable;
    private final List<String> timRsuTable;
    private final List<String> dataFrameItisCodeTable;

    public TimDbTables() {
        log.debug("Initializing TimDbTables");

        // Initialize all lists in the constructor
        timTable = createColumnList(
            "MSG_CNT", "PACKET_ID", "URL_B", "TIME_STAMP", "RECORD_GENERATED_BY",
            "RMD_LD_ELEVATION", "RMD_LD_HEADING", "RMD_LD_LATITUDE", "RMD_LD_LONGITUDE",
            "RMD_LD_SPEED", "RMD_RX_SOURCE", "SCHEMA_VERSION", "SECURITY_RESULT_CODE",
            "LOG_FILE_NAME", "RECORD_GENERATED_AT", "SANITIZED", "SERIAL_ID_STREAM_ID",
            "SERIAL_ID_BUNDLE_SIZE", "SERIAL_ID_BUNDLE_ID", "SERIAL_ID_RECORD_ID",
            "SERIAL_ID_SERIAL_NUMBER", "PAYLOAD_TYPE", "RECORD_TYPE", "ODE_RECEIVED_AT",
            "SAT_RECORD_ID"
        );

        dataFrameTable = createColumnList(
            "TIM_ID", "SSP_TIM_RIGHTS", "FRAME_TYPE", "DURATION_TIME",
            "PRIORITY", "SSP_LOCATION_RIGHTS", "SSP_MSG_TYPES",
            "SSP_MSG_CONTENT", "CONTENT", "URL", "START_DATE_TIME"
        );

        pathTable = createColumnList("SCALE");

        regionTable = createColumnList(
            "DATA_FRAME_ID", "NAME", "LANE_WIDTH", "DIRECTIONALITY", "DIRECTION",
            "CLOSED_PATH", "ANCHOR_LAT", "ANCHOR_LONG", "PATH_ID", "GEOMETRY_DIRECTION",
            "GEOMETRY_EXTENT", "GEOMETRY_LANE_WIDTH", "GEOMETRY_CIRCLE_POSITION_LAT",
            "GEOMETRY_CIRCLE_POSITION_LONG", "GEOMETRY_CIRCLE_POSITION_ELEV",
            "GEOMETRY_CIRCLE_RADIUS", "GEOMETRY_CIRCLE_UNITS"
        );

        pathNodeXYTable = createColumnList("NODE_XY_ID", "PATH_ID");

        pathNodeLLTable = createColumnList("NODE_LL_ID", "PATH_ID");

        nodeXYTable = createColumnList(
            "DELTA", "NODE_LAT", "NODE_LONG", "X", "Y",
            "ATTRIBUTES_DWIDTH", "ATTRIBUTES_DELEVATION"
        );

        nodeLLTable = createColumnList(
            "DELTA", "NODE_LAT", "NODE_LONG", "X", "Y",
            "ATTRIBUTES_DWIDTH", "ATTRIBUTES_DELEVATION"
        );

        timTypeTable = createColumnList("TYPE", "DESCRIPTION");

        activeTimTable = createColumnList(
            "TIM_ID", "DIRECTION", "TIM_START", "TIM_END", "TIM_TYPE_ID",
            "ROUTE", "CLIENT_ID", "SAT_RECORD_ID", "PK", "START_LATITUDE",
            "START_LONGITUDE", "END_LATITUDE", "END_LONGITUDE",
            "EXPIRATION_DATE", "PROJECT_KEY"
        );

        activeTimHoldingTable = createColumnList(
            "ACTIVE_TIM_HOLDING_ID", "CLIENT_ID", "DIRECTION", "RSU_TARGET",
            "SAT_RECORD_ID", "START_LATITUDE", "START_LONGITUDE", "END_LATITUDE",
            "END_LONGITUDE", "RSU_INDEX", "DATE_CREATED", "PROJECT_KEY",
            "EXPIRATION_DATE", "PACKET_ID", "TIM_END"
        );

        timRsuTable = createColumnList("TIM_ID", "RSU_ID", "RSU_INDEX");

        dataFrameItisCodeTable = createColumnList(
            "ITIS_CODE_ID", "DATA_FRAME_ID", "TEXT", "POSITION"
        );

        log.debug("TimDbTables initialization complete");
    }

    /**
     * Create a thread-safe, immutable list from column names
     */
    private List<String> createColumnList(String... columns) {
        Set<String> uniqueColumns = new LinkedHashSet<>();
        for (String column : columns) {
            if (!uniqueColumns.add(column)) {
                log.warn("Duplicate column detected and removed: {}", column);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(uniqueColumns));
    }
}