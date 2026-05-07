package com.critmon.watchdog.service;

import com.critmon.watchdog.model.MonitoredDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoredDeviceService {
       private static final Logger log = LoggerFactory.getLogger(MonitoredDeviceService.class); //creates logger
       private final Map<String, MonitoredDevice> monitoredDevices = new ConcurrentHashMap<>();// in memory db

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

        //find a device logic
       public Optional<MonitoredDevice> findById(String id){
           return Optional.ofNullable(monitoredDevices.get(id));
       }

        //heartbeat logic
       public MonitoredDevice heartbeat(String id) {
           MonitoredDevice device = getDeviceOrThrow(id);

           if (device.getStatus() == MonitoredDevice.Status.DOWN) {
               throw new IllegalStateException("Cannot heartbeat a DOWN device: " + id);
           }

           device.resetTimer();

           log.info("[HEARTBEAT] Device {} reset. Expired at {}" , id, device.getExpiresAt());
           return device;
       }

        //pause timer logic
       public MonitoredDevice pause(String id) {
           MonitoredDevice device = getDeviceOrThrow(id);
           if (device.getStatus() == MonitoredDevice.Status.DOWN) {
               throw new IllegalStateException("Cannot pause a DOWN device: " + id);
           }
           if (device.getStatus() == MonitoredDevice.Status.PAUSED) {
               throw new IllegalStateException("Device is already paused: " + id);
           }

            device.pause();

           log.info("[PAUSED] Device {} paused", id);
           return device;
       }

       //deleting a device
        public void delete(String id) {
            getDeviceOrThrow(id);
            monitoredDevices.remove(id);
            log.info("[DELETED] device {} removed", id);
        }

        // checking for "dead" devices (didnt send a ping before timeout)
        public void checkExpiredDevices() {
           if (monitoredDevices.isEmpty()) return; // do not check if there are no devices being monitored
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

        //returns all devices
        public Collection<MonitoredDevice> getAllMonitoredDevices() {
            return monitoredDevices.values();
        }
       
        private MonitoredDevice getDeviceOrThrow(String id) {
        MonitoredDevice device = monitoredDevices.get(id);

        if (device == null) {
            throw new IllegalArgumentException("Device not found: " + id);
        }

        return device;
    }

}

