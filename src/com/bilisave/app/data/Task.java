package com.bilisave.app.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Task {
    public static final String PENDING = "等待中", RUNNING = "下载中", MERGING = "合封中",
            DONE = "已完成", FAIL = "失败", INTERRUPTED = "已中断";

    public String id = UUID.randomUUID().toString();
    public String bvid = "";
    public long cid;
    public String title = "";
    public String part = "";
    public int qn;
    public String qlabel = "";
    public boolean audioOnly;
    public String state = PENDING;
    public long bytes;
    public long total;
    public String path = "";
    public String error = "";
    public long created = System.currentTimeMillis();

    public int pct() {
        if (DONE.equals(state)) return 100;
        if (total <= 0) return 0;
        return (int) Math.min(99, bytes * 100 / total);
    }

    public String sizeText() {
        if (total <= 0) return "";
        return mb(bytes) + " / " + mb(total);
    }

    private static String mb(long v) {
        if (v < 1024 * 1024) return Math.max(1, v / 1024) + "KB";
        return String.format("%.1fMB", v / 1048576.0);
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("bvid", bvid);
            o.put("cid", cid);
            o.put("title", title);
            o.put("part", part);
            o.put("qn", qn);
            o.put("qlabel", qlabel);
            o.put("audioOnly", audioOnly);
            o.put("state", state);
            o.put("bytes", bytes);
            o.put("total", total);
            o.put("path", path);
            o.put("error", error);
            o.put("created", created);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static Task fromJson(JSONObject o) {
        Task t = new Task();
        t.id = o.optString("id", t.id);
        t.bvid = o.optString("bvid");
        t.cid = o.optLong("cid");
        t.title = o.optString("title");
        t.part = o.optString("part");
        t.qn = o.optInt("qn");
        t.qlabel = o.optString("qlabel");
        t.audioOnly = o.optBoolean("audioOnly");
        t.state = o.optString("state", PENDING);
        t.bytes = o.optLong("bytes");
        t.total = o.optLong("total");
        t.path = o.optString("path");
        t.error = o.optString("error");
        t.created = o.optLong("created");
        return t;
    }

    public static String listToJson(List<Task> tasks) {
        JSONArray a = new JSONArray();
        for (Task t : tasks) a.put(t.toJson());
        return a.toString();
    }

    public static List<Task> listFromJson(String s) {
        List<Task> out = new ArrayList<>();
        if (s == null || s.isEmpty()) return out;
        try {
            JSONArray a = new JSONArray(s);
            for (int i = 0; i < a.length(); i++) out.add(fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }
}

