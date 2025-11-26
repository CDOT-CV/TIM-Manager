package com.trihydro.tasks.actions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.mail.MessagingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.trihydro.library.helpers.EmailHelper;
import com.trihydro.library.helpers.TimGenerationHelper;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.ActiveTim;
import com.trihydro.library.model.AdvisorySituationDataDeposit;
import com.trihydro.library.model.ResubmitTimException;
import com.trihydro.library.model.SemiDialogID;
import com.trihydro.library.service.ActiveTimService;
import com.trihydro.library.service.SdwService;
import com.trihydro.tasks.config.DataTasksConfiguration;
import com.trihydro.tasks.helpers.EmailFormatter;
import com.trihydro.tasks.models.CActiveTim;
import com.trihydro.tasks.models.CAdvisorySituationDataDeposit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;

@ExtendWith(MockitoExtension.class)
class ValidateSdxTest {
    // Mocked dependencies
    @Mock
    EmailHelper mockEmailHelper;
    @Mock
    SdwService mockSdwService;
    @Mock
    ActiveTimService mockActiveTimService;
    @Mock
    EmailFormatter mockEmailFormatter;
    @Mock
    DataTasksConfiguration mockConfig;
    @Mock
    Utility mockUtility;
    @Mock
    TimGenerationHelper mockTimGenerationHelper;

    // Argument Captors
    @Captor
    ArgumentCaptor<List<CActiveTim>> toResendCaptor;
    @Captor
    ArgumentCaptor<List<CAdvisorySituationDataDeposit>> deleteFromSdxCaptor;
    @Captor
    ArgumentCaptor<List<CActiveTim>> invDbRecordsCaptor;
    @Captor
    ArgumentCaptor<String> exceptionMessageCaptor;

    final Gson gson = createGson();

    // Unit under test
    @InjectMocks
    ValidateSdx uut;

    @Test
    void validateSDX_run_noRecords() throws MailException, MessagingException, SdwService.SdwServiceException {
        uut.run();

        // Services were called
        verify(mockSdwService).getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        verify(mockActiveTimService).getActiveTimsForSDX();

        // Email was sent
        verify(mockEmailHelper, times(1)).SendEmail(any(), any(), any());
    }

    @Test
    void validateSDX_run_allValid() throws MailException, MessagingException, SdwService.SdwServiceException {
        // 2 Active TIMs, 2 SDX records. Both Active TIMs correspond to SDX records with matching ITIS codes.
        List<ActiveTim> activeTims = new ArrayList<>();
        List<AdvisorySituationDataDeposit> asdds = new ArrayList<>();

        ActiveTim activeTim1 = new ActiveTim();
        activeTim1.setActiveTimId(100L);
        activeTim1.setSatRecordId("00000000");
        activeTim1.setItisCodes(Arrays.asList(8, 7, 6));
        activeTim1.setEndDateTime("2099-12-31 23:59:59");
        activeTims.add(activeTim1);

        AdvisorySituationDataDeposit sdxRecord1 = new AdvisorySituationDataDeposit();
        sdxRecord1.setRecordId(0);
        sdxRecord1.setAdvisoryMessage("0");
        asdds.add(sdxRecord1);
        doReturn(Arrays.asList(8, 7, 6)).when(mockSdwService).getItisCodesFromAdvisoryMessage("0");

        ActiveTim activeTim2 = new ActiveTim();
        activeTim2.setActiveTimId(101L);
        activeTim2.setSatRecordId("00000001");
        activeTim2.setItisCodes(Arrays.asList(8, 7, 6));
        activeTim2.setEndDateTime("2099-12-31 23:59:59");
        activeTims.add(activeTim2);

        AdvisorySituationDataDeposit sdxRecord2 = new AdvisorySituationDataDeposit();
        sdxRecord2.setRecordId(1);
        sdxRecord2.setAdvisoryMessage("1");
        asdds.add(sdxRecord2);
        doReturn(Arrays.asList(8, 7, 6)).when(mockSdwService).getItisCodesFromAdvisoryMessage("1");

        // Arrange service responses
        when(mockSdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep)).thenReturn(asdds);
        when(mockActiveTimService.getActiveTimsForSDX()).thenReturn(activeTims);

        // Act
        uut.run();

        // Assert
        // Services were called
        verify(mockSdwService).getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        verify(mockActiveTimService).getActiveTimsForSDX();
        verify(mockSdwService, times(2)).getItisCodesFromAdvisoryMessage(any());

