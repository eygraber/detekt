package io.github.detekt.tooling.api.spec

import java.util.concurrent.ExecutorService

interface ExecutionSpec {

    /**
     * [ExecutorService] to be used for parsing and analysis.
     */
    val executorService: ExecutorService?

    /**
     * Uses [executorService] to parse input paths.
     */
    val parallelParsing: Boolean

    /**
     * Uses [executorService] to run rules in parallel.
     */
    val parallelAnalysis: Boolean

    /**
     * Enables rule-level profiling to measure execution time of individual rules.
     * When enabled, parallel analysis is automatically disabled to ensure accurate timing.
     */
    val profiling: Boolean
}
