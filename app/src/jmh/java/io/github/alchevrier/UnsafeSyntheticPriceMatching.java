package io.github.alchevrier;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import sun.misc.Unsafe;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class UnsafeSyntheticPriceMatching {

    private static final Unsafe UNSAFE;
    private long[] prices;
    private int idx;
    private int[] indices;

    static {
        try {
            var f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Setup(Level.Trial)
    public void setup() {
        prices = new long[1 << 20];
        for (var i = 0; i < prices.length; i++) {
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
    public long priceAt(Blackhole bh) {
        var i = indices[idx & ((1 << 20) - 1)];
        idx++;
        var price = UNSAFE.getLong(prices, Unsafe.ARRAY_LONG_BASE_OFFSET + (i << 3));
        bh.consume(price);
        return price;
    }
}
