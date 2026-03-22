package io.github.alchevrier;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MapSyntheticPriceMatching {

    private Map<Integer, Long> priceMap;
    private int idx;
    private int[] indices;

    @Setup(Level.Trial)
    public void setup() {
        this.priceMap = new HashMap<>((int)((1 << 20) / 0.75) + 1);
        for (var i = 0; i < (1 << 20); i++) {
            priceMap.put(i, this.packPrice(10000L + (i % 10000)));
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
        var price = priceMap.get(i);
        bh.consume(price);
        return price;
    }
}
