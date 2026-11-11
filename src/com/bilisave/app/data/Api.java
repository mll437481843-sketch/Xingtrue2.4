package com.bilisave.app.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bilibili web API + DASH stream downloader. All requests carry a fixed UA / Referer. */
public final class Api {
    public static final String UA_DEFAULT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String REFERER = "https://www.bilibili.com/";
    private static final Pattern BV = Pattern.compile("BV[0-9A-Za-z]{10}");
    private static final Pattern AV = Pattern.compile("(?i)av(\\d{1,12})");
    private static final int[] MIXIN_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };
    private static final String B58 = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf";

    private static String cookie = "";
    private static String ua = UA_DEFAULT;
    private static String mixinKey = null;
    private static long mixinAt = 0L;

    private Api() {
    }

    public static void refresh(Context c) {
        cookie = Prefs.cookie(c);
        String over = Prefs.ua(c);
        ua = (over == null || over.trim().isEmpty()) ? UA_DEFAULT : over.trim();
    }

    public static class ApiException extends IOException {
        public ApiException(String m) {
            super(m);
        }
    }

    public interface Progress {
        void onProgress(long done, long total);

        boolean cancelled();
    }

    public static class PlayResult {
        public final List<Models.Dash> videos = new ArrayList<>();
        public final List<Models.Dash> audios = new ArrayList<>();
        public final List<Models.Format> formats = new ArrayList<>();
        public String durlUrl = "";
        public long durlSize = 0;
        public int durlQn = 0;
    }

    // ---------------------------------------------------------------- http

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", ua);
        c.setRequestProperty("Referer", REFERER);
        c.setRequestProperty("Accept", "*/*");
        c.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
        if (!cookie.isEmpty()) c.setRequestProperty("Cookie", cookie);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        return c;
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    public static JSONObject json(String url) throws IOException {
        HttpURLConnection c = open(url);
        try {
            int code = c.getResponseCode();
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            String body = is == null ? "" : readAll(is);
            if (body.isEmpty()) throw new ApiException("服务器无响应（HTTP " + code + "）");
            try {
                return new JSONObject(body);
            } catch (Exception e) {
                throw new ApiException("返回格式异常（HTTP " + code + "）");
            }
        } finally {
            c.disconnect();
        }
    }

    public static byte[] bytes(String url) {
        HttpURLConnection c = null;
        try {
            c = open(url);
            c.setConnectTimeout(12000);
            c.setReadTimeout(20000);
            if (c.getResponseCode() >= 400) return null;
            InputStream is = c.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------- resolve

    public static String resolveBvid(String raw) throws IOException {
        if (raw == null || raw.trim().isEmpty()) throw new ApiException("请先粘贴 BV 号或 b23.tv 链接");
        String in = raw.trim();
        Matcher m = BV.matcher(in);
        if (m.find()) return m.group();
        Matcher a = AV.matcher(in);
        if (a.find()) {
            try {
                return av2bv(Long.parseLong(a.group(1)));
            } catch (Exception ignored) {
            }
        }
        String url = null;
        if (in.startsWith("http://") || in.startsWith("https://")) url = in;
        else if (in.contains("b23.tv") || in.contains("bilibili.com") || in.contains("bili")) url = "https://" + in;
        if (url != null) {
            String fin = follow(url);
            Matcher b = BV.matcher(fin);
            if (b.find()) return b.group();
        }
        throw new ApiException("未找到 BV 号，请粘贴 BV 号 / av 号 / b23.tv 短链");
    }

    /** Follow redirects manually (b23 short links) and scan the final URL / body for a BV id. */
    private static String follow(String start) {
        String cur = start;
        for (int i = 0; i < 6; i++) {
            HttpURLConnection c = null;
            try {
                c = open(cur);
                c.setInstanceFollowRedirects(false);
                c.setRequestMethod("GET");
                int code = c.getResponseCode();
                String loc = c.getHeaderField("Location");
                if (code >= 300 && code < 400 && loc != null) {
                    cur = new URL(new URL(cur), loc).toString();
                    continue;
                }
                String body = "";
                if (code < 400) {
                    InputStream is = c.getInputStream();
                    if (is != null) body = readAll(is);
                }
                Matcher b = BV.matcher(body);
                if (b.find()) return b.group();
                return cur;
            } catch (Exception e) {
                return cur;
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return cur;
    }

    public static String av2bv(long avid) {
        char[] bvid = "BV1000000000".toCharArray();
        long bv = (1L << 51) | avid;
        for (int i = bvid.length - 1; i >= 0; i--) {
            bvid[i] = B58.charAt((int) (bv % 58));
            bv /= 58;
            if (bv == 0) break;
        }
        char t = bvid[3];
        bvid[3] = bvid[9];
        bvid[9] = t;
        t = bvid[4];
        bvid[4] = bvid[7];
        bvid[7] = t;
        return new String(bvid);
    }

    public static long bv2av(String bvid) {
        if (bvid == null || bvid.length() < 10) return 0;
        char[] bv = bvid.toCharArray();
        char t = bv[3];
        bv[3] = bv[9];
        bv[9] = t;
        t = bv[4];
        bv[4] = bv[7];
        bv[7] = t;
        long r = 0;
        for (int i = 3; i < bv.length; i++) {
            int idx = B58.indexOf(bv[i]);
            if (idx < 0) return 0;
            r = r * 58 + idx;
        }
        return r & ((1L << 51) - 1);
    }

    // ---------------------------------------------------------------- view

    public static Models.Video view(String bvid) throws IOException {
        JSONObject o = json("https://api.bilibili.com/x/web-interface/view?bvid=" + enc(bvid));
        if (o.optInt("code") != 0)
            throw new ApiException("解析失败：" + o.optString("message") + "（" + o.optInt("code") + "）");
        JSONObject d = o.optJSONObject("data");
        if (d == null) throw new ApiException("解析失败：无数据");
        Models.Video v = new Models.Video();
        v.aid = d.optLong("aid");
        v.bvid = d.optString("bvid", bvid);
        v.title = d.optString("title");
        v.pic = d.optString("pic");
        v.duration = d.optInt("duration");
        Models.Part single = new Models.Part();
        single.cid = d.optLong("cid");
        single.page = 1;
        single.part = v.title;
        single.duration = v.duration;
        JSONArray pages = d.optJSONArray("pages");
        if (pages != null && pages.length() > 0) {
            for (int i = 0; i < pages.length(); i++) {
                JSONObject p = pages.optJSONObject(i);
                if (p == null) continue;
                Models.Part pt = new Models.Part();
                pt.cid = p.optLong("cid");
                pt.page = p.optInt("page", i + 1);
                pt.part = p.optString("part", "P" + (i + 1));
                pt.duration = p.optInt("duration");
                v.parts.add(pt);
            }
        } else if (single.cid != 0) {
            v.parts.add(single);
        }
        if (v.parts.isEmpty()) throw new ApiException("解析失败：没有可下载的分 P");
        return v;
    }

    // ----------------------------------------------------------------- nav

    public static JSONObject navData() throws IOException {
        JSONObject o = json("https://api.bilibili.com/x/web-interface/nav");
        JSONObject d = o.optJSONObject("data");
        if (d == null) throw new ApiException("登录态校验请求失败");
        return d;
    }

    public static class User {
        public String name = "";
        public String face = "";
        public long mid;
    }

    public static User verifyCookie(String testCookie) throws IOException {
        String back = cookie;
        cookie = testCookie == null ? "" : testCookie.trim();
        try {
            JSONObject d = navData();
            if (!d.optBoolean("isLogin")) throw new ApiException("Cookie 失效或未登录，请重新复制 SESSDATA");
            User u = new User();
            u.name = d.optString("uname");
            u.face = d.optString("face");
            u.mid = d.optLong("mid");
            return u;
        } finally {
            cookie = back;
        }
    }

    // ------------------------------------------------------------ qr login

    public static class Qr {
        public String url = "";
        public String key = "";
    }

    /** state: 0 waiting, 1 scanned-unconfirmed, 2 ok, 3 expired */
    public static class QrState {
        public int state;
        public String message = "";
        public String cookie = "";
    }

    public static Qr qrGenerate() throws IOException {
        JSONObject o = json("https://passport.bilibili.com/x/passport-login/web/qrcode/generate");
        JSONObject d = o.optJSONObject("data");
        if (o.optInt("code") != 0 || d == null)
            throw new ApiException("二维码生成失败：" + o.optString("message"));
        Qr q = new Qr();
        q.url = d.optString("url");
        q.key = d.optString("qrcode_key");
        if (q.url.isEmpty() || q.key.isEmpty()) throw new ApiException("二维码生成失败：返回为空");
        return q;
    }

    public static QrState qrPoll(String key) throws IOException {
        HttpURLConnection c = open("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + enc(key));
        try {
            int code = c.getResponseCode();
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            String body = is == null ? "" : readAll(is);
            JSONObject o;
            try {
                o = new JSONObject(body);
            } catch (Exception e) {
                throw new ApiException("扫码状态解析失败（HTTP " + code + "）");
            }
            QrState s = new QrState();
            JSONObject d = o.optJSONObject("data");
            int dc = d == null ? -1 : d.optInt("code", -1);
            s.message = d == null ? o.optString("message") : d.optString("message");
            if (dc == 0) {
                s.state = 2;
                s.cookie = cookieFromResponse(c);
            } else if (dc == 86090) {
                s.state = 1;
            } else if (dc == 86038) {
                s.state = 3;
            }
            return s;
        } finally {
            c.disconnect();
        }
    }

    /** Pull login cookies out of the poll response headers. */
    private static String cookieFromResponse(HttpURLConnection c) {
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, List<String>> e : c.getHeaderFields().entrySet()) {
            if (e.getKey() == null || !e.getKey().equalsIgnoreCase("Set-Cookie")) continue;
            for (String v : e.getValue()) raw.append(v).append("; ");
        }
        String[] names = {"SESSDATA", "bili_jct", "DedeUserID__ckMd5", "DedeUserID", "sid"};
        StringBuilder out = new StringBuilder();
        for (String n : names) {
            Matcher mm = Pattern.compile("(?:^|[;,] *)" + n + "=([^;, ]+)").matcher(raw);
            if (mm.find()) {
                if (out.length() > 0) out.append("; ");
                out.append(n).append('=').append(mm.group(1));
            }
        }
        return out.toString();
    }

    private static String mixinKey() throws IOException {
        long now = System.currentTimeMillis();
        if (mixinKey != null && now - mixinAt < 3600_000L) return mixinKey;
        JSONObject d = navData();
        JSONObject img = d.optJSONObject("wbi_img");
        if (img == null) throw new ApiException("无法获取 WBI 签名密钥");
        String i = img.optString("img_url");
        String s = img.optString("sub_url");
        i = i.substring(i.lastIndexOf('/') + 1);
        s = s.substring(s.lastIndexOf('/') + 1);
        int dot = i.lastIndexOf('.');
        if (dot > 0) i = i.substring(0, dot);
        dot = s.lastIndexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        String orig = i + s;
        if (orig.length() < 64) throw new ApiException("WBI 密钥格式异常");
        StringBuilder sb = new StringBuilder();
        for (int idx : MIXIN_TAB) sb.append(orig.charAt(idx));
        mixinKey = sb.substring(0, 32);
        mixinAt = now;
        return mixinKey;
    }

    private static String sign(Map<String, String> params) throws IOException {
        String mixin = mixinKey();
        params.put("wts", String.valueOf(System.currentTimeMillis() / 1000L));
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(k)).append('=').append(enc(filter(params.get(k))));
        }
        String q = sb.toString();
        return q + "&w_rid=" + md5(q + mixin);
    }

    private static String filter(String v) {
        if (v == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : v.toCharArray()) if ("!'()*".indexOf(ch) < 0) sb.append(ch);
        return sb.toString();
    }

    // -------------------------------------------------------------- playurl

    public static PlayResult play(String bvid, long cid, int qn) throws IOException {
        PlayResult r = new PlayResult();
        Map<String, String> p = new LinkedHashMap<>();
        p.put("bvid", bvid);
        p.put("cid", String.valueOf(cid));
        p.put("qn", String.valueOf(qn));
        p.put("fnval", "16");
        p.put("fourk", "1");
        p.put("platform", "pc");
        JSONObject o = json("https://api.bilibili.com/x/player/wbi/playurl?" + sign(p));
        int code = o.optInt("code");
        if (code != 0)
            throw new ApiException("取流失败：" + o.optString("message") + "（" + code + "）");
        JSONObject data = o.optJSONObject("data");
        if (data == null) throw new ApiException("取流失败：无数据");
        parseFormats(data, r);
        JSONObject dash = data.optJSONObject("dash");
        if (dash != null) {
            JSONArray vids = dash.optJSONArray("video");
            if (vids != null) {
                for (int i = 0; i < vids.length(); i++) {
                    Models.Dash d = dash(vids.optJSONObject(i));
                    if (d != null) r.videos.add(d);
                }
            }
            JSONArray auds = dash.optJSONArray("audio");
            if (auds != null) {
                for (int i = 0; i < auds.length(); i++) {
                    Models.Dash d = dash(auds.optJSONObject(i));
                    if (d != null) r.audios.add(d);
                }
            }
        }
        if (r.videos.isEmpty() || r.audios.isEmpty()) {
            // fallback: progressive single-file (mp4 with audio muxed)
            Map<String, String> p2 = new LinkedHashMap<>();
            p2.put("bvid", bvid);
            p2.put("cid", String.valueOf(cid));
            p2.put("qn", String.valueOf(qn));
            p2.put("fnval", "1");
            p2.put("platform", "pc");
            JSONObject o2 = json("https://api.bilibili.com/x/player/wbi/playurl?" + sign(p2));
            JSONObject d2 = o2.optJSONObject("data");
            if (d2 != null) {
                JSONArray durl = d2.optJSONArray("durl");
                if (durl != null && durl.length() > 0) {
                    JSONObject first = durl.optJSONObject(0);
                    r.durlUrl = first.optString("url");
                    r.durlSize = first.optLong("size");
                    r.durlQn = d2.optInt("quality");
                }
                if (r.formats.isEmpty()) parseFormats(d2, r);
            }
        }
        if (r.videos.isEmpty() && r.durlUrl.isEmpty())
            throw new ApiException("没有拿到可用流，可能被风控或需要大会员");
        return r;
    }

    private static void parseFormats(JSONObject data, PlayResult r) {
        JSONArray sf = data.optJSONArray("support_formats");
        if (sf == null) return;
        for (int i = 0; i < sf.length(); i++) {
            JSONObject f = sf.optJSONObject(i);
            if (f == null) continue;
            int q = f.optInt("quality");
            String label = f.optString("new_description", f.optString("display_desc", q + "P"));
            label = label.replace(" 高清", "").replace(" 准高清", "").replace(" 标清", "").replace(" 流畅", "");
            r.formats.add(new Models.Format(q, label));
        }
    }

    private static Models.Dash dash(JSONObject o) {
        if (o == null) return null;
        Models.Dash d = new Models.Dash();
        d.id = o.optInt("id");
        d.codecid = o.optInt("codecid");
        d.size = o.optLong("size");
        d.url = o.optString("baseUrl", o.optString("base_url"));
        d.backup = o.optString("backupUrl", o.optString("backup_url"));
        if (d.backup.startsWith("[")) {
            try {
                JSONArray a = new JSONArray(d.backup);
                d.backup = a.length() > 0 ? a.optString(0) : "";
            } catch (Exception e) {
                d.backup = "";
            }
        }
        if (d.url.isEmpty()) return null;
        return d;
    }

    /** Highest quality not above the requested one; prefers AVC (codecid 7) for clean remuxing. */
    public static Models.Dash pickVideo(PlayResult r, int wantQn) {
        Models.Dash best = null;
        for (Models.Dash d : r.videos) {
            if (d.id > wantQn) continue;
            if (best == null || d.id > best.id) best = d;
            else if (d.id == best.id && best.codecid != 7 && d.codecid == 7) best = d;
        }
        if (best == null && !r.videos.isEmpty()) best = r.videos.get(0);
        return best;
    }

    public static Models.Dash pickAudio(PlayResult r) {
        Models.Dash best = null;
        for (Models.Dash d : r.audios) if (best == null || d.id > best.id) best = d;
        return best;
    }

    /** Progressive single-file fallback (mp4 with muxed audio) used when DASH remuxing fails. */
    public static PlayResult progressive(String bvid, long cid, int qn) throws IOException {
        PlayResult r = new PlayResult();
        Map<String, String> p = new LinkedHashMap<>();
        p.put("bvid", bvid);
        p.put("cid", String.valueOf(cid));
        p.put("qn", String.valueOf(qn));
        p.put("fnval", "1");
        p.put("platform", "pc");
        JSONObject o = json("https://api.bilibili.com/x/player/wbi/playurl?" + sign(p));
        JSONObject d = o.optJSONObject("data");
        if (d != null) {
            JSONArray durl = d.optJSONArray("durl");
            if (durl != null && durl.length() > 0) {
                JSONObject first = durl.optJSONObject(0);
                r.durlUrl = first.optString("url");
                r.durlSize = first.optLong("size");
                r.durlQn = d.optInt("quality");
            }
            parseFormats(d, r);
        }
        return r;
    }

    // -------------------------------------------------------------- download

    public static void download(String url, String backup, File out, Progress p) throws IOException {
        IOException last = null;
        String[] urls = {url, backup};
        for (String u : urls) {
            if (u == null || u.isEmpty()) continue;
            try {
                streamTo(u, out, p);
                return;
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new ApiException("下载地址为空");
    }

    private static void streamTo(String url, File out, Progress p) throws IOException {
        long start = out.exists() ? out.length() : 0L;
        HttpURLConnection c = open(url);
        if (start > 0) c.setRequestProperty("Range", "bytes=" + start + "-");
        int code = c.getResponseCode();
        if (code == 416) {
            c.disconnect();
            out.delete();
            start = 0;
            c = open(url);
            code = c.getResponseCode();
        }
        if (code != 200 && code != 206) {
            c.disconnect();
            throw new ApiException("下载被拒绝（HTTP " + code + "），检查 Cookie / UA / Referer");
        }
        long total = (code == 206 ? start : 0L) + c.getContentLengthLong();
        InputStream is = c.getInputStream();
        FileOutputStream fos = new FileOutputStream(out, code == 206);
        long done = start;
        try {
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) > 0) {
                if (p != null && p.cancelled()) throw new ApiException("已取消");
                fos.write(buf, 0, n);
                done += n;
                if (p != null) p.onProgress(done, total);
            }
            fos.flush();
        } finally {
            try {
                fos.close();
            } catch (Exception ignored) {
            }
            try {
                is.close();
            } catch (Exception ignored) {
            }
            c.disconnect();
        }
    }
}

