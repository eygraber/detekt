package io.gitlab.arturbosch.detekt.core.reporting

import io.gitlab.arturbosch.detekt.api.Detektion
import io.gitlab.arturbosch.detekt.api.OutputReport
import io.gitlab.arturbosch.detekt.api.RuleFileExecution
import io.gitlab.arturbosch.detekt.api.RuleProfilingKeys

/**
 * Output report that generates a CSV file with rule execution profiling data.
 *
 * The report includes per-file execution details for each rule, including:
 * - RuleSet: The rule set containing the rule
 * - Rule: The rule identifier
 * - File: The file path that was analyzed
 * - Duration(ms): Time taken to execute the rule on the file
 * - Findings: Number of findings detected
 *
 * To use this report, enable profiling and specify the output file:
 * ```
 * detekt --profiling --report profiling:profile.csv
 * ```
 */
class RuleProfilingOutputReport : OutputReport() {

    override val id: String = "RuleProfilingOutputReport"

    override val ending: String = "csv"

    override val name: String = "profiling"

    override fun render(detektion: Detektion): String? {
        val executions = detektion.getData(RuleProfilingKeys.FILE_EXECUTIONS)
            ?: return null

        if (executions.isEmpty()) {
            return null
        }

        return buildString {
            appendLine("RuleSet,Rule,File,Duration(ms),Findings")
            executions
                .sortedWith(compareBy({ it.ruleSetId }, { it.ruleId }, { it.filePath }))
                .forEach { execution ->
                    appendLine(formatExecution(execution))
                }
        }
    }

    private fun formatExecution(execution: RuleFileExecution): String {
        val durationMs = execution.duration.inWholeMilliseconds
        val escapedFilePath = escapeCSV(execution.filePath)
        return "${execution.ruleSetId},${execution.ruleId},$escapedFilePath,$durationMs,${execution.findings}"
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
