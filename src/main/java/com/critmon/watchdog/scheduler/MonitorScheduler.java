package com.critmon.watchdog.scheduler;

import com.critmon.watchdog.service.MonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitorScheduler {

    private final MonitorService monitorService;

    public MonitorScheduler(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Scheduled(fixedRate = 5000) // runs every 5 seconds
    public void checkMonitors() {
        monitorService.checkExpiredMonitors();
    }
}