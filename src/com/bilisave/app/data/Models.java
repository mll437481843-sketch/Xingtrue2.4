package com.bilisave.app.data;

import java.util.ArrayList;
import java.util.List;

public final class Models {
    public static class Part {
        public long cid;
        public int page;
        public String part = "";
        public int duration;
    }

    public static class Video {
        public long aid;
        public String bvid = "";
        public String title = "";
        public String pic = "";
        public int duration;
        public final List<Part> parts = new ArrayList<>();

        public String durText() {
            int s = Math.max(0, duration);
            int h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
            if (h > 0) return String.format("%d:%02d:%02d", h, m, sec);
            return String.format("%d:%02d", m, sec);
        }
    }

    public static class Format {
        public int qn;
        public String label;

        public Format(int qn, String label) {
            this.qn = qn;
            this.label = label;
        }
    }

    public static class Dash {
        public int id;
        public int codecid;
        public long size;
        public String url = "";
        public String backup = "";
    }
}

