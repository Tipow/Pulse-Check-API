package com.critmon.watchdog.service;

import com.critmon.watchdog.model.Monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitorService {
       private static final Logger log = LoggerFactory.getLogger(MonitorService.class); //creates logger
       private final Map<String, Monitor> monitors = new ConcurrentHashMap<>();// in memory db

       //registers a monitor
       public Monitor register(String id, int timeoutSeconds, String alertMail) {
              if (monitors.containsKey(id)) {
                  throw new IllegalArgumentException("Monitor already exists");
              }
              if(timeoutSeconds <= 0){
                  throw new IllegalArgumentException("Timeout must be greater than 0");
              }

              Monitor monitor = new Monitor(id, timeoutSeconds, alertMail);
              monitors.put(id, monitor);
              log.info("[REGISTERED] Device {} expires at {}" , id, monitor.getExpiresAt());
              return monitor;
       }

        //find a monitor logic
       public Optional<Monitor> findById(String id){
           return Optional.ofNullable(monitors.get(id));
       }

        //heartbeat logic
       public Monitor heartbeat(String id) {
           Monitor monitor = getMonitorOrThrow(id);

           if (monitor.getStatus() == Monitor.Status.DOWN) {
               throw new IllegalStateException("Cannot heartbeat a DOWN monitor: " + id);
           }

           monitor.resetTimer();

           log.info("[HEARTBEAT] Monitor {} reset. Expired at {}" , id, monitor.getExpiresAt());
           return monitor;
       }

        //pause timer logic
       public Monitor pause(String id) {
           Monitor monitor = getMonitorOrThrow(id);
           if (monitor.getStatus() == Monitor.Status.DOWN) {
               throw new IllegalStateException("Cannot pause a DOWN monitor: " + id);
           }
           if (monitor.getStatus() == Monitor.Status.PAUSED) {
               throw new IllegalStateException("Monitor is already paused: " + id);
           }

            monitor.pause();

           log.info("[PAUSED] Monitor {} paused", id);
           return monitor;
       }

       //deleting a monitor
        public void delete(String id) {
            getMonitorOrThrow(id);
            monitors.remove(id);
            log.info("[DELETED] monitor {} removed", id);
        }

        // checking for "dead" monitors (didnt send a ping before timeout)
        public void checkExpiredMonitors() {
           if (monitors.isEmpty()) return; // do not check if there are no monitors
        Instant now = Instant.now();

            for (Monitor monitor : monitors.values()) {

                if (monitor.getStatus() == Monitor.Status.ACTIVE
                    && now.isAfter(monitor.getExpiresAt())) {

                monitor.setStatus(Monitor.Status.DOWN);

                log.error(
                        "[ALERT] Device {} is DOWN | time={} | email={}", monitor.getId(), now, monitor.getAlertMail());
                }
            }
        }

        //returns all monitors
        public Collection<Monitor> getAllmonitors() {
            return monitors.values();
        }

        private Monitor getMonitorOrThrow(String id) {
        Monitor monitor = monitors.get(id);

        if (monitor == null) {
            throw new IllegalArgumentException("Monitor not found: " + id);
        }

        return monitor;
    }

}

