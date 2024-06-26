package io.deephaven.example;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.Deflater;

/**
 * This is meant to be a small reproduction of the error "Retried waiting for GCLocker too often allocating". In
 * combination with {@link SoftReference}, we are able to get an {@link OutOfMemoryError} error, which seems to break
 * its JavaDoc contract: "All soft references to softly-reachable objects are guaranteed to have been cleared before the
 * virtual machine throws an OutOfMemoryError".
 */
public class GCLockerTooOftenAllocating {

    public static void main(String[] args) {
        // Startup some number of continuous computations that dip down into JNI critical sections.
        // Values are meant to be small enough to complete in a reasonable amount of time when error is not triggered.
        final int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        final int inputSize = 100_000;
        final int numLoops = 1_000;
        final Thread[] compressThreads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int seed = i;
            compressThreads[i] = new Thread(() -> {
                System.out.printf("seed=%d, totalOutputSize=%d%n", seed, loopCompress(seed, inputSize, numLoops));
            });
            compressThreads[i].setName("loopCompress-" + i);
            compressThreads[i].setDaemon(true);
        }
        for (Thread compressThread : compressThreads) {
            compressThread.start();
        }

        // Each buffer will be 1 / 1024 of heap
        final int bufferPart = 1024;
        final int bufferSize = (int)(Runtime.getRuntime().maxMemory() / bufferPart);
        // We just need to make sure the array can accommodate at least the total heap size
        final int arraySize = bufferPart * 2;
        final Reference<ByteBuffer>[] references = new Reference[arraySize];
        long count = 0;
        while (isAlive(compressThreads)) {
            references[(int)(count++ % arraySize)] = new SoftReference<>(ByteBuffer.allocate(bufferSize));
            // WeakReference does not trigger the issue
            //references[(int)(count++ % arraySize)] = new WeakReference<>(ByteBuffer.allocate(bufferSize));
        }
        System.out.printf("Completed, count=%d%n", count);
    }

    private static boolean isAlive(Thread[] threads) {
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static long loopCompress(int seed, int inputSize, int numLoops) {
        long totalOutputSize = 0;
        final Random random = new Random(seed);
        final byte[] input = new byte[inputSize];
        // assume it's compressible
        final byte[] output = new byte[inputSize];
        for (int i = 0; i < numLoops; i++) {
            fillNonRandom(random, ByteBuffer.wrap(input));
            compress(input, output);
            totalOutputSize += compress(input, output);
        }
        return totalOutputSize;
    }

    private static void fillNonRandom(Random r, ByteBuffer b) {
        while (b.remaining() >= 8) {
            // gaussian so we have something that is compressible
            b.putDouble(r.nextGaussian());
        }
        while (b.hasRemaining()) {
            // just fill remaining w/ 0s
            b.put((byte) 0);
        }
    }

    private static int compress(byte[] input, byte[] output) {
        final Deflater compresser = new Deflater(Deflater.BEST_COMPRESSION);
        compresser.setInput(input);
        compresser.finish();
        final int compressedDataLength = compresser.deflate(output);
        compresser.end();
        return compressedDataLength;
    }
}
