package com.vucelich.vistamacfix;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mac replacement for {@code glCopyBufferSubData}. The source buffer is read
 * into a temporary direct {@link ByteBuffer} with {@code glGetBufferSubData}
 * (or from its existing map, when the buffer is mapped and that call is
 * illegal) and written with {@code glBufferSubData}. Bindings for the read
 * and write targets are saved and restored. On any other system the original
 * GL call is used.
 */
public final class BufferCopyFallback {
    public static final String REAL_CALL_REACHED = "real glCopyBufferSubData was reached";

    static final int CHUNK_BYTES = 4 * 1024 * 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger("vista_mac_fix");
    private static final AtomicBoolean LOGGED_REDIRECT = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_LATER_FAILURE = new AtomicBoolean();
    private static final AtomicLong REDIRECTED = new AtomicLong();
    private static final ThreadLocal<ByteBuffer> SCRATCH = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(8192));

    private BufferCopyFallback() {
    }

    public static void copy(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (!AppleGl.isActive()) {
            direct(readTarget, writeTarget, readOffset, writeOffset, size);
            return;
        }
        try {
            transfer(LwjglGl.INSTANCE, readTarget, writeTarget, readOffset, writeOffset, size);
            long count = REDIRECTED.incrementAndGet();
            if (LOGGED_REDIRECT.compareAndSet(false, true)) {
                System.err.println("Vista Mac Fix: glCopyBufferSubData is using a CPU readback on this client");
                LOGGER.info("Vista Mac Fix: glCopyBufferSubData is using a CPU readback on this client");
            } else if (count % 500 == 0) {
                LOGGER.debug("Vista Mac Fix: redirected {} buffer copies", count);
            }
        } catch (Throwable error) {
            if (LOGGED_FAILURE.compareAndSet(false, true)) {
                LOGGER.warn("Vista Mac Fix: CPU buffer copy failed; the Apple copy was skipped", error);
            } else if (LOGGED_LATER_FAILURE.compareAndSet(false, true)) {
                LOGGER.warn("Vista Mac Fix: later CPU buffer copies are also being skipped");
            }
        }
    }

    /**
     * The unredirected GL call. With {@code -Dvista_mac_fix.trap=true} this throws
     * instead, so a test run can prove the native function was not used.
     */
    public static void direct(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (AppleGl.trapNativeCalls()) {
            throw new IllegalStateException(REAL_CALL_REACHED);
        }
        LwjglGl.nativeCopy(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    static long redirectedCount() {
        return REDIRECTED.get();
    }

    static void transfer(Gl gl, int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (size < 0) {
            throw new IllegalArgumentException("negative glCopyBufferSubData size " + size);
        }
        if (size == 0) {
            return;
        }
        int readQuery = bindingQuery(readTarget);
        int writeQuery = bindingQuery(writeTarget);
        int savedRead = readQuery == 0 ? 0 : gl.getInteger(readQuery);
        int savedWrite = writeQuery == 0 || writeTarget == readTarget ? savedRead : gl.getInteger(writeQuery);
        try {
            long remaining = size;
            long readAt = readOffset;
            long writeAt = writeOffset;
            while (remaining > 0) {
                int chunk = (int) Math.min(remaining, CHUNK_BYTES);
                ByteBuffer scratch = scratch(chunk);
                gl.read(readTarget, readAt, scratch);
                scratch.position(0);
                scratch.limit(chunk);
                gl.write(writeTarget, writeAt, scratch);
                readAt += chunk;
                writeAt += chunk;
                remaining -= chunk;
            }
        } finally {
            if (readQuery != 0) {
                gl.bindBuffer(readTarget, savedRead);
            }
            if (writeQuery != 0 && writeTarget != readTarget) {
                gl.bindBuffer(writeTarget, savedWrite);
            }
        }
    }

    /**
     * Rejects a mapped read that starts before the map or extends past
     * {@code GL_BUFFER_MAP_LENGTH}. Returns the byte offset from the map start.
     */
    static long requireInsideMappedRange(long offset, int mapOffset, int mapLength, int count) {
        long delta = offset - mapOffset;
        long length = mapLength;
        if (delta < 0 || length < 0 || delta > length || count < 0 || count > length - delta) {
            throw new IllegalStateException("read is outside the mapped range");
        }
        return delta;
    }

    static int bindingQuery(int target) {
        if (target == GL15.GL_ARRAY_BUFFER) {
            return GL15.GL_ARRAY_BUFFER_BINDING;
        }
        if (target == GL15.GL_ELEMENT_ARRAY_BUFFER) {
            return GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING;
        }
        if (target == GL21.GL_PIXEL_PACK_BUFFER) {
            return GL21.GL_PIXEL_PACK_BUFFER_BINDING;
        }
        if (target == GL21.GL_PIXEL_UNPACK_BUFFER) {
            return GL21.GL_PIXEL_UNPACK_BUFFER_BINDING;
        }
        if (target == GL31.GL_COPY_READ_BUFFER || target == GL31.GL_COPY_WRITE_BUFFER) {
            // The binding query token is the same value as the target enum.
            return target;
        }
        if (target == GL31.GL_UNIFORM_BUFFER) {
            return GL31.GL_UNIFORM_BUFFER_BINDING;
        }
        if (target == GL31.GL_TEXTURE_BUFFER) {
            return GL31.GL_TEXTURE_BINDING_BUFFER;
        }
        if (target == GL30.GL_TRANSFORM_FEEDBACK_BUFFER) {
            return GL30.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING;
        }
        return 0;
    }

    private static ByteBuffer scratch(int bytes) {
        ByteBuffer buffer = SCRATCH.get();
        if (buffer.capacity() < bytes) {
            buffer = ByteBuffer.allocateDirect(bytes);
            SCRATCH.set(buffer);
        }
        buffer.clear();
        buffer.limit(bytes);
        return buffer;
    }

    interface Gl {
        int getInteger(int pname);

        void bindBuffer(int target, int buffer);

        void read(int target, long offset, ByteBuffer dst);

        void write(int target, long offset, ByteBuffer src);
    }

    private static final class LwjglGl implements Gl {
        static final LwjglGl INSTANCE = new LwjglGl();

        static void nativeCopy(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
            GL31C.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        }

        @Override
        public int getInteger(int pname) {
            return GL11.glGetInteger(pname);
        }

        @Override
        public void bindBuffer(int target, int buffer) {
            GL15.glBindBuffer(target, buffer);
        }

        @Override
        public void read(int target, long offset, ByteBuffer dst) {
            int mapped = GL15.glGetBufferParameteri(target, GL15.GL_BUFFER_MAPPED);
            if (mapped == GL11.GL_TRUE) {
                long pointer = GL15.glGetBufferPointer(target, GL15.GL_BUFFER_MAP_POINTER);
                if (pointer == 0L) {
                    throw new IllegalStateException("mapped buffer has a null pointer");
                }
                int mapOffset = GL15.glGetBufferParameteri(target, GL30.GL_BUFFER_MAP_OFFSET);
                int mapLength = GL15.glGetBufferParameteri(target, GL30.GL_BUFFER_MAP_LENGTH);
                int count = dst.remaining();
                long delta = requireInsideMappedRange(offset, mapOffset, mapLength, count);
                ByteBuffer view = MemoryUtil.memByteBuffer(pointer + delta, count);
                int position = dst.position();
                dst.put(view);
                dst.position(position);
                return;
            }
            drainErrors();
            GL15.glGetBufferSubData(target, offset, dst);
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                throw new IllegalStateException("glGetBufferSubData failed: 0x" + Integer.toHexString(error));
            }
        }

        @Override
        public void write(int target, long offset, ByteBuffer src) {
            GL15.glBufferSubData(target, offset, src);
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                throw new IllegalStateException("glBufferSubData failed: 0x" + Integer.toHexString(error));
            }
        }

        private static void drainErrors() {
            for (int i = 0; i < 8 && GL11.glGetError() != GL11.GL_NO_ERROR; i++) {
                // Drop a stale error so it is not blamed on this read.
            }
        }
    }
}
