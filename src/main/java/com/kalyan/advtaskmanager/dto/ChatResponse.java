package com.kalyan.advtaskmanager.dto;

public class ChatResponse {
    private String reply;
    private boolean refreshData;
    private boolean error;

    // ── Static factories ──────────────────────────────────────────────────────
    public static ChatResponse success(String reply, boolean refreshData) {
        ChatResponse r = new ChatResponse();
        r.reply = reply;
        r.refreshData = refreshData;
        r.error = false;
        return r;
    }

    public static ChatResponse error(String message) {
        ChatResponse r = new ChatResponse();
        r.reply = message;
        r.refreshData = false;
        r.error = true;
        return r;
    }

    public String getReply()       { return reply; }
    public boolean isRefreshData() { return refreshData; }
    public boolean isError()       { return error; }
}

