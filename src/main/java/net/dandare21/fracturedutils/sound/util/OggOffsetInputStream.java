package net.dandare21.fracturedutils.sound.util;

import net.dandare21.fracturedutils.FracturedUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OggOffsetInputStream extends FilterInputStream {

    private final byte[] headerData;
    private int headerPos = 0;

    public OggOffsetInputStream(InputStream in, long totalSize, long offsetMs) throws IOException {
        super(in);

        if (offsetMs > 500L && totalSize > 4096) {
            // 1. Read first 2048 bytes containing Vorbis setup headers
            byte[] buf = new byte[2048];
            int read = 0;
            while (read < buf.length) {
                int n = in.read(buf, read, buf.length - read);
                if (n <= 0) break;
                read += n;
            }

            if (read < buf.length) {
                byte[] exact = new byte[read];
                System.arraycopy(buf, 0, exact, 0, read);
                this.headerData = exact;
            } else {
                this.headerData = buf;
            }

            // 2. Estimate skip byte offset (~22000 bytes/sec for standard 160kbps Ogg)
            double seconds = offsetMs / 1000.0;
            double estimatedBytesPerSec = 22000.0;
            long skipTarget = (long) (seconds * estimatedBytesPerSec);
            long maxSkip = Math.max(0, totalSize - 8192);
            skipTarget = Math.min(skipTarget, maxSkip);

            long actualSkipped = 0;
            while (actualSkipped < skipTarget) {
                long n = in.skip(skipTarget - actualSkipped);
                if (n <= 0) break;
                actualSkipped += n;
            }
            FracturedUtils.LOGGER.info("[OggOffsetInputStream] Prepared Ogg stream offset for {}ms (headers: {} bytes, skipped: {} bytes)", offsetMs, headerData.length, actualSkipped);
        } else {
            this.headerData = new byte[0];
        }
    }

    @Override
    public int read() throws IOException {
        if (headerPos < headerData.length) {
            return headerData[headerPos++] & 0xFF;
        }
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
        if (len == 0) return 0;

        if (headerPos < headerData.length) {
            int available = headerData.length - headerPos;
            int bytesToCopy = Math.min(len, available);
            System.arraycopy(headerData, headerPos, b, off, bytesToCopy);
            headerPos += bytesToCopy;
            return bytesToCopy;
        }

        return super.read(b, off, len);
    }
}
