package com.trihydro.tasks.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.trihydro.tasks.models.CActiveTim;
import com.trihydro.tasks.models.CAdvisorySituationDataDeposit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class EmailConfiguration {
    private String formatMain;
    private String formatSection;

    public EmailConfiguration() throws IOException {
        formatMain = readFile("email-templates/main.html");
        formatSection = readFile("email-templates/section.html");
    }

    private String readFile(String path) throws IOException {
        File tmp = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(tmp.toPath()));
    }

    public String generateSdxSummaryEmail(int numSdxOrphaned, int numOutdatedSdx, int numNotOnSdx,
            List<CActiveTim> toResend, List<CAdvisorySituationDataDeposit> deleteFromSdx,
            List<CActiveTim> invOracleRecords) {
        // Create summary
        String body = formatMain.replaceAll("\\{num-stale\\}", Integer.toString(numOutdatedSdx));
        body = body.replaceAll("\\{num-sdx-orphaned\\}", Integer.toString(numSdxOrphaned));
        body = body.replaceAll("\\{num-not-present-sdx\\}", Integer.toString(numNotOnSdx));
        body = body.replaceAll("\\{num-inv-oracle\\}", Integer.toString(invOracleRecords.size()));

        // Add tables w/ detailed info, if available
        String content = "";

        // Invalid Oracle records
        if (invOracleRecords.size() > 0) {
            String section = formatSection.replaceAll("\\{title\\}", "Invalid Oracle records");
            section = section.replaceAll("\\{headers\\}", getHeader("ACTIVE_TIM_ID", "SAT_RECORD_ID"));

            String rows = "";
            for (CActiveTim record : invOracleRecords) {
                rows += getRow(Long.toString(record.getActiveTim().getActiveTimId()),
                        record.getActiveTim().getSatRecordId());
            }
            section = section.replaceAll("\\{rows\\}", rows);

            content += section;
        }

        // ActiveTims to resend to SDX
        if(toResend.size() > 0) {
            String section = formatSection.replaceAll("\\{title\\}", "ActiveTims to resend to SDX");
            section = section.replaceAll("\\{headers\\}", getHeader("ACTIVE_TIM_ID", "SAT_RECORD_ID"));

            String rows = "";
            for (CActiveTim record : toResend) {
                rows += getRow(Long.toString(record.getActiveTim().getActiveTimId()),
                        record.getActiveTim().getSatRecordId());
            }
            section = section.replaceAll("\\{rows\\}", rows);

            content += section;
        }

        // Orphaned records to delete from SDX
        if(deleteFromSdx.size() > 0) {
            String section = formatSection.replaceAll("\\{title\\}", "Orphaned records to delete from SDX");
            section = section.replaceAll("\\{headers\\}", getHeader("recordId"));

            String rows = "";
            for (CAdvisorySituationDataDeposit record : deleteFromSdx) {
                rows += getRow(Integer.toString(record.getRecordId()));
            }
            section = section.replaceAll("\\{rows\\}", rows);

            content += section;
        }

        body = body.replaceAll("\\{summary-tables\\}", content);

        return body;
    }

    private String getHeader(String... values) {
        String headers = "";
        for (String value : values) {
            headers += "<th>" + value + "</th>\n";
        }

        return headers;
    }

    private String getRow(String... values) {
        String row = "<tr>";
        for (String value : values) {
            row += "<td>" + value + "</td>";
        }

        row += "</tr>\n";
        return row;
    }
}