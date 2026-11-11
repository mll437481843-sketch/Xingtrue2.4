package com.bilisave.app.download;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.bilisave.app.data.Prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Copy a finished file into the user's chosen folder (SAF) or the public Movies/BiliSave collection. */
public final class Export {

    public static String publish(Context c, File src, String displayName, boolean audio) throws IOException {
        String mime = audio ? "audio/mp4" : "video/mp4";
        String tree = Prefs.dirUri(c);
        if (tree != null && !tree.isEmpty()) {
            try {
                return toTree(c, Uri.parse(tree), src, displayName, mime);
            } catch (Exception e) {
                // fall through to public media collection
            }
        }
        if (Build.VERSION.SDK_INT >= 29) return toMediaStore(c, src, displayName, mime, audio);
        return toPublicDir(c, src, displayName, audio);
    }

    private static String toTree(Context c, Uri tree, File src, String name, String mime) throws IOException {
        ContentResolver cr = c.getContentResolver();
        Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        Uri doc = DocumentsContract.createDocument(cr, parent, mime, name);
        if (doc == null) throw new IOException("无法在最外层目录创建文件");
        OutputStream os = cr.openOutputStream(doc, "w");
        copy(src, os);
        String path = DocumentsContract.getDocumentId(doc);
        int colon = path.indexOf(':');
        return colon >= 0 ? path.substring(colon + 1) : name;
    }

    private static String toMediaStore(Context c, File src, String name, String mime, boolean audio) throws IOException {
        ContentResolver cr = c.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, (audio ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES) + "/BiliSave");
        Uri collection = audio ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri item = cr.insert(collection, v);
        if (item == null) throw new IOException("无法写入系统相册目录");
        OutputStream os = cr.openOutputStream(item);
        copy(src, os);
        return (audio ? "Music" : "Movies") + "/BiliSave/" + name;
    }

    @SuppressWarnings("deprecation")
    private static String toPublicDir(Context c, File src, String name, boolean audio) throws IOException {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                audio ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES), "BiliSave");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("无法创建目录");
        File out = new File(dir, name);
        try (InputStream is = new java.io.FileInputStream(src);
             OutputStream os = new FileOutputStream(out)) {
            copy(is, os);
        }
        return (audio ? "Music" : "Movies") + "/BiliSave/" + name;
    }

    private static void copy(File src, OutputStream os) throws IOException {
        try (InputStream is = new java.io.FileInputStream(src)) {
            copy(is, os);
        } finally {
            try {
                os.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[262144];
        int n;
        while ((n = is.read(buf)) > 0) os.write(buf, 0, n);
        os.flush();
    }
}