        // Email was sent
        verify(mockEmailHelper, times(1)).SendEmail(any(), any(), any());

    }

    @Test
    void validateSDX_noSdx() throws MailException, MessagingException, SdwService.SdwServiceException {
        // Arrange
        // 2 Active TIMs, 0 SDX.
        List<ActiveTim> activeTims = new ArrayList<>();
        ActiveTim activeTim1 = new ActiveTim();
        activeTim1.setActiveTimId(100L);
        activeTim1.setSatRecordId("00000000");
        activeTim1.setItisCodes(Arrays.asList(8, 7, 6));
        activeTim1.setEndDateTime("2099-12-31 23:59:59");
        activeTims.add(activeTim1);
        ActiveTim activeTim2 = new ActiveTim();
        activeTim2.setActiveTimId(101L);
        activeTim2.setSatRecordId("00000001");
        activeTim2.setItisCodes(Arrays.asList(8, 7, 6));
        activeTim2.setEndDateTime("2099-12-31 23:59:59");
        activeTims.add(activeTim2);

        List<AdvisorySituationDataDeposit> asdds = new ArrayList<>();

        // Arrange service responses
        when(mockSdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep)).thenReturn(asdds);
        when(mockActiveTimService.getActiveTimsForSDX()).thenReturn(activeTims);
        List<ResubmitTimException> resubExs = new ArrayList<>();
        resubExs.add(new ResubmitTimException(-1L, "Unit test exception"));
        when(mockTimGenerationHelper.resubmitToOde(any())).thenReturn(resubExs);

        // Act
        uut.run();

        // Assert
        // 2 Active TIMs, with 0 records in the SDX. We're expecting:
        // - Number of Database records without corresponding message in SDX: 2
        // - toResend to contain 2 records

        // Services were called
        verify(mockSdwService).getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        verify(mockActiveTimService).getActiveTimsForSDX();

        // Email was sent
        verify(mockEmailHelper).SendEmail(any(), any(), any());

        // Email had expected counts
        verify(mockEmailFormatter).generateSdxSummaryEmail(eq(0), eq(0), eq(2), toResendCaptor.capture(),
                deleteFromSdxCaptor.capture(), invDbRecordsCaptor.capture(), exceptionMessageCaptor.capture());

        Assertions.assertEquals(2, toResendCaptor.getValue().size());
        Assertions.assertEquals(0, deleteFromSdxCaptor.getValue().size());
        Assertions.assertEquals(0, invDbRecordsCaptor.getValue().size());

        Gson gson = new Gson();
        StringBuilder expectedExceptionText = new StringBuilder("The following exceptions were found while attempting to resubmit TIMs: ");
        expectedExceptionText.append("<br/>");
        for (ResubmitTimException rte : resubExs) {
            expectedExceptionText.append(gson.toJson(rte));
            expectedExceptionText.append("<br/>");
        }
        expectedExceptionText.append("Note: TIM resubmission failures may indicate issues with the TIM data or the ODE service. Please investigate the failed resubmissions and consider reaching out to the ODE support team for assistance.<br>");
        Assertions.assertEquals(expectedExceptionText.toString(), exceptionMessageCaptor.getValue());
    }

    @Test
    void validateSDX_noDatabase() throws MailException, MessagingException, SdwService.SdwServiceException, IOException {
        // Arrange
        // 0 Active TIMS, 2 SDX.
        List<ActiveTim> activeTims = new ArrayList<>();
        List<AdvisorySituationDataDeposit> asdds = new ArrayList<>();
        AdvisorySituationDataDeposit asdd1 = new AdvisorySituationDataDeposit();
        asdd1.setRecordId(0);
        asdd1.setAdvisoryMessage("0");
        asdds.add(asdd1);
        AdvisorySituationDataDeposit asdd2 = new AdvisorySituationDataDeposit();
        asdd2.setRecordId(1);
        asdd2.setAdvisoryMessage("1");
        asdds.add(asdd2);
        doReturn(Arrays.asList(8, 7, 6)).when(mockSdwService).getItisCodesFromAdvisoryMessage("0");
        doReturn(Arrays.asList(8, 7, 6)).when(mockSdwService).getItisCodesFromAdvisoryMessage("1");

        // Arrange service responses
        when(mockSdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep)).thenReturn(asdds);
        when(mockActiveTimService.getActiveTimsForSDX()).thenReturn(activeTims);

        HashMap<Integer, Boolean> sdxDelResults = new HashMap<>();
        sdxDelResults.put(0, true);
        sdxDelResults.put(1, false);
        when(mockSdwService.deleteSdxDataByRecordIdIntegers(any())).thenReturn(sdxDelResults);

        // Act
        uut.run();

        // Assert
        // 0 Active TIMs, with 2 records in the SDX. We're expecting:
        // - Number of messages on SDX without corresponding Database record: 2
        // - deleteFromSdx to contain 2 records

        // Services were called
        verify(mockSdwService).getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        verify(mockActiveTimService).getActiveTimsForSDX();

        // Email was sent
        verify(mockEmailHelper).SendEmail(any(), any(), any());

        // Email had expected counts
        verify(mockEmailFormatter).generateSdxSummaryEmail(eq(2), eq(0), eq(0), toResendCaptor.capture(),
                deleteFromSdxCaptor.capture(), invDbRecordsCaptor.capture(), exceptionMessageCaptor.capture());

        Assertions.assertEquals(0, toResendCaptor.getValue().size());
        Assertions.assertEquals(2, deleteFromSdxCaptor.getValue().size());
        Assertions.assertEquals(0, invDbRecordsCaptor.getValue().size());

        String expectedExceptionText = "The following recordIds failed to delete from the SDX: 1<br>Note: SDX deletion failures may indicate errant data in the SDX. Please investigate the failed deletions and consider reaching out to the SDX support team for assistance.<br>";
        Assertions.assertEquals(expectedExceptionText, exceptionMessageCaptor.getValue());
    }

    @Test
    void validateSDX_mixSuccess() throws MailException, MessagingException, SdwService.SdwServiceException {
        // 3 Active TIMs, 3 SDX records.
        // 2 Active TIM and SDX records are aligned. Of those, 1 pair is accurate while
        // another is "stale".
        // The last Active TIM isn't present on the SDX, and the last SDX record is
        // orphaned.
        List<ActiveTim> activeTims = new ArrayList<>();
        List<AdvisorySituationDataDeposit> asdds = new ArrayList<>();

        // Create corresponding pair of records with matching ITIS codes, should result in no action
        ActiveTim at1 = new ActiveTim(); // corresponds to asdd1
        at1.setActiveTimId(100L);
        at1.setSatRecordId("00000000");
        at1.setItisCodes(Arrays.asList(8, 7, 6));
        at1.setEndDateTime("2099-12-31 23:59:59");
        activeTims.add(at1);

        AdvisorySituationDataDeposit a1 = new AdvisorySituationDataDeposit(); // corresponds to at1
        a1.setRecordId(0);
        a1.setAdvisoryMessage("0");
        asdds.add(a1);
        doReturn(Arrays.asList(8, 7, 6)).when(mockSdwService).getItisCodesFromAdvisoryMessage("0");

        // Create corresponding pair of records with differing ITIS codes, should be marked for resend
        ActiveTim at2 = new ActiveTim(); // corresponds to asdd2, but is stale
        at2.setActiveTimId(101L);
        at2.setSatRecordId("00000001");
        at2.setItisCodes(Arrays.asList(1, 2, 3));
        at2.setEndDateTime("2099-12-31 23:59:59");

        AdvisorySituationDataDeposit a2 = new AdvisorySituationDataDeposit(); // corresponds to at2, but is stale
        a2.setRecordId(1);
        a2.setAdvisoryMessage("-1");
        asdds.add(a2);
        doReturn(List.of(0)).when(mockSdwService).getItisCodesFromAdvisoryMessage("-1"); // stale record

        // Create ActiveTim with no corresponding ASDD
        activeTims.add(at2);
        ActiveTim at3 = new ActiveTim(); // no corresponding ASDD
        at3.setActiveTimId(102L);
        at3.setSatRecordId("00000017");
        at3.setItisCodes(Arrays.asList(17, 16, 18));
        at3.setEndDateTime("2099-12-31 23:59:59");
        activeTims.add(at3);

        // Create ASDD with no corresponding ActiveTim
        AdvisorySituationDataDeposit a3 = new AdvisorySituationDataDeposit(); // no corresponding ActiveTim
        a3.setRecordId(11);
        a3.setAdvisoryMessage("1");
        asdds.add(a3);
        doReturn(Arrays.asList(17, 16, 18)).when(mockSdwService).getItisCodesFromAdvisoryMessage("1");

        // Arrange service responses
        when(mockSdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep)).thenReturn(asdds);
        when(mockActiveTimService.getActiveTimsForSDX()).thenReturn(activeTims);

        HashMap<Integer, Boolean> sdxDelResults = new HashMap<>();
        sdxDelResults.put(0, true);
        sdxDelResults.put(1, false);
        when(mockSdwService.deleteSdxDataByRecordIdIntegers(any())).thenReturn(sdxDelResults);

        List<ResubmitTimException> resubExs = new ArrayList<>();
        resubExs.add(new ResubmitTimException(-1L, "Unit test exception"));
        when(mockTimGenerationHelper.resubmitToOde(any())).thenReturn(resubExs);

        // Act
        uut.run();

        // Assert
        // We're expecting:
        // - Number of stale records on SDX (different ITIS codes than ActiveTim): 1
        // - Number of messages on SDX without corresponding Database record: 1
        // - Number of Database records without corresponding message in SDX: 1
        // - toResend count: 2
        // - deleteFromSdx count: 1
        // - invDbRecords count: 0

        // Services were called
        verify(mockSdwService).getMsgsForOdeUser(SemiDialogID.AdvSitDataDep);
        verify(mockActiveTimService).getActiveTimsForSDX();
        verify(mockSdwService, times(3)).getItisCodesFromAdvisoryMessage(any());

        // Email was sent
        verify(mockEmailHelper).SendEmail(any(), any(), any());

        // Email had expected counts
        verify(mockEmailFormatter).generateSdxSummaryEmail(eq(1), eq(1), eq(1), toResendCaptor.capture(),
                deleteFromSdxCaptor.capture(), invDbRecordsCaptor.capture(), exceptionMessageCaptor.capture());

        Assertions.assertEquals(2, toResendCaptor.getValue().size());
        Assertions.assertEquals(1, deleteFromSdxCaptor.getValue().size());
        Assertions.assertEquals(0, invDbRecordsCaptor.getValue().size());

        Gson gson = new Gson();
        StringBuilder expectedExceptionText = new StringBuilder("The following recordIds failed to delete from the SDX: 1<br>");
        expectedExceptionText.append("The following exceptions were found while attempting to resubmit TIMs: ");
        expectedExceptionText.append("<br/>");
        for (ResubmitTimException rte : resubExs) {
            expectedExceptionText.append(gson.toJson(rte));
            expectedExceptionText.append("<br/>");
        }
        expectedExceptionText.append("Note: SDX deletion failures may indicate errant data in the SDX. Please investigate the failed deletions and consider reaching out to the SDX support team for assistance.<br>Note: TIM resubmission failures may indicate issues with the TIM data or the ODE service. Please investigate the failed resubmissions and consider reaching out to the ODE support team for assistance.<br>");
        Assertions.assertEquals(expectedExceptionText.toString(), exceptionMessageCaptor.getValue());
    }

    @Test
    void validateSDX_run_emptyDeleteList() throws MailException, MessagingException, SdwService.SdwServiceException {
        // Arrange
        // Setup active TIMs and SDX records where nothing needs to be deleted
        ActiveTim[] activeTims = new ActiveTim[1];
        ActiveTim activeTim = new ActiveTim();
        activeTim.setActiveTimId(100L);
        activeTim.setSatRecordId("00000000");
        activeTim.setItisCodes(Arrays.asList(8, 7, 6));
        activeTim.setEndDateTime("2099-12-31 23:59:59");
        activeTims[0] = activeTim;

        AdvisorySituationDataDeposit[] asdds = new AdvisorySituationDataDeposit[1];
        AdvisorySituationDataDeposit asdd = new AdvisorySituationDataDeposit();
        asdd.setRecordId(0);
        asdd.setAdvisoryMessage("0");
        asdds[0] = asdd;

        when(mockSdwService.getMsgsForOdeUser(SemiDialogID.AdvSitDataDep)).thenReturn(Arrays.asList(asdds));
        when(mockActiveTimService.getActiveTimsForSDX()).thenReturn(Arrays.asList(activeTims));
        doReturn(Arrays.asList(8, 7, 6)).when(mockSdwService).getItisCodesFromAdvisoryMessage("0");

        // Act
        uut.run();

        // Assert
        // Verify SDX deletion was not called since there were no records to delete
        verify(mockSdwService, times(0)).deleteSdxDataByRecordIdIntegers(any());

        // Email should be sent since there were no discrepancies
        verify(mockEmailHelper, times(1)).SendEmail(any(), any(), any());
    }

    /**
     * Creates a Gson instance with a LocalDateTime adapter to avoid reflection issues.
     * @return Gson instance
     */
    static Gson createGson() {
        return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
            @Override
            public void write(JsonWriter out, LocalDateTime value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public LocalDateTime read(JsonReader in) throws IOException {
                return LocalDateTime.parse(in.nextString());
            }
        }).create();
    }
}