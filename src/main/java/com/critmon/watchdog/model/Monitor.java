package com.critmon.watchdog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public class Monitor {
    public enum Status {ACTIVE, PAUSED, DOWN, UNDER_RECOVERY} //states

    private String id;
    private int timeout;
    private String alertEmail; //where alerts would be sent
    private Status status; //tracks device state
    private Instant expiresAt; //time at which device would be considered down if no heartbeat is received

    public Monitor(String id, int timeout, String alertEmail) {
        this.id = id;
        this.timeout = timeout;
        this.alertEmail = alertEmail;
        this.status = Status.ACTIVE;
        this.expiresAt = Instant.now().plusSeconds(timeout);
    }

    public void pause() {
        this.status = Status.PAUSED;
    }

    public void resetTimer() {
        this.expiresAt = Instant.now().plusSeconds(timeout);
        this.status = Status.ACTIVE;
    }




    @JsonIgnore
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

    public int getTimeout() {
        return timeout;
    }

    public String getAlertEmail() {
        return alertEmail;
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
