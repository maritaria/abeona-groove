package groove.benchmark;

import org.openjdk.jmh.annotations.*;

@Fork(value = 1, warmups = 0)
@Warmup(iterations = 0)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.SingleShotTime)
public class Benchmarks {
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @Benchmark
    public void executeBenchmark(BenchConfig config) {
        config.run();
    }
}
