package io.gitlab.arturbosch.detekt.api

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import kotlin.time.Duration

/**
 * Keys for storing profiling data in [Detektion.getData].
 */
object RuleProfilingKeys {
    /**
     * Key for storing aggregated rule execution metrics.
     * Value type: List<RuleExecutionMetric>
     */
    val RULE_METRICS: Key<List<RuleExecutionMetric>> = Key.create("detekt.profiling.ruleMetrics")

    /**
     * Key for storing per-file rule execution data.
     * Value type: List<RuleFileExecution>
     */
    val FILE_EXECUTIONS: Key<List<RuleFileExecution>> = Key.create("detekt.profiling.fileExecutions")

    /**
     * Key indicating that parallel analysis was disabled for profiling.
     * Value type: Boolean
     */
    val PARALLEL_DISABLED: Key<Boolean> = Key.create("detekt.profiling.parallelDisabled")
}

/**
 * Aggregated execution metrics for a single rule across all analyzed files.
 *
 * @property ruleId the unique identifier of the rule
 * @property ruleSetId the rule set this rule belongs to
 * @property totalDuration the cumulative time spent executing this rule
 * @property executionCount the number of times this rule was executed
 * @property totalFindings the total number of findings from this rule
 */
data class RuleExecutionMetric(
    val ruleId: String,
    val ruleSetId: String,
    val totalDuration: Duration,
    val executionCount: Int,
    val totalFindings: Int
)

/**
 * Execution details for a single rule on a single file.
 *
 * @property ruleId the unique identifier of the rule
 * @property ruleSetId the rule set this rule belongs to
 * @property filePath the path of the analyzed file
 * @property duration the time taken to execute the rule on this file
 * @property findings the number of findings detected
 */
data class RuleFileExecution(
    val ruleId: String,
    val ruleSetId: String,
    val filePath: String,
    val duration: Duration,
    val findings: Int
)
