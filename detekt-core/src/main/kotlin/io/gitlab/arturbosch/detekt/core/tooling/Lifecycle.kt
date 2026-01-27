package io.gitlab.arturbosch.detekt.core.tooling

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Detektion
import io.gitlab.arturbosch.detekt.api.FileProcessListener
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.RuleExecutionListener
import io.gitlab.arturbosch.detekt.api.RuleProfilingKeys
import io.gitlab.arturbosch.detekt.api.RuleSetId
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.api.UnstableApi
import io.gitlab.arturbosch.detekt.core.Analyzer
import io.gitlab.arturbosch.detekt.core.DetektResult
import io.gitlab.arturbosch.detekt.core.FileProcessorLocator
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.config.validation.checkConfiguration
import io.gitlab.arturbosch.detekt.core.extensions.handleReportingExtensions
import io.gitlab.arturbosch.detekt.core.generateBindingContext
import io.gitlab.arturbosch.detekt.core.profiling.RuleExecutionListenerLocator
import io.gitlab.arturbosch.detekt.core.reporting.OutputFacade
import io.gitlab.arturbosch.detekt.core.rules.createRuleProviders
import io.gitlab.arturbosch.detekt.core.util.PerformanceMonitor.Phase
import io.gitlab.arturbosch.detekt.core.util.getOrCreateMonitor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

internal interface Lifecycle {

    val baselineConfig: Config
    val settings: ProcessingSettings
    val parsingStrategy: ParsingStrategy
    val bindingProvider: (files: List<KtFile>) -> BindingContext
    val processorsProvider: () -> List<FileProcessListener>
    val ruleSetsProvider: () -> List<RuleSetProvider>
    val ruleListenersProvider: () -> List<RuleExecutionListener>

    @OptIn(UnstableApi::class)
    private fun <R> measure(phase: Phase, block: () -> R): R = settings.getOrCreateMonitor().measure(phase, block)

    fun analyze(): Detektion {
        measure(Phase.ValidateConfig) { checkConfiguration(settings, baselineConfig) }
        val filesToAnalyze = measure(Phase.Parsing) { parsingStrategy.invoke(settings) }
        val bindingContext = measure(Phase.Binding) { bindingProvider.invoke(filesToAnalyze) }
        val (processors, ruleSets, ruleListeners) = measure(Phase.LoadingExtensions) {
            Triple(
                processorsProvider.invoke(),
                ruleSetsProvider.invoke(),
                ruleListenersProvider.invoke()
            )
        }

        val result = measure(Phase.Analyzer) {
            val analyzer = Analyzer(settings, ruleSets, processors, ruleListeners)

            // Notify rule listeners about the start of analysis
            if (ruleListeners.isNotEmpty()) {
                val allRules = ruleSets.flatMap { it.instance(settings.config).rules }
                ruleListeners.forEach { it.onStart(filesToAnalyze, allRules) }
            }

            processors.forEach { it.onStart(filesToAnalyze, bindingContext) }
            val findings: Map<RuleSetId, List<Finding>> = analyzer.run(filesToAnalyze, bindingContext)
            var result: Detektion = DetektResult(findings.toSortedMap())
            processors.forEach { it.onFinish(filesToAnalyze, result, bindingContext) }

            // Allow rule listeners to process and augment the result
            if (ruleListeners.isNotEmpty()) {
                result = ruleListeners.fold(result) { acc, listener -> listener.onFinish(acc) }
                if (analyzer.parallelDisabledForProfiling) {
                    result.addData(RuleProfilingKeys.PARALLEL_DISABLED, true)
                }
            }

            result
        }

        return measure(Phase.Reporting) {
            val finalResult = handleReportingExtensions(settings, result)
            OutputFacade(settings).run(finalResult)
            finalResult
        }
    }
}

internal class DefaultLifecycle(
    override val baselineConfig: Config,
    override val settings: ProcessingSettings,
    override val parsingStrategy: ParsingStrategy,
    override val bindingProvider: (files: List<KtFile>) -> BindingContext =
        { generateBindingContext(settings.environment, settings.classpath, it, settings::debug) },
    override val processorsProvider: () -> List<FileProcessListener> =
        { FileProcessorLocator(settings).load() },
    override val ruleSetsProvider: () -> List<RuleSetProvider> =
        { settings.createRuleProviders() },
    override val ruleListenersProvider: () -> List<RuleExecutionListener> =
        { RuleExecutionListenerLocator(settings).load() }
) : Lifecycle
