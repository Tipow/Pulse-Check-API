package com.critmon.watchdog.controller;


import com.critmon.watchdog.model.Monitor;
import com.critmon.watchdog.service.MonitorService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/monitors")
public class MonitorController {
    private final MonitorService monitorService;

    // constructor Injection
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    //register a new device (POST /monitors)
    @PostMapping
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {

        String id = (String) body.get("id");
        int timeoutSeconds = (Integer) body.get("timeoutSeconds");
        String alertMail = (String) body.get("alertMail");

        if (id == null || timeoutSeconds <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "id and timeout are required"));
        }
        try {
            Monitor monitor = monitorService.register(id, timeoutSeconds, alertMail);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Monitor created successfully",
                            "id", monitor.getId(),
                            "expires_at", monitor.getExpiresAt()
                    )
            );
        } catch (IllegalArgumentException e) {
            // duplicate id — 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // heartbeat POST /monitors/{id}/heartbeat
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String id) {

        try {

            Monitor monitor = monitorService.heartbeat(id);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Heartbeat received. Timer resets.",
                            "expiresAt", monitor.getExpiresAt(),
                            "remainingSeconds", monitor.getRemainingSeconds()
                    )
            );

        } catch (IllegalArgumentException e) {
            //monitor not found(or does not exists)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            //monitor is down
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", e.getMessage())
            );
        }
    }


    //pause monitor (POST /monitors/{id}/pause)
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id) {

        try {
            Monitor monitor = monitorService.pause(id);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Monitor paused",
                            "status", monitor.getStatus()
                    )
            );

        } catch (IllegalArgumentException e) {
            //monitor  cannot be found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            //monitor is down or paused already
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", e.getMessage())
            );
        }
    }


    //get a device (GET /monitors/{id})
    @GetMapping("/{id}")
    public ResponseEntity<?> getMonitor(@PathVariable String id) {

        return monitorService.findById(id).<ResponseEntity<?>>map(monitor ->
                        ResponseEntity.ok(
                                Map.of(
                                        "id", monitor.getId(),
                                        "status", monitor.getStatus(),
                                        "expiresAt", monitor.getExpiresAt(),
                                        "remainingSeconds", monitor.getRemainingSeconds()
                                )
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                Map.of("error", "Device not found")
                        )
                );
    }

    //get all monitors (GET /monitors)
    @GetMapping
    public ResponseEntity<Collection<Monitor>> getAll() {
        return ResponseEntity.ok(monitorService.getAllmonitors());
    }


    // delete a monitor (DELETE /monitors/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {

        try {
            monitorService.delete(id);
            return ResponseEntity.ok(
                    Map.of("message", "Monitor deleted successfully", "id", id)
            );
        } catch (IllegalArgumentException e) {
            // monitor not found or does not exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        }
    }
}
