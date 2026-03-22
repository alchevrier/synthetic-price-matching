package io.github.alchevrier;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class VarHandleSyntheticPriceMatching {

    private static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(long[].class);

    private long[] prices;
    private int[] indices;
    private int idx;

    @Setup(Level.Trial)
    public void setup() {
        prices = new long[1 << 20];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = this.packPrice(10000L + (i % 10000));
        }

        indices = new int[1 << 20];
        idx = 0;
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        // shuffle
        var rng = new Random(42);
        for (int i = indices.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = indices[i]; indices[i] = indices[j]; indices[j] = tmp;
        }
    }

    private long packPrice(long price) {
        return price * 100 + 50;
    }

    @Benchmark
    public long priceAtPlain(Blackhole bh) {
        int i = indices[idx & ((1 << 20) - 1)];
        idx++;
        return (long) HANDLE.get(prices, i); // plain read
    }

    @Benchmark
    public long priceAtAcquire(Blackhole bh) {
        int i = indices[idx & ((1 << 20) - 1)];
        idx++;
        return (long) HANDLE.getAcquire(prices, i); // acquire semantics (provides guarantees about instructions orders before it)
    }

    @Benchmark
    public long priceAtVolatile(Blackhole bh) {
        int i = indices[idx & ((1 << 20) - 1)];
        idx++;
        return (long) HANDLE.getVolatile(prices, i); // volatile read (happens-before write before reads)
    }
}
