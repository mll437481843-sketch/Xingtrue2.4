package com.bilisave.app.download;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;

/** Remux separate DASH video / audio tracks into one playable mp4 (or m4a). */
public final class Remuxer {

    public static void merge(File video, File audio, File out) throws IOException {
        MediaExtractor ve = new MediaExtractor();
        MediaExtractor ae = null;
        MediaMuxer mux = null;
        try {
            ve.setDataSource(video.getAbsolutePath());
            int vt = firstTrack(ve, "video/");
            if (vt < 0) throw new IOException("视频流里没有视频轨");
            int at = -1;
            if (audio != null && audio.exists() && audio.length() > 0) {
                ae = new MediaExtractor();
                ae.setDataSource(audio.getAbsolutePath());
                at = firstTrack(ae, "audio/");
            }
            mux = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int vOut = mux.addTrack(ve.getTrackFormat(vt));
            int aOut = at >= 0 ? mux.addTrack(ae.getTrackFormat(at)) : -1;
            mux.start();
            Reader vr = new Reader(ve, vt, vOut, mux);
            Reader ar = ae != null && at >= 0 ? new Reader(ae, at, aOut, mux) : null;
            while (!vr.done || (ar != null && !ar.done)) {
                if (vr.done) {
                    ar.pump();
                } else if (ar == null || ar.done) {
                    vr.pump();
                } else if (vr.pts <= ar.pts) {
                    vr.pump();
                } else {
                    ar.pump();
                }
            }
            mux.stop();
            mux.release();
            mux = null;
            if (!playable(out)) throw new IOException("合封结果无法播放");
        } finally {
            try {
                ve.release();
            } catch (Exception ignored) {
            }
            if (ae != null) try {
                ae.release();
            } catch (Exception ignored) {
            }
            if (mux != null) try {
                mux.release();
            } catch (Exception ignored) {
            }
        }
    }

    public static void audioOnly(File audio, File out) throws IOException {
        MediaExtractor ex = new MediaExtractor();
        MediaMuxer mux = null;
        try {
            ex.setDataSource(audio.getAbsolutePath());
            int at = firstTrack(ex, "audio/");
            if (at < 0) throw new IOException("音频流里没有音轨");
            MediaFormat f = ex.getTrackFormat(at);
            String mime = f.getString(MediaFormat.KEY_MIME);
            int fmt = (mime != null && mime.contains("mp4a"))
                    ? MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    : MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
            mux = new MediaMuxer(out.getAbsolutePath(), fmt);
            int track = mux.addTrack(f);
            mux.start();
            Reader r = new Reader(ex, at, track, mux);
            while (!r.done) r.pump();
            mux.stop();
            mux.release();
            mux = null;
        } finally {
            try {
                ex.release();
            } catch (Exception ignored) {
            }
            if (mux != null) try {
                mux.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static int firstTrack(MediaExtractor ex, String prefix) {
        for (int i = 0; i < ex.getTrackCount(); i++) {
            MediaFormat f = ex.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(prefix)) return i;
        }
        return -1;
    }

    private static boolean playable(File f) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String hasVideo = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
            String hasAudio = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            return dur != null && Long.parseLong(dur) > 500 && "yes".equals(hasVideo) && "yes".equals(hasAudio);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static final class Reader {
        final MediaExtractor ex;
        final MediaMuxer mux;
        final int srcTrack, dstTrack;
        final java.nio.ByteBuffer buf;
        final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean done;
        long pts;
        long lastPts = -1;

        Reader(MediaExtractor ex, int srcTrack, int dstTrack, MediaMuxer mux) {
            this.ex = ex;
            this.srcTrack = srcTrack;
            this.dstTrack = dstTrack;
            this.mux = mux;
            int cap = 4 * 1024 * 1024;
            MediaFormat f = ex.getTrackFormat(srcTrack);
            if (f.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                int m = f.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                if (m > cap) cap = m;
            }
            buf = java.nio.ByteBuffer.allocateDirect(cap);
            ex.selectTrack(srcTrack);
            fill();
        }

        private void fill() {
            if (done) return;
            buf.clear();
            int size = ex.readSampleData(buf, 0);
            if (size < 0) {
                done = true;
                return;
            }
            buf.position(0);
            buf.limit(size);
            info.offset = 0;
            info.size = size;
            long t = ex.getSampleTime();
            if (t < lastPts) t = lastPts;
            pts = t;
            info.presentationTimeUs = t;
            info.flags = ex.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC
                    ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
        }

        void pump() {
            if (done) return;
            mux.writeSampleData(dstTrack, buf, info);
            lastPts = pts;
            ex.advance();
            fill();
        }
    }
}

