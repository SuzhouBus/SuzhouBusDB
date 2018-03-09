package com.fangjue.suzhoubusdb;

import java.util.ArrayList;

class RealtimeLine {
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_DISCARDED = 1;
    private String guid;
    private String name;
    private String direction;
    private int status;
    private ArrayList<RealtimeRelatedLine> relatedLines;

    RealtimeLine(String guid, String name, String direction, int status) {
        this(guid, name, direction, status, null);
    }

    RealtimeLine(String guid, String name, String direction, int status, ArrayList<RealtimeRelatedLine> relatedLines) {
        this.guid = guid;
        this.name = name;
        this.direction = direction;
        this.status = status;
        this.relatedLines = relatedLines;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

class RealtimeRelatedLine {
    private final String name;
    private final String direction;
    private final String guid;
    private final String relation;

    RealtimeRelatedLine(String name, String direction, String guid, String relation) {
        this.name = name;
        this.direction = direction;
        this.guid = guid;
        this.relation = relation;
    }
}