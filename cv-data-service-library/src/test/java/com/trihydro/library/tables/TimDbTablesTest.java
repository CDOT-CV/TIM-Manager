package com.trihydro.library.tables;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TimDbTablesTest {

    private TimDbTables timDbTables;

    @BeforeEach
    void setUp() {
        timDbTables = new TimDbTables();
    }

    @Test
    void testGetTimTable() {
        // Act
        List<String> timTable = timDbTables.getTimTable();

        // Assert
        assertNotNull(timTable);
        assertEquals(25, timTable.size());
        assertTrue(timTable.contains("MSG_CNT"));
        assertTrue(timTable.contains("PACKET_ID"));
        assertTrue(timTable.contains("URL_B"));
        assertTrue(timTable.contains("TIME_STAMP"));
        assertTrue(timTable.contains("RECORD_GENERATED_BY"));
        assertTrue(timTable.contains("RMD_LD_ELEVATION"));
        assertTrue(timTable.contains("RMD_LD_HEADING"));
        assertTrue(timTable.contains("RMD_LD_LATITUDE"));
        assertTrue(timTable.contains("RMD_LD_LONGITUDE"));
        assertTrue(timTable.contains("RMD_LD_SPEED"));
        assertTrue(timTable.contains("RMD_RX_SOURCE"));
        assertTrue(timTable.contains("SCHEMA_VERSION"));
        assertTrue(timTable.contains("SECURITY_RESULT_CODE"));
        assertTrue(timTable.contains("LOG_FILE_NAME"));
        assertTrue(timTable.contains("RECORD_GENERATED_AT"));
        assertTrue(timTable.contains("SANITIZED"));
        assertTrue(timTable.contains("SERIAL_ID_STREAM_ID"));
        assertTrue(timTable.contains("SERIAL_ID_BUNDLE_SIZE"));
        assertTrue(timTable.contains("SERIAL_ID_BUNDLE_ID"));
        assertTrue(timTable.contains("SERIAL_ID_RECORD_ID"));
        assertTrue(timTable.contains("SERIAL_ID_SERIAL_NUMBER"));
        assertTrue(timTable.contains("PAYLOAD_TYPE"));
        assertTrue(timTable.contains("RECORD_TYPE"));
        assertTrue(timTable.contains("ODE_RECEIVED_AT"));
        assertTrue(timTable.contains("SAT_RECORD_ID"));
    }

    @Test
    void testGetDataFrameTable() {
        // Act
        List<String> dataFrameTable = timDbTables.getDataFrameTable();

        // Assert
        assertNotNull(dataFrameTable);
        assertEquals(11, dataFrameTable.size());
        assertTrue(dataFrameTable.contains("TIM_ID"));
        assertTrue(dataFrameTable.contains("SSP_TIM_RIGHTS"));
        assertTrue(dataFrameTable.contains("FRAME_TYPE"));
        assertTrue(dataFrameTable.contains("DURATION_TIME"));
        assertTrue(dataFrameTable.contains("PRIORITY"));
        assertTrue(dataFrameTable.contains("SSP_LOCATION_RIGHTS"));
        assertTrue(dataFrameTable.contains("SSP_MSG_TYPES"));
        assertTrue(dataFrameTable.contains("SSP_MSG_CONTENT"));
        assertTrue(dataFrameTable.contains("CONTENT"));
        assertTrue(dataFrameTable.contains("URL"));
        assertTrue(dataFrameTable.contains("START_DATE_TIME"));
    }

    @Test
    void testGetPathTable() {
        // Act
        List<String> pathTable = timDbTables.getPathTable();

        // Assert
        assertNotNull(pathTable);
        assertEquals(1, pathTable.size());
        assertTrue(pathTable.contains("SCALE"));
    }

    @Test
    void testGetRegionTable() {
        // Act
        List<String> regionTable = timDbTables.getRegionTable();

        // Assert
        assertNotNull(regionTable);
        assertEquals(17, regionTable.size());
        assertTrue(regionTable.contains("DATA_FRAME_ID"));
        assertTrue(regionTable.contains("NAME"));
        assertTrue(regionTable.contains("LANE_WIDTH"));
        assertTrue(regionTable.contains("DIRECTIONALITY"));
        assertTrue(regionTable.contains("DIRECTION"));
        assertTrue(regionTable.contains("CLOSED_PATH"));
        assertTrue(regionTable.contains("ANCHOR_LAT"));
        assertTrue(regionTable.contains("ANCHOR_LONG"));
        assertTrue(regionTable.contains("PATH_ID"));
        assertTrue(regionTable.contains("GEOMETRY_DIRECTION"));
        assertTrue(regionTable.contains("GEOMETRY_EXTENT"));
        assertTrue(regionTable.contains("GEOMETRY_LANE_WIDTH"));
        assertTrue(regionTable.contains("GEOMETRY_CIRCLE_POSITION_LAT"));
        assertTrue(regionTable.contains("GEOMETRY_CIRCLE_POSITION_LONG"));
        assertTrue(regionTable.contains("GEOMETRY_CIRCLE_POSITION_ELEV"));
        assertTrue(regionTable.contains("GEOMETRY_CIRCLE_RADIUS"));
        assertTrue(regionTable.contains("GEOMETRY_CIRCLE_UNITS"));
    }

    @Test
    void testGetPathNodeXYTable() {
        // Act
        List<String> pathNodeXYTable = timDbTables.getPathNodeXYTable();

        // Assert
        assertNotNull(pathNodeXYTable);
        assertEquals(2, pathNodeXYTable.size());
        assertTrue(pathNodeXYTable.contains("NODE_XY_ID"));
        assertTrue(pathNodeXYTable.contains("PATH_ID"));
    }

    @Test
    void testGetPathNodeLLTable() {
        // Act
        List<String> pathNodeLLTable = timDbTables.getPathNodeLLTable();

        // Assert
        assertNotNull(pathNodeLLTable);
        assertEquals(2, pathNodeLLTable.size());
        assertTrue(pathNodeLLTable.contains("NODE_LL_ID"));
        assertTrue(pathNodeLLTable.contains("PATH_ID"));
    }

    @Test
    void testGetNodeXYTable() {
        // Act
        List<String> nodeXYTable = timDbTables.getNodeXYTable();

        // Assert
        assertNotNull(nodeXYTable);
        assertEquals(7, nodeXYTable.size());
        assertTrue(nodeXYTable.contains("DELTA"));
        assertTrue(nodeXYTable.contains("NODE_LAT"));
        assertTrue(nodeXYTable.contains("NODE_LONG"));
        assertTrue(nodeXYTable.contains("X"));
        assertTrue(nodeXYTable.contains("Y"));
        assertTrue(nodeXYTable.contains("ATTRIBUTES_DWIDTH"));
        assertTrue(nodeXYTable.contains("ATTRIBUTES_DELEVATION"));
    }

    @Test
    void testGetNodeLLTable() {
        // Act
        List<String> nodeLLTable = timDbTables.getNodeLLTable();

        // Assert
        assertNotNull(nodeLLTable);
        assertEquals(7, nodeLLTable.size());
        assertTrue(nodeLLTable.contains("DELTA"));
        assertTrue(nodeLLTable.contains("NODE_LAT"));
        assertTrue(nodeLLTable.contains("NODE_LONG"));
        assertTrue(nodeLLTable.contains("X"));
        assertTrue(nodeLLTable.contains("Y"));
        assertTrue(nodeLLTable.contains("ATTRIBUTES_DWIDTH"));
        assertTrue(nodeLLTable.contains("ATTRIBUTES_DELEVATION"));
    }

    @Test
    void testGetTimTypeTable() {
        // Act
        List<String> timTypeTable = timDbTables.getTimTypeTable();

        // Assert
        assertNotNull(timTypeTable);
        assertEquals(2, timTypeTable.size());
        assertTrue(timTypeTable.contains("TYPE"));
        assertTrue(timTypeTable.contains("DESCRIPTION"));
    }

    @Test
    void testGetActiveTimTable() {
        // Act
        List<String> activeTimTable = timDbTables.getActiveTimTable();

        // Assert
        assertNotNull(activeTimTable);
        assertEquals(15, activeTimTable.size());
        assertTrue(activeTimTable.contains("TIM_ID"));
        assertTrue(activeTimTable.contains("DIRECTION"));
        assertTrue(activeTimTable.contains("TIM_START"));
        assertTrue(activeTimTable.contains("TIM_END"));
        assertTrue(activeTimTable.contains("TIM_TYPE_ID"));
        assertTrue(activeTimTable.contains("ROUTE"));
        assertTrue(activeTimTable.contains("CLIENT_ID"));
        assertTrue(activeTimTable.contains("SAT_RECORD_ID"));
        assertTrue(activeTimTable.contains("PK"));
        assertTrue(activeTimTable.contains("START_LATITUDE"));
        assertTrue(activeTimTable.contains("START_LONGITUDE"));
        assertTrue(activeTimTable.contains("END_LATITUDE"));
        assertTrue(activeTimTable.contains("END_LONGITUDE"));
        assertTrue(activeTimTable.contains("EXPIRATION_DATE"));
        assertTrue(activeTimTable.contains("PROJECT_KEY"));
    }

    @Test
    void testGetActiveTimHoldingTable() {
        // Act
        List<String> activeTimHoldingTable = timDbTables.getActiveTimHoldingTable();

        // Assert
        assertNotNull(activeTimHoldingTable);
        assertEquals(15, activeTimHoldingTable.size());
        assertTrue(activeTimHoldingTable.contains("ACTIVE_TIM_HOLDING_ID"));
        assertTrue(activeTimHoldingTable.contains("CLIENT_ID"));
        assertTrue(activeTimHoldingTable.contains("DIRECTION"));
        assertTrue(activeTimHoldingTable.contains("RSU_TARGET"));
        assertTrue(activeTimHoldingTable.contains("SAT_RECORD_ID"));
        assertTrue(activeTimHoldingTable.contains("START_LATITUDE"));
        assertTrue(activeTimHoldingTable.contains("START_LONGITUDE"));
        assertTrue(activeTimHoldingTable.contains("END_LATITUDE"));
        assertTrue(activeTimHoldingTable.contains("END_LONGITUDE"));
        assertTrue(activeTimHoldingTable.contains("RSU_INDEX"));
        assertTrue(activeTimHoldingTable.contains("DATE_CREATED"));
        assertTrue(activeTimHoldingTable.contains("PROJECT_KEY"));
        assertTrue(activeTimHoldingTable.contains("EXPIRATION_DATE"));
        assertTrue(activeTimHoldingTable.contains("PACKET_ID"));
        assertTrue(activeTimHoldingTable.contains("TIM_END"));
    }

    @Test
    void testGetTimRsuTable() {
        // Act
        List<String> timRsuTable = timDbTables.getTimRsuTable();

        // Assert
        assertNotNull(timRsuTable);
        assertEquals(3, timRsuTable.size());
        assertTrue(timRsuTable.contains("TIM_ID"));
        assertTrue(timRsuTable.contains("RSU_ID"));
        assertTrue(timRsuTable.contains("RSU_INDEX"));
    }

    @Test
    void testGetDataFrameItisCodeTable() {
        // Act
        List<String> dataFrameItisCodeTable = timDbTables.getDataFrameItisCodeTable();

        // Assert
        assertNotNull(dataFrameItisCodeTable);
        assertEquals(4, dataFrameItisCodeTable.size());
        assertTrue(dataFrameItisCodeTable.contains("ITIS_CODE_ID"));
        assertTrue(dataFrameItisCodeTable.contains("DATA_FRAME_ID"));
        assertTrue(dataFrameItisCodeTable.contains("TEXT"));
        assertTrue(dataFrameItisCodeTable.contains("POSITION"));
    }

    @Test
    public void testGetActiveTimHoldingTableThreadSafety() throws InterruptedException {
        // Number of threads to use in the test
        int numThreads = 10;

        // Create a countdown latch to synchronize thread starts
        CountDownLatch startLatch = new CountDownLatch(1);

        // Create a countdown latch to wait for all threads to finish
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        // Create an executor service with a fixed thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        // Create an atomic reference to store any exception that might occur
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();

        // Lists obtained by each thread
        List<String>[] obtainedLists = new List[numThreads];

        // Submit tasks to the executor service
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    // Wait for the start signal
                    startLatch.await();

                    // Get the list
                    obtainedLists[threadIndex] = timDbTables.getActiveTimHoldingTable();

                    // Check for duplicate columns within the list
                    Set<String> seenColumns = new HashSet<>();
                    for (String column : obtainedLists[threadIndex]) {
                        if (!seenColumns.add(column.toLowerCase())) {
                            throw new RuntimeException("Thread " + threadIndex +
                                " found duplicate column: " + column);
                        }
                    }
                } catch (Exception e) {
                    exceptionRef.set(e);
                } finally {
                    // Count down the finish latch
                    finishLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to finish
        boolean allThreadsFinished = finishLatch.await(10, TimeUnit.SECONDS);

        // Shutdown the executor service
        executorService.shutdown();

        // Assert that all threads finished
        assertTrue(allThreadsFinished, "Not all threads finished in time");

        // Check if any thread encountered an exception
        if (exceptionRef.get() != null) {
            fail("Thread encountered an exception: " + exceptionRef.get().getMessage());
        }

        // Check that all threads got the same list reference (singleton behavior)
        for (int i = 1; i < numThreads; i++) {
            assertSame(obtainedLists[0], obtainedLists[i],
                "Thread " + i + " got a different list instance than thread 0");
        }

        // Check that the list obtained by the first thread has no duplicates
        Set<String> uniqueColumns = new HashSet<>();
        for (String column : obtainedLists[0]) {
            boolean added = uniqueColumns.add(column.toLowerCase());
            assertTrue(added, "Found duplicate column in the final list: " + column);
        }

        // Verify the list size matches the expected number of unique columns
        assertEquals(uniqueColumns.size(), obtainedLists[0].size(),
            "List size does not match the expected number of unique columns");

        // Additional test: try to modify the list and see if it affects other threads
        try {
            obtainedLists[0].add("TEST_COLUMN");
            fail("List should be unmodifiable or thread-safe");
        } catch (UnsupportedOperationException e) {
            // This is actually good - it means the list is immutable
        } catch (Exception e) {
            // Any other exception is unexpected
            fail("Unexpected exception when trying to modify the list: " + e.getMessage());
        }
    }
}