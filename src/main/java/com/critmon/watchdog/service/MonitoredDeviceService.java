package com.critmon.watchdog.service;

import com.critmon.watchdog.model.MonitoredDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoredDeviceService {
       private static final Logger log = LoggerFactory.getLogger(MonitoredDeviceService.class);
       private final Map<String, MonitoredDevice> monitoredDevices = new ConcurrentHashMap<>();

       //registers a device
       public MonitoredDevice register(String id, int timeoutSeconds, String alertMail) {
              if (monitoredDevices.containsKey(id)) {
                  throw new IllegalArgumentException("Device already exists");
              }
              if(timeoutSeconds <= 0){
                  throw new IllegalArgumentException("Timeout must be greater than 0");
              }

              MonitoredDevice device = new MonitoredDevice(id, timeoutSeconds, alertMail);
              monitoredDevices.put(id, device);
              log.info("[REGISTERED] Device {} expires at {}" , id, device.getExpiresAt());
              return device;
       }


       public Optional<MonitoredDevice> findById(String id){
           return Optional.ofNullable(monitoredDevices.get(id));
       }

       public MonitoredDevice heartbeat(String id) {
           MonitoredDevice device = getDeviceOrThrow(id);

           if (device.getStatus() == MonitoredDevice.Status.DOWN) {
               throw new IllegalStateException("Cannot heartbeat a DOWN device: " + id);
           }

           device.resetTimer();

           log.info("[HEARTBEAT] Device {} reset. Expired at {}" , id, device.getExpiresAt());
           return device;
       }

       public MonitoredDevice pause(String id) {
           MonitoredDevice device = getDeviceOrThrow(id);

           device.pause();

           log.info("[PAUSED] Device {} paused", id);
           return device;
       }

    public void checkExpiredMonitors() {
        Instant now = Instant.now();

        for (MonitoredDevice device : monitoredDevices.values()) {

            if (device.getStatus() == MonitoredDevice.Status.ACTIVE
                    && now.isAfter(device.getExpiresAt())) {

                device.setStatus(MonitoredDevice.Status.DOWN);

                log.error(
                        "[ALERT] Device {} is DOWN | time={} | email={}", device.getId(), now, device.getAlertMail());
            }
        }
    }
       
        private MonitoredDevice getDeviceOrThrow(String id) {
        MonitoredDevice device = monitoredDevices.get(id);

        if (device == null) {
            throw new IllegalArgumentException("Device not found: " + id);
        }

        return device;
    }

}

