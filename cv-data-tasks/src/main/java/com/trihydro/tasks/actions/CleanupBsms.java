package com.trihydro.tasks.actions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.BsmCoreDataPartition;
import com.trihydro.library.service.UtilityService;
import com.trihydro.tasks.config.DataTasksConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CleanupBsms implements Runnable {
    private DataTasksConfiguration configuration;
    private UtilityService utilityService;
    private Utility utility;

    @Autowired
    public void InjectDependencies(DataTasksConfiguration configuration, UtilityService utilityService,
            Utility utility) {
        this.configuration = configuration;
        this.utilityService = utilityService;
        this.utility = utility;
    }

    @Override
    public void run() {
        try {
            var partitions = utilityService.getBsmCoreDataPartitions();
            var toRemove = new ArrayList<BsmCoreDataPartition>();
            var cutoff = new Date(
                    Instant.now().toEpochMilli() - (configuration.getBsmRetentionPeriodDays() * 86400000));

            for (var part : partitions) {
                if (part.getHighValue().compareTo(cutoff) < 0) {
                    toRemove.add(part);
                }
            }

            if (toRemove.size() > 0) {
                utility.logWithDate("Removing " + toRemove.size() + " partitions from BSM_CORE_DATA.", this.getClass());
                utilityService.dropBsmPartitions(
                        toRemove.stream().map(x -> x.getPartitionName()).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}