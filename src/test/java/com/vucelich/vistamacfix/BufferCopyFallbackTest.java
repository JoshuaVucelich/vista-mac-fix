package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BufferCopyFallbackTest {
    @AfterEach
    void clearTrap() {
        System.clearProperty(AppleGl.TRAP_PROPERTY);
    }

    @Test
    void copiesBytesAndRestoresBothBindings() {
        MemoryGl gl = new MemoryGl();
        // GL_COPY_READ_BUFFER and GL_COPY_WRITE_BUFFER. The binding query uses the same tokens.
        int read = 0x8F36;
        int write = 0x8F37;
        assertEquals(read, BufferCopyFallback.bindingQuery(read));
        assertEquals(write, BufferCopyFallback.bindingQuery(write));
        gl.bound.put(read, 7);
        gl.bound.put(write, 8);
        gl.alloc(7, new byte[] {1, 2, 3, 4, 5});
        gl.alloc(8, new byte[] {0, 0, 0, 0, 0});

        BufferCopyFallback.transfer(gl, read, write, 1, 2, 3);

        assertArrayEquals(new byte[] {0, 0, 2, 3, 4}, gl.data.get(8));
        assertEquals(2, gl.binds.size());
        assertArrayEquals(new int[] {read, 7}, gl.binds.get(0));
        assertArrayEquals(new int[] {write, 8}, gl.binds.get(1));
    }

    @Test
    void sameTargetIsRestoredOnce() {
        MemoryGl gl = new MemoryGl();
        int read = 0x8F36;
        gl.bound.put(read, 7);
        gl.alloc(7, new byte[] {9, 8, 7, 6});

        BufferCopyFallback.transfer(gl, read, read, 0, 2, 2);

        assertArrayEquals(new byte[] {9, 8, 9, 8}, gl.data.get(7));
        assertEquals(1, gl.binds.size());
        assertArrayEquals(new int[] {read, 7}, gl.binds.get(0));
    }

    @Test
    void zeroSizeDoesNotTouchGl() {
        MemoryGl gl = new MemoryGl();
        BufferCopyFallback.transfer(gl, 11, 22, 0, 0, 0);
        assertTrue(gl.binds.isEmpty());
        assertEquals(0, gl.reads);
    }

    @Test
    void negativeSizeThrows() {
        MemoryGl gl = new MemoryGl();
        assertThrows(IllegalArgumentException.class, () -> BufferCopyFallback.transfer(gl, 1, 2, 0, 0, -1));
    }

    @Test
    void largeCopyUsesMoreThanOneChunk() {
        int size = BufferCopyFallback.CHUNK_BYTES + 10;
        MemoryGl gl = new MemoryGl();
        gl.bound.put(1, 3);
        gl.bound.put(2, 4);
        byte[] source = new byte[size];
        byte[] dest = new byte[size];
        for (int i = 0; i < size; i++) {
            source[i] = (byte) (i * 31);
        }
        gl.alloc(3, source);
        gl.alloc(4, dest);

        BufferCopyFallback.transfer(gl, 1, 2, 0, 0, size);

        assertArrayEquals(source, gl.data.get(4));
        assertEquals(2, gl.reads);
        assertEquals(2, gl.writes);
    }

    @Test
    void mappedReadMustStayInsideTheMappedLength() {
        assertEquals(2L, BufferCopyFallback.requireInsideMappedRange(6, 4, 10, 3));
        assertEquals(10L, BufferCopyFallback.requireInsideMappedRange(10, 0, 10, 0));
        assertThrows(IllegalStateException.class,
                () -> BufferCopyFallback.requireInsideMappedRange(3, 4, 10, 1));
        assertThrows(IllegalStateException.class,
                () -> BufferCopyFallback.requireInsideMappedRange(8, 0, 10, 3));
        assertThrows(IllegalStateException.class,
                () -> BufferCopyFallback.requireInsideMappedRange(11, 0, 10, 0));
        long mapOffset = (1L << 32) + 16L;
        assertEquals(8L, BufferCopyFallback.requireInsideMappedRange(mapOffset + 8L, mapOffset, 64L, 4));
    }

    @Test
    void trapThrowsBeforeTheNativeCall() {
        System.setProperty(AppleGl.TRAP_PROPERTY, "true");
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> BufferCopyFallback.direct(1, 2, 0, 0, 4));
        assertEquals(BufferCopyFallback.REAL_CALL_REACHED, error.getMessage());
    }

    private static final class MemoryGl implements BufferCopyFallback.Gl {
        final Map<Integer, byte[]> data = new HashMap<>();
        final Map<Integer, Integer> bound = new HashMap<>();
        final List<int[]> binds = new ArrayList<>();
        int reads;
        int writes;

        void alloc(int id, byte[] bytes) {
            data.put(id, bytes);
        }

        @Override
        public int getInteger(int pname) {
            return bound.getOrDefault(pname, 0);
        }

        @Override
        public void bindBuffer(int target, int buffer) {
            binds.add(new int[] {target, buffer});
            bound.put(target, buffer);
        }

        @Override
        public void read(int target, long offset, ByteBuffer dst) {
            reads++;
            byte[] bytes = data.get(bound.get(target));
            int count = dst.remaining();
            dst.put(bytes, (int) offset, count);
        }

        @Override
        public void write(int target, long offset, ByteBuffer src) {
            writes++;
            byte[] bytes = data.get(bound.get(target));
            src.get(bytes, (int) offset, src.remaining());
        }
    }
}
