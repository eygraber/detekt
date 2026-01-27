package io.gitlab.arturbosch.detekt.core.profiling

import io.github.detekt.psi.absolutePath
import io.gitlab.arturbosch.detekt.api.BaseRule
import io.gitlab.arturbosch.detekt.api.Detektion
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleExecutionListener
import io.gitlab.arturbosch.detekt.api.RuleExecutionMetric
import io.gitlab.arturbosch.detekt.api.RuleFileExecution
import io.gitlab.arturbosch.detekt.api.RuleProfilingKeys
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Built-in [RuleExecutionListener] that collects execution timing metrics for rules.
 *
 * This listener aggregates timing data during analysis and stores the results in
 * the [Detektion] result using [RuleProfilingKeys].
 */
class RuleProfilingListener : RuleExecutionListener {

    override val id: String = "RuleProfilingListener"

    private val fileExecutions = ConcurrentHashMap.newKeySet<RuleFileExecution>()

    override fun afterRuleExecution(file: KtFile, rule: BaseRule, findings: Int, duration: Duration) {
        val ruleSetId = getRuleSetId(rule)
        fileExecutions.add(
            RuleFileExecution(
                ruleId = rule.ruleId,
                ruleSetId = ruleSetId,
                filePath = file.absolutePath().toString(),
                duration = duration,
                findings = findings
            )
        )
    }

    override fun onFinish(result: Detektion): Detektion {
        val executions = fileExecutions.toList()

        // Aggregate metrics by rule
        val aggregatedMetrics = executions
            .groupBy { it.ruleId to it.ruleSetId }
            .map { (key, executions) ->
                val (ruleId, ruleSetId) = key
                RuleExecutionMetric(
                    ruleId = ruleId,
                    ruleSetId = ruleSetId,
                    totalDuration = executions.map { it.duration }.reduce { acc, d -> acc + d },
                    executionCount = executions.size,
                    totalFindings = executions.sumOf { it.findings }
                )
            }
            .sortedByDescending { it.totalDuration }

        result.addData(RuleProfilingKeys.RULE_METRICS, aggregatedMetrics)
        result.addData(RuleProfilingKeys.FILE_EXECUTIONS, executions)

        return result
    }

    private fun getRuleSetId(rule: BaseRule): String {
        return when (rule) {
            is Rule -> rule.ruleSetConfig.parentPath ?: "unknown"
            else -> "unknown"
        }
    }
}
