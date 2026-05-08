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
       public Monitor register(String id, int timeout, String alertEmail) {
              if (monitors.containsKey(id)) {
                  throw new IllegalArgumentException("Monitor already exists");
              }
              if(timeout <= 0){
                  throw new IllegalArgumentException("Timeout must be greater than 0");
              }

              Monitor monitor = new Monitor(id, timeout, alertEmail);
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

        //device attached to a dead monitor under recovery
        public Monitor startRecovery(String id) {
            Monitor monitor = getMonitorOrThrow(id);

            if (monitor.getStatus() != Monitor.Status.DOWN) {
                throw new IllegalStateException("Only DOWN monitors can enter recovery");
            }

            monitor.setStatus(Monitor.Status.UNDER_RECOVERY);

            log.info(
                    "{{\"RECOVERY_START\": \"Monitor {} is under technician repair\", \"time\": \"{}\"}}",
                    id, Instant.now()
            );

            return monitor;
        }

        //recovery complete
        public Monitor completeRecovery(String id) {
            Monitor monitor = getMonitorOrThrow(id);

            if (monitor.getStatus() != Monitor.Status.UNDER_RECOVERY) {
                throw new IllegalStateException("Only UNDER_RECOVERY monitors can complete recovery");
            }

            monitor.resetTimer(); // sets status back to ACTIVE and resets expiresAt

            log.info(
                    "{{\"RECOVERY_COMPLETE\": \"Monitor {} is back online\", \"time\": \"{}\"}}",
                    id, Instant.now()
            );

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
                            "{{\"ALERT\": \"Device {} is down!\", \"time\": \"{}\"}}",
                            monitor.getId(), now);
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

