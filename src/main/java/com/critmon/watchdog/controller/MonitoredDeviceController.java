package com.critmon.watchdog.controller;


import com.critmon.watchdog.model.MonitoredDevice;
import com.critmon.watchdog.service.MonitoredDeviceService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/monitoredDevices")
public class MonitoredDeviceController {
    private final MonitoredDeviceService monitoredDeviceService;

    // constructor Injection
    public MonitoredDeviceController(MonitoredDeviceService monitoredDeviceService) {
        this.monitoredDeviceService = monitoredDeviceService;
    }

    //register a new device // POST /monitoredDevices
    @PostMapping
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {

        String id = (String) body.get("id");
        int timeoutSeconds = (Integer) body.get("timeoutSeconds");
        String alertMail = (String) body.get("alertMail");

        if (id == null || timeoutSeconds <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "id and timeout are required"));
        }
        try {
            MonitoredDevice device = monitoredDeviceService.register(id, timeoutSeconds, alertMail);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Monitor created successfully",
                            "id", device.getId(),
                            "expires_at", device.getExpiresAt()
                    )
            );
        } catch (IllegalArgumentException e) {
            // Duplicate ID — 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // heartbeat POST /monitoredDevices/{id}/heartbeat
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String id) {

        try {

            MonitoredDevice device = monitoredDeviceService.heartbeat(id);
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Heartbeat received. Timer resets.",
                            "expiresAt", device.getExpiresAt(),
                            "remainingSeconds", device.getRemainingSeconds()
                    )
            );

        } catch (IllegalArgumentException e) {
            //device not found(or does not exists)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            //device is down
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", e.getMessage())
            );
        }
    }


    //pause device (POST /monitoredDevices/{id}/pause)
    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id) {

        try {
            MonitoredDevice device = monitoredDeviceService.pause(id);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Device paused",
                            "status", device.getStatus()
                    )
            );

        } catch (IllegalArgumentException e) {
            //device  cannot be found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );

        } catch (IllegalStateException e) {
            //device is down or paused already
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("error", e.getMessage())
            );
        }
    }


    //get a device (GET /monitoredDevices/{id})
    @GetMapping("/{id}")
    public ResponseEntity<?> getDevice(@PathVariable String id) {

        return monitoredDeviceService.findById(id).<ResponseEntity<?>>map(device ->
                        ResponseEntity.ok(
                                Map.of(
                                        "id", device.getId(),
                                        "status", device.getStatus(),
                                        "expiresAt", device.getExpiresAt(),
                                        "remainingSeconds", device.getRemainingSeconds()
                                )
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                Map.of("error", "Device not found")
                        )
                );
    }

    //get all devices (GET /monitoredDevices)
    @GetMapping
    public ResponseEntity<Collection<MonitoredDevice>> getAll() {
        return ResponseEntity.ok(MonitoredDeviceService.getAllMonitoredDevices());
    }


    // delete a device (DELETE /monitoredDevices/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {

        try {
            monitoredDeviceService.delete(id);
            return ResponseEntity.ok(
                    Map.of("message", "Monitor deleted successfully", "id", id)
            );
        } catch (IllegalArgumentException e) {
            // Device not found or does not exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", e.getMessage())
            );
        }
    }
}
