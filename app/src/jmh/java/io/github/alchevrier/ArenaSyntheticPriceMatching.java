package io.github.alchevrier;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ArenaSyntheticPriceMatching {

    private Arena arena;
    private MemorySegment memorySegment;
    private int idx;
    private int[] indices;

    @Setup(Level.Trial)
    public void setup() {
        this.arena = Arena.ofConfined();
        this.memorySegment = this.arena.allocate(ValueLayout.JAVA_LONG, 1 << 20);
        for (var i = 0; i < 1 << 20; i++) {
            this.memorySegment.setAtIndex(ValueLayout.JAVA_LONG, i, this.packPrice(10000L + (i % 10000)));
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

    @TearDown
    public void tearDown() {
        this.arena.close();
    }

    private long packPrice(long price) {
        return price * 100 + 50;
    }

    @Benchmark
    public long priceAt(Blackhole bh) {
        var i = indices[idx & ((1 << 20) - 1)];
        idx++;
        var price = memorySegment.getAtIndex(ValueLayout.JAVA_LONG, i);
        bh.consume(price);
        return price;
    }
}
