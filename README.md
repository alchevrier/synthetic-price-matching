# Synthetic price matching

A repository to understand on a simple use-case of synthetic price matching the key differences in performance between: Unsafe (direct memory access mechanism to be deprecated), Panama (new API to access off-heap memory), Map (classic way of storing data - not used on the hot path due to performance).

This is not simulating a real production scenario as in reality there will be multiple threads in contention for the data as well as hardware side-effects. This is just meant to validate the correct use of API for the job. Absolute numbers depend on the environment in which the code runs and must be interpreted accordingly.

## Overview

We are implementing the following:
- ArenaSyntheticPriceMatching (new API to access off-heap memory)
- MapSyntheticPriceMatching (classic way of storing data).
- UnsafeSyntheticPriceMatching (direct memory access mechanism to be deprecated)

### Key Decisions

#### JMH class annotations

@State(Scope.Thread) -> Used because Arena.ofConfined is thread-confined, accessing it from benchmark infrastructure threads would throw WrongThreadException (the case if Scope.Benchmark were used instead). Also idx used in different benchmarks is mutable and therefore cannot be shared across threads without data races (no synchronization is applied). 

@BenchmarkMode(Mode.SampleTime) -> Measures wall-clock time of every invocation and builds a histogram. Sampling is of the distribution, not random skipping of calls. 

@OutputTimeUnit(TimeUnit.NANOSECONDS) -> Capturing the result in ns 

#### Arena

Arena.ofConfined() used for the test due to the single-threaded manner of the benchmark. 

#### How I defined the next price to be accessed
- int idx -> next index to define the possible price to be fetched, incremented after each price access
- int[] indices -> spread possible indices mimicking a computed next price to be fetched, shuffled to mimic random access in production

Next price index is defined as 'idx & ((1 << 20) - 1)':
- & ((1 << 20) - 1) to avoid division, we use this technique since ((1 << 20) - 1) is a power of 2 minus 1 (all lower bits set to 1) so AND gives the remainder of modulo 2²⁰. 

## Conclusion

Results below were collected on a Windows development machine (JDK 25.0.2), 5 forks × 5 warmup × 5 measurement iterations, `@BenchmarkMode(Mode.SampleTime)`. Absolute numbers are platform-specific; the relative ordering and tail behaviour are the meaningful signal.

| Benchmark | Mean (ns) | p90 (ns) | p99 (ns) | p99.9 (ns) | p100 (ns) |
|---|---|---|---|---|---|
| `UnsafeSyntheticPriceMatching` | 185 | 200 | 300 | 900 | 2,215,936 |
| `ArenaSyntheticPriceMatching` | 189 | 200 | 300 | 800 | 930,816 |
| `MapSyntheticPriceMatching` | 209 | 300 | 600 | 2,200 | 5,816,320 |

`Unsafe` and `Arena` are statistically indistinguishable up to p99.9. The separation from `HashMap` begins at p90 and widens through the tail: p99.9 is ~2.5x worse for `HashMap`, and p100 is ~6x worse — consistent with GC pauses on boxed heap objects that off-heap layouts avoid entirely.

The mean difference (~12%) is not the story. The tail is.

