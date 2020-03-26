package groove.benchmark;

import groove.explore.Generator;
import org.kohsuke.args4j.CmdLineException;
import org.openjdk.jmh.annotations.*;

import java.io.File;

@State(Scope.Benchmark)
public class BenchConfig {
    private static final String STRATEGY_ABEONA_BFS = "abeona:fifo,flags,";
    private static final String STRATEGY_ABEONA_DFS = "abeona:lifo,flags,";
    private static final String STRATEGY_GROOVE_BFS = "bfs";
    private static final String STRATEGY_GROOVE_DFS = "dfs";

    @Param({"bfs", "dfs"})
    public String algorithm;
    @Param({"abeona", "groove"})
    public String framework;
    @Param({
            "leader-election:start-1",
            "leader-election:start-2",
            "leader-election:start-3",
            "leader-election:start-4",
            "leader-election:start-5",
            "leader-election:start-6",
    })
    public String problem;

    public String getStrategyName() {
        switch (framework + "_" + algorithm) {
            case "abeona_bfs":
                return STRATEGY_ABEONA_BFS;
            case "abeona_dfs":
                return STRATEGY_ABEONA_DFS;
            case "groove_bfs":
                return STRATEGY_GROOVE_BFS;
            case "groove_dfs":
                return STRATEGY_GROOVE_DFS;
        }
        throw new IllegalStateException("Should not happen");
    }

    public String getParameters() {
        final var problemPieces = problem.split(":");
        final var grammarName = problemPieces[0];
        final var grammarPath = "grammars" + File.separator + grammarName;
        final var graphName = problemPieces[1];
        final var graphPath = grammarPath + ".gps" + File.separator + graphName;
        return grammarPath + " -s " + getStrategyName() + " -a ruleapp:hasLeader -r 1 " + graphPath;
    }

    Generator generator;

    @Setup(Level.Invocation)
    public void prepareRun() throws CmdLineException {
        final var args = getParameters();
        generator = new Generator(args.split(" "));
    }

    public void run() {
        try {
            generator.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
