package com.critmon.watchdog.model;

import java.time.Instant;

public class MonitoredDevice {
    public enum Status {ACTIVE, PAUSED, DOWN} //states

    private String id;
    private int timeoutSeconds;
    private String alertMail; //where alerts would be sent
    private Status status; //tracks device state
    private Instant expiresAt; //time at which device would be considered down if no heartbeat is received

    public MonitoredDevice(String id, int timeoutSeconds, String alertMail) {
        this.id = id;
        this.timeoutSeconds = timeoutSeconds;
        this.alertMail = alertMail;
        this.status = Status.ACTIVE;
        this.expiresAt = Instant.now().plusSeconds(timeoutSeconds);
    }

    public void pause() {
        this.status = Status.PAUSED;
    }

    public void resetTimer() {
        this.expiresAt = Instant.now().plusSeconds(timeoutSeconds);
        this.status = Status.ACTIVE;
    }

    public boolean isExpired() {
        return status == Status.ACTIVE && Instant.now().isAfter(expiresAt);
    }// if expired, send alert

    public long getRemainingSeconds() {
        if(status != Status.ACTIVE) return -1;  //(no valid remaining time)
        return Instant.now().until(expiresAt, java.time.temporal.ChronoUnit.SECONDS);
    }

    public String getId() {
        return id;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getAlertMail() {
        return alertMail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
