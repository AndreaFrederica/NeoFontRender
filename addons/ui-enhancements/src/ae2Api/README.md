# AE2 Supergiant compile signatures

This source set describes only the public binary signatures used by
`NfrAe2TooltipApi`. It allows a clean checkout to compile the optional bridge
without the local AE2 development checkout. AE2 provides the real classes at
runtime; these classes must never be included in UIE's development or remapped
JAR. Release workflows verify that no `ae2/` entries are bundled.

When updating the bridge, verify against a real MCP-named AE2 development JAR:

```text
gradlew :addons:ui-enhancements:test --tests '*Ae2BinaryContractTest' -Pae2CompatJar=/path/to/ae2-dev.jar
```

The contract test checks the bridge's emitted JVM calls against the real class
descriptors, including interface and static invocation modes.
