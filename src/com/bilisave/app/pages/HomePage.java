package com.bilisave.app.pages;

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bilisave.app.AppState;
import com.bilisave.app.MainActivity;
import com.bilisave.app.data.Api;
import com.bilisave.app.data.Models;
import com.bilisave.app.data.Prefs;
import com.bilisave.app.data.Task;
import com.bilisave.app.download.DownloadService;
import com.bilisave.app.ui.ChipView;
import com.bilisave.app.ui.Monet;
import com.bilisave.app.ui.Toggle;
import com.bilisave.app.ui.Ui;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends ScrollView {
    private final MainActivity act;
    private final Monet m;
    private LinearLayout box;
    private EditText field;
    private ImageView cover;
    private String input = "";
    private boolean busy = false;

    public HomePage(MainActivity a) {
        super(a);
        act = a;
        m = a.theme();
        setFillViewport(true);
        setClipToPadding(false);
        setVerticalScrollBarEnabled(false);
        build();
    }

    private int dp(float v) {
        return Ui.dp(getContext(), v);
    }

    private void build() {
        removeAllViews();
        box = Ui.column(getContext());
        box.setPadding(dp(20), dp(6), dp(20), dp(28));
        addView(box, new FrameLayout.LayoutParams(-1, -2));

        Models.Video v = AppState.I.video;
        box.addView(inputCard());
        if (v != null) box.addView(videoCard(v), marginTop(14));
        else box.addView(emptyCard(), marginTop(14));
        box.addView(footNote(), marginTop(16));
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(top);
        return lp;
    }

    private View inputCard() {
        LinearLayout card = Ui.card(getContext(), m);
        card.addView(Ui.label(getContext(), m, "视频地址"));
        field = new EditText(getContext());
        field.setHint("BV 号 / av 号 / b23.tv 短链");
        field.setHintTextColor(Ui.alpha(m.onSurfaceVar, 150));
        field.setTextColor(m.onSurface);
        field.setTextSize(15f);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setBackground(Ui.round(Ui.alpha(m.surfaceVar, m.dark ? 150 : 190), dp(18), dp(1), Ui.alpha(m.outline, 50)));
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.setText(input);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
        flp.topMargin = dp(10);
        card.addView(field, flp);

        LinearLayout actions = Ui.row(getContext());
        actions.setPadding(0, dp(12), 0, 0);
        TextView paste = Ui.softButton(getContext(), m, "粘贴");
        paste.setOnClickListener(x -> pasteFromClipboard());
        TextView parse = Ui.primaryButton(getContext(), m, busy ? "解析中…" : "解析");
        parse.setOnClickListener(x -> parse());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-2, -2);
        plp.rightMargin = dp(10);
        actions.addView(paste, plp);
        actions.addView(parse, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(actions);
        return card;
    }

    private View emptyCard() {
        LinearLayout card = Ui.card(getContext(), m);
        TextView t = Ui.text(getContext(), "把视频地址粘进来，解析后挑清晰度下载。\n音画会自动合封成一个可播放的 mp4。", 13.5f,
                Ui.alpha(m.onSurfaceVar, 235), false);
        t.setLineSpacing(dp(4), 1f);
        card.addView(t);
        return card;
    }

    private View videoCard(Models.Video v) {
        LinearLayout card = Ui.card(getContext(), m);
        cover = new ImageView(getContext());
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(Ui.round(Ui.alpha(m.surfaceVar, 180), dp(20)));
        cover.setClipToOutline(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, dp(176));
        card.addView(cover, clp);

        TextView title = Ui.text(getContext(), v.title, 17f, m.onSurface, true);
        title.setMaxLines(3);
        title.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.topMargin = dp(14);
        card.addView(title, tlp);

        boolean logged = !Prefs.cookie(getContext()).isEmpty();
        String metaText = "时长 " + v.durText() + " · " + v.parts.size() + " 个分 P · " + (logged ? "已导入登录态" : "未登录");
        TextView meta = Ui.text(getContext(), metaText, 12.5f, Ui.alpha(m.onSurfaceVar, 235), false);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = dp(6);
        card.addView(meta, mlp);

        if (v.parts.size() > 1) {
            card.addView(sectionLabel("选择分 P"), topMarginSmall());
            HorizontalScrollView hs = new HorizontalScrollView(getContext());
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout row = Ui.row(getContext());
            for (int i = 0; i < v.parts.size(); i++) {
                final int idx = i;
                Models.Part p = v.parts.get(i);
                ChipView chip = new ChipView(getContext(), m, "P" + p.page + " " + p.part);
                chip.setActive(idx == AppState.I.partIndex);
                chip.setOnClickListener(x -> {
                    AppState.I.partIndex = idx;
                    build();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
                lp.rightMargin = dp(8);
                row.addView(chip, lp);
            }
            hs.addView(row);
            card.addView(hs, topMarginSmall());
        }

        card.addView(sectionLabel("清晰度（来自接口实际返回）"), topMarginSmall());
        HorizontalScrollView hq = new HorizontalScrollView(getContext());
        hq.setHorizontalScrollBarEnabled(false);
        LinearLayout qrow = Ui.row(getContext());
        List<Models.Format> fs = AppState.I.formats;
        if (fs.isEmpty()) {
            TextView none = Ui.text(getContext(), "未取到清晰度列表，默认按最高可用下载", 12.5f, Ui.alpha(m.onSurfaceVar, 220), false);
            qrow.addView(none);
        }
        for (Models.Format f : fs) {
            ChipView chip = new ChipView(getContext(), m, f.label);
            chip.setActive(f.qn == AppState.I.qn);
            chip.setOnClickListener(x -> {
                AppState.I.qn = f.qn;
                build();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            qrow.addView(chip, lp);
        }
        hq.addView(qrow);
        card.addView(hq, topMarginSmall());

        LinearLayout audioRow = Ui.row(getContext());
        audioRow.setPadding(0, dp(16), 0, 0);
        TextView al = Ui.text(getContext(), "只下载音频（m4a）", 14f, m.onSurface, false);
        audioRow.addView(al, new LinearLayout.LayoutParams(0, -2, 1f));
        Toggle tg = new Toggle(getContext(), m, AppState.I.audioOnly);
        tg.setOnChange(() -> AppState.I.audioOnly = tg.isChecked());
        audioRow.addView(tg, new LinearLayout.LayoutParams(dp(48), dp(28)));
        card.addView(audioRow);

        TextView dl = Ui.primaryButton(getContext(), m, downloadLabel());
        dl.setOnClickListener(x -> startDownload());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
        dlp.topMargin = dp(14);
        card.addView(dl, dlp);

        TextView hint = Ui.text(getContext(), "清晰度上限由登录态决定：未登录通常只有 480P。大会员才可能拿到 1080P 以上。", 11.5f,
                Ui.alpha(m.onSurfaceVar, 190), false);
        hint.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.topMargin = dp(10);
        card.addView(hint, hlp);

        loadCover(v.pic);
        return card;
    }

    private LinearLayout.LayoutParams topMarginSmall() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        return lp;
    }

    private View sectionLabel(String s) {
        TextView t = Ui.label(getContext(), m, s);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        return t;
    }

    private String qualityLabel(int qn) {
        for (Models.Format f : AppState.I.formats) if (f.qn == qn) return f.label;
        return qn > 0 ? qn + "P" : "最高可用";
    }

    private String downloadLabel() {
        return AppState.I.audioOnly ? "下载音频" : "下载 · " + qualityLabel(AppState.I.qn);
    }

    private View footNote() {
        TextView t = Ui.text(getContext(), "本工具仅供个人离线观看。请尊重 UP 主版权，不要二次分发，也不要批量抓取。登录态由你自己粘贴，只存在本机。",
                11.5f, Ui.alpha(m.onSurfaceVar, 165), false);
        t.setLineSpacing(dp(4), 1f);
        return t;
    }

    private void pasteFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || cm.getPrimaryClip() == null || cm.getPrimaryClip().getItemCount() == 0) {
            act.toast("剪贴板是空的");
            return;
        }
        CharSequence cs = cm.getPrimaryClip().getItemAt(0).coerceToText(getContext());
        input = cs == null ? "" : cs.toString().trim();
        if (field != null) field.setText(input);
    }

    private void parse() {
        if (field != null) input = field.getText().toString().trim();
        if (input.isEmpty()) {
            act.toast("请先粘贴 BV 号或视频链接");
            return;
        }
        if (busy) return;
        busy = true;
        build();
        final String raw = input;
        new Thread(() -> {
            try {
                Api.refresh(getContext());
                String bvid = Api.resolveBvid(raw);
                Models.Video v = Api.view(bvid);
                long cid = v.parts.get(0).cid;
                int best = 0;
                List<Models.Format> fmts = new ArrayList<>();
                String warn = null;
                try {
                    Api.PlayResult pr = Api.play(bvid, cid, 127);
                    fmts = new ArrayList<>(pr.formats);
                    for (Models.Dash d : pr.videos) if (d.id > best) best = d.id;
                    if (fmts.isEmpty())
                        for (Models.Dash d : pr.videos)
                            fmts.add(new Models.Format(d.id, d.id + "P"));
                } catch (Exception pe) {
                    warn = "清晰度获取失败：" + (pe.getMessage() == null ? "未知错误" : pe.getMessage());
                }
                final List<Models.Format> formats = fmts;
                final int top = best;
                final String warning = warn;
                act.runOnUiThread(() -> {
                    AppState.I.video = v;
                    AppState.I.formats = formats;
                    AppState.I.partIndex = 0;
                    AppState.I.qn = top > 0 ? top : (formats.isEmpty() ? 0 : formats.get(0).qn);
                    busy = false;
                    build();
                    if (warning != null) act.toast(warning);
                });
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? "解析失败" : e.getMessage();
                act.runOnUiThread(() -> {
                    busy = false;
                    build();
                    act.toast(msg);
                });
            }
        }).start();
    }

    private void startDownload() {
        Models.Video v = AppState.I.video;
        if (v == null || v.parts.isEmpty()) {
            act.toast("请先解析视频");
            return;
        }
        String cookie = Prefs.cookie(getContext());
        Models.Part p = v.parts.get(Math.min(AppState.I.partIndex, v.parts.size() - 1));
        Task t = new Task();
        t.bvid = v.bvid;
        t.cid = p.cid;
        t.title = v.title;
        t.part = p.part;
        t.qn = AppState.I.qn;
        t.qlabel = qualityLabel(AppState.I.qn);
        t.audioOnly = AppState.I.audioOnly;
        DownloadService.enqueue(getContext(), t);
        act.toast(cookie.isEmpty() ? "已加入下载队列（未登录，清晰度可能受限于 480P）" : "已加入下载队列");
        act.setPage(1);
    }

    private void loadCover(String url) {
        if (url == null || url.isEmpty()) return;
        final ImageView target = cover;
        new Thread(() -> {
            byte[] b = Api.bytes(url.startsWith("http://") ? "https://" + url.substring(7) : url);
            if (b == null) return;
            Bitmap bm = BitmapFactory.decodeByteArray(b, 0, b.length);
            if (bm == null) return;
            act.runOnUiThread(() -> {
                if (target != null) target.setImageBitmap(bm);
            });
        }).start();
    }

    public void refresh() {
        if (field != null) input = field.getText().toString();
        build();
    }

    public void refreshState() {
        if (field != null) input = field.getText().toString();
        build();
    }
}

