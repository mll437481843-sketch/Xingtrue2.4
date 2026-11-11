package com.bilisave.app.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

/** Material You / Monet palette. Dynamic from wallpaper on API 31+, seeded from Bilibili pink otherwise. */
public final class Monet {
    public boolean dark;
    public boolean dynamic;
    public int primary, onPrimary, primaryContainer, onPrimaryContainer;
    public int secondary, tertiary;
    public int surface, surfaceHi, surfaceVar, onSurface, onSurfaceVar, outline, error;
    public int mistA, mistB, mistC;

    public static Monet of(Context c, boolean dynamic, int mode) {
        boolean configDark = (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Monet m = new Monet();
        m.dark = mode == 0 ? configDark : mode == 2;
        m.dynamic = dynamic && Build.VERSION.SDK_INT >= 31;
        if (m.dynamic) m.fromWallpaper(c, m.dark);
        else m.fromSeed(m.dark);
        m.mistA = m.primary;
        m.mistB = m.secondary;
        m.mistC = m.tertiary;
        return m;
    }

    private void fromWallpaper(Context c, boolean dark) {
        if (dark) {
            primary = c.getColor(android.R.color.system_accent1_200);
            onPrimary = c.getColor(android.R.color.system_accent1_900);
            primaryContainer = c.getColor(android.R.color.system_accent1_700);
            onPrimaryContainer = c.getColor(android.R.color.system_accent1_100);
            secondary = c.getColor(android.R.color.system_accent2_200);
            tertiary = c.getColor(android.R.color.system_accent3_200);
            surface = c.getColor(android.R.color.system_neutral1_900);
            surfaceHi = c.getColor(android.R.color.system_neutral1_800);
            surfaceVar = c.getColor(android.R.color.system_neutral2_800);
            onSurface = c.getColor(android.R.color.system_neutral1_100);
            onSurfaceVar = c.getColor(android.R.color.system_neutral1_300);
            outline = c.getColor(android.R.color.system_neutral2_500);
        } else {
            primary = c.getColor(android.R.color.system_accent1_500);
            onPrimary = c.getColor(android.R.color.system_accent1_0);
            primaryContainer = c.getColor(android.R.color.system_accent1_100);
            onPrimaryContainer = c.getColor(android.R.color.system_accent1_900);
            secondary = c.getColor(android.R.color.system_accent2_500);
            tertiary = c.getColor(android.R.color.system_accent3_500);
            surface = c.getColor(android.R.color.system_neutral1_50);
            surfaceHi = c.getColor(android.R.color.system_neutral1_100);
            surfaceVar = c.getColor(android.R.color.system_neutral2_100);
            onSurface = c.getColor(android.R.color.system_neutral1_900);
            onSurfaceVar = c.getColor(android.R.color.system_neutral1_700);
            outline = c.getColor(android.R.color.system_neutral2_500);
        }
        error = dark ? 0xFFFFB4AB : 0xFFBA1A1A;
    }

    private void fromSeed(boolean dark) {
        if (dark) {
            primary = hsv(348, 0.62f, 0.96f);
            onPrimary = hsv(348, 0.55f, 0.18f);
            primaryContainer = hsv(348, 0.40f, 0.34f);
            onPrimaryContainer = hsv(348, 0.35f, 0.95f);
            secondary = hsv(268, 0.42f, 0.82f);
            tertiary = hsv(188, 0.42f, 0.74f);
            surface = hsv(262, 0.18f, 0.085f);
            surfaceHi = hsv(262, 0.15f, 0.145f);
            surfaceVar = hsv(262, 0.13f, 0.20f);
            onSurface = hsv(340, 0.18f, 0.96f);
            onSurfaceVar = hsv(340, 0.09f, 0.72f);
            outline = hsv(340, 0.08f, 0.46f);
            error = 0xFFFFB4AB;
        } else {
            primary = hsv(348, 0.66f, 0.98f);
            onPrimary = 0xFFFFFFFF;
            primaryContainer = hsv(348, 0.42f, 0.98f);
            onPrimaryContainer = hsv(348, 0.55f, 0.32f);
            secondary = hsv(268, 0.40f, 0.74f);
            tertiary = hsv(188, 0.40f, 0.66f);
            surface = hsv(340, 0.12f, 0.985f);
            surfaceHi = hsv(340, 0.10f, 0.95f);
            surfaceVar = hsv(340, 0.08f, 0.90f);
            onSurface = hsv(340, 0.14f, 0.13f);
            onSurfaceVar = hsv(340, 0.08f, 0.42f);
            outline = hsv(340, 0.08f, 0.66f);
            error = 0xFFBA1A1A;
        }
    }

    private static int hsv(float h, float s, float v) {
        return Color.HSVToColor(new float[]{h, s, v});
    }
}

