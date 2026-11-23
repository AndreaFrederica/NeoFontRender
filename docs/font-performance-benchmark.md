# Font rendering benchmark

Use the client with the same world, resolution, GUI scale, font settings, and camera for each
run. Enable the optional counters with the JVM argument `-Dnfr.fontPerfStats=true`.

Run the render-thread comparison in game:

```text
/neofontrender benchmark draw 1000
```

The command renders the same prepared text through two temporary submission modes:

- `off`: NFR's legacy per-glyph submission.
- `auto`: NFR batching when Actinium's streaming drawer is available, otherwise the configured
  fallback.

The result reports total time, microseconds per draw, batch flushes, glyph draws, and SDF draws.
Repeat each mode at least three times after one warm-up run. Compare medians from the same client
session; GPU driver shader compilation and texture cache warm-up can dominate the first run.

For a call-stack profile, start Spark immediately before the benchmark and stop it afterward:

```text
/spark profiler start --timeout 60
/neofontrender benchmark draw 2000
/spark profiler stop
```

The important stacks are `TextRunBatcher.flush`, `BakedGlyph.renderClipped`, and
`CosmicSdfPipeline.drawRegion`. The NFR counters are complementary to Spark: counters show how
many submissions and vertices were produced, while Spark shows where the CPU time was spent.

When comparing Actinium itself, keep NFR in `auto` mode and run once with Actinium's streaming
drawer enabled and once with it disabled before launching the client. The Actinium drawer's
runtime switch is read during class initialization, so changing it requires a client restart.
