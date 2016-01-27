package com.fangjue.suzhoubusdb;

import android.text.TextUtils;

public class Bus {
    private String busId;
    private String licenseId;
    private String lineId;
    private int revision = 0;
    private String[] comments = new String[]{};
    private final int TOSTRING_MAX_COMMENTS_LENGTH = 16;
    
    public Bus(String busId, String licenseId, String lineId, String comments) {
        this.busId = busId;
        this.licenseId = licenseId;
        this.lineId = lineId;
        if (comments != null)
            this.comments = comments.trim().split("\t| {8}");
    }
    
    @Override
    public String toString() {
        String busId = this.busId == null ? "" : this.busId;
        String licenseId = this.licenseId == null ? "" : this.licenseId;
        String lineId = this.lineId == null ? "" : this.lineId;
        String comments = TextUtils.join(" ", this.comments);
        
        if (comments.length() > TOSTRING_MAX_COMMENTS_LENGTH)
            comments = comments.substring(0, TOSTRING_MAX_COMMENTS_LENGTH - 3) + "...";
        return busId + " " + licenseId + " " + lineId +  "   " + comments;
    }

    public String getBusId() {
        return busId;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public String getLineId() {
        return lineId;
    }

    public String[] getComments() {
        return comments;
    }

    public boolean isComplete() {
        return this.getBusId().length() != 0 && this.getLicenseId().length() != 0 &&
                this.getLineId().length() != 0;
    }
}
