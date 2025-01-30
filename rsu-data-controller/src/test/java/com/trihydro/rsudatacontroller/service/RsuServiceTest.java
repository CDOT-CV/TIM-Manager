package com.trihydro.rsudatacontroller.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.helpers.Utility;
import com.trihydro.rsudatacontroller.config.BasicConfiguration;
import com.trihydro.rsudatacontroller.model.RsuTim;
import com.trihydro.rsudatacontroller.process.ProcessFactory;

import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.plugin.SnmpProtocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class RsuServiceTest {
    @Mock
    ProcessFactory mockProcessFactory;
    ArgumentCaptor<String> factoryArgs = ArgumentCaptor.forClass(String.class);

    @Mock
    Process mockProcess;

    @Mock
    BasicConfiguration mockConfig;

    @Mock
    Utility mockUtility;

    @Mock
    InputStream mockInputStream;

    @InjectMocks
    RsuService uut;

    public void initMocks() {
        when(mockConfig.getSnmpRetries()).thenReturn(0);
        when(mockConfig.getSnmpTimeoutSeconds()).thenReturn(10);
        when(mockConfig.getSnmpAuthProtocol()).thenReturn("protocol");
        when(mockConfig.getSnmpSecurityLevel()).thenReturn("level");
    }

    private void setupProcess() {
        when(mockProcessFactory.buildAndStartProcess(factoryArgs.capture())).thenReturn(mockProcess);
    }

    @Test
    public void getAllDeliveryStartTimes_success() throws Exception {
        // Arrange
        setupProcess();
        initMocks();
        InputStream output = getInputStream("iso.0.15628.4.1.4.1.7.2 = Hex-STRING: 07 E4 03 14 11 3B",
                "iso.0.15628.4.1.4.1.7.3 = Hex-STRING: 07 E4 03 14 13 1E");
        doReturn(output).when(mockProcess).getInputStream();
        RsuService spyUut = spy(uut);
        doReturn(new RSU("0.0.0.0", "username", "password", 3, 10, SnmpProtocol.FOURDOT1)).when(spyUut).getRSU(any());

        // Act
        List<RsuTim> results = spyUut.getAllDeliveryStartTimes("0.0.0.0");

        // Assert
        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals(2, (int) results.get(0).getIndex());
        Assertions.assertEquals("2020-03-20 17:59:00", results.get(0).getDeliveryStartTime());
        Assertions.assertEquals(3, (int) results.get(1).getIndex());
        Assertions.assertEquals("2020-03-20 19:30:00", results.get(1).getDeliveryStartTime());

        Assertions.assertEquals(
                "snmpwalk -v 3 -r 0 -t 10 -u username -l level -a protocol -A password 0.0.0.0 1.0.15628.4.1.4.1.7",
                String.join(" ", factoryArgs.getAllValues()));
    }

    @Test
    public void getAllDeliveryStartTimes_single() throws Exception {
        // Arrange
        setupProcess();
        initMocks();
        InputStream output = getInputStream("iso.0.15628.4.1.4.1.7.2 = Hex-STRING: 07 E4 03 14 11 3B ");
        doReturn(output).when(mockProcess).getInputStream();
        RsuService spyUut = spy(uut);
        doReturn(new RSU("0.0.0.0", "username", "password", 3, 10, SnmpProtocol.FOURDOT1)).when(spyUut).getRSU(any());

        // Act
        List<RsuTim> results = spyUut.getAllDeliveryStartTimes("0.0.0.0");

        // Assert
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(2, (int) results.get(0).getIndex());
        Assertions.assertEquals("2020-03-20 17:59:00", results.get(0).getDeliveryStartTime());

        Assertions.assertEquals(
                "snmpwalk -v 3 -r 0 -t 10 -u username -l level -a protocol -A password 0.0.0.0 1.0.15628.4.1.4.1.7",
                String.join(" ", factoryArgs.getAllValues()));
    }

    @Test
    public void getAllDeliveryStartTimes_none() throws Exception {
        // Arrange
        setupProcess();
        initMocks();
        InputStream output = getInputStream("");
        doReturn(output).when(mockProcess).getInputStream();
        RsuService spyUut = spy(uut);
        doReturn(new RSU("0.0.0.0", "username", "password", 3, 10, SnmpProtocol.FOURDOT1)).when(spyUut).getRSU(any());

        // Act
        List<RsuTim> results = spyUut.getAllDeliveryStartTimes("0.0.0.0");

        // Assert
        Assertions.assertEquals(0, results.size());

        Assertions.assertEquals(
                "snmpwalk -v 3 -r 0 -t 10 -u username -l level -a protocol -A password 0.0.0.0 1.0.15628.4.1.4.1.7",
                String.join(" ", factoryArgs.getAllValues()));
    }

    @Test
    public void getAllDeliveryStartTimes_snmpTimeout() throws Exception {
        // Arrange
        setupProcess();
        initMocks();
        InputStream output = getInputStream("snmpwalk: Timeout");
        doReturn(output).when(mockProcess).getInputStream();
        RsuService spyUut = spy(uut);
        doReturn(new RSU("0.0.0.0", "username", "password", 3, 10, SnmpProtocol.FOURDOT1)).when(spyUut).getRSU(any());

        // Act
        List<RsuTim> results = spyUut.getAllDeliveryStartTimes("0.0.0.0");

        // Assert
        Assertions.assertNull(results);
        verify(mockUtility).logWithDate(any());
    }

    @Test
    public void getAllDeliveryStartTimes_throwsRuntimeException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("unable to find snmpwalk command")).when(mockProcessFactory)
                .buildAndStartProcess(any());
        RsuService spyUut = spy(uut);
        doReturn(new RSU("0.0.0.0", "username", "password", 3, 10, SnmpProtocol.FOURDOT1)).when(spyUut).getRSU(any());

        // Act
        Assertions.assertThrows(RuntimeException.class, () -> spyUut.getAllDeliveryStartTimes("0.0.0.0"));
    }

    @Test
    public void getAllDeliveryStartTimes_throwsIOException() throws Exception {
        // Arrange
        setupProcess();
        initMocks();
        doThrow(new IOException("error occurred reading input stream")).when(mockInputStream).read(any(), anyInt(),
                anyInt());
        doReturn(mockInputStream).when(mockProcess).getInputStream();
        RsuService spyUut = spy(uut);
        doReturn(new RSU("0.0.0.0", "username", "password", 3, 10, SnmpProtocol.FOURDOT1)).when(spyUut).getRSU(any());

        // Act
        Assertions.assertThrows(IOException.class, () -> spyUut.getAllDeliveryStartTimes("0.0.0.0"));
    }

    @Test
    public void getRSU_Success() throws Exception {
        // Arrange
        String rsuIpv4Address = "192.168.1.1";
        String username = "testUser";
        String password = "testPass";
        String nickname = "NTCIP 1218";
        SnmpProtocol snmpProtocol = SnmpProtocol.NTCIP1218;

        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        RsuService spyUut = spy(uut);
        DbInteractions mockDbInteractions = mock(DbInteractions.class);

        when(mockDbInteractions.getConnectionPool()).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString("username")).thenReturn(username);
        when(mockResultSet.getString("password")).thenReturn(password);
        when(mockResultSet.getString("nickname")).thenReturn(nickname);

        spyUut.dbInteractions = mockDbInteractions;

        // Act
        RSU result = spyUut.getRSU(rsuIpv4Address);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(rsuIpv4Address, result.getRsuTarget());
        Assertions.assertEquals(username, result.getRsuUsername());
        Assertions.assertEquals(password, result.getRsuPassword());
        Assertions.assertEquals(snmpProtocol, result.getSnmpProtocol());

        verify(mockConnection).close();
        verify(mockStatement).close();
        verify(mockResultSet).close();
    }

    @Test
    public void getRSU_NoResult() throws Exception {
        // Arrange
        String rsuIpv4Address = "192.168.1.1";

        Connection mockConnection = mock(Connection.class);
        Statement mockStatement = mock(Statement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        DbInteractions mockDbInteractions = mock(DbInteractions.class);

        RsuService spyUut = spy(uut);

        when(mockDbInteractions.getConnectionPool()).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        spyUut.dbInteractions = mockDbInteractions;

        // Act
        RSU result = spyUut.getRSU(rsuIpv4Address);

        // Assert
        Assertions.assertNull(result);

        verify(mockConnection).close();
        verify(mockStatement).close();
        verify(mockResultSet).close();
    }

    private InputStream getInputStream(String... lines) {
        String contents = String.join("\n", lines);

        return new ByteArrayInputStream(contents.getBytes());
    }
}