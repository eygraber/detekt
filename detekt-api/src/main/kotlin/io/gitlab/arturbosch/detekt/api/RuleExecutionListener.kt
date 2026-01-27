package io.gitlab.arturbosch.detekt.api

import org.jetbrains.kotlin.psi.KtFile
import kotlin.time.Duration

/**
 * Extension point for listening to rule execution events.
 *
 * Implementations can collect metrics about rule execution timing and findings.
 * When profiling is enabled and any RuleExecutionListener is active, parallel
 * analysis is automatically disabled to ensure accurate timing measurements.
 *
 * @see Extension
 */
interface RuleExecutionListener : Extension {

    /**
     * Called once at the start of analysis, before any rules are executed.
     *
     * @param files the list of files to be analyzed
     * @param rules the list of rules that will be executed
     */
    fun onStart(files: List<KtFile>, rules: List<BaseRule>) {}

    /**
     * Called before a rule is executed on a specific file.
     *
     * @param file the file about to be analyzed
     * @param rule the rule about to be executed
     */
    fun beforeRuleExecution(file: KtFile, rule: BaseRule) {}

    /**
     * Called after a rule has been executed on a specific file.
     *
     * @param file the file that was analyzed
     * @param rule the rule that was executed
     * @param findings the number of findings detected by the rule on this file
     * @param duration the time taken to execute the rule
     */
    fun afterRuleExecution(file: KtFile, rule: BaseRule, findings: Int, duration: Duration) {}

    /**
     * Called after all rules have been executed on all files.
     * Implementations can use this to aggregate metrics and store them in the result.
     *
     * @param result the detection result to potentially augment with metrics
     * @return the (potentially modified) detection result
     */
    fun onFinish(result: Detektion): Detektion = result
}
