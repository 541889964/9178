package com.superrouter.dytool;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 纯系统 API 提取音频：从 MP4 中抽出音频轨（通常 AAC），
 * 直接封装为 .m4a。不做转码 —— 秒级完成、零额外功耗、音质无损。
 */
public class AudioExtractor {

    /** @return 生成的 m4a 文件 */
    public static File extract(File video, File outM4a) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            extractor.setDataSource(video.getAbsolutePath());

            int audioTrack = -1;
            MediaFormat audioFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrack = i;
                    audioFormat = f;
                    break;
                }
            }
            if (audioTrack < 0 || audioFormat == null) {
                throw new IOException("该视频没有音轨");
            }

            extractor.selectTrack(audioTrack);

            if (outM4a.exists()) outM4a.delete();
            muxer = new MediaMuxer(outM4a.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int outTrack = muxer.addTrack(audioFormat);
            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            long totalUs = audioFormat.containsKey(MediaFormat.KEY_DURATION)
                    ? audioFormat.getLong(MediaFormat.KEY_DURATION) : 0;

            while (true) {
                int size = extractor.readSampleData(buffer, 0);
                if (size < 0) break;

                info.offset = 0;
                info.size = size;
                info.presentationTimeUs = extractor.getSampleTime();
                info.flags = extractor.getSampleFlags();

                muxer.writeSampleData(outTrack, buffer, info);
                extractor.advance();
            }

            return outM4a;
        } finally {
            try { extractor.release(); } catch (Exception ignored) {}
            if (muxer != null) {
                try { muxer.stop(); } catch (Exception ignored) {}
                try { muxer.release(); } catch (Exception ignored) {}
            }
        }
    }
}
