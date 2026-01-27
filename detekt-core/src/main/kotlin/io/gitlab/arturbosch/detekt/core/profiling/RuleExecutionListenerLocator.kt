package io.gitlab.arturbosch.detekt.core.profiling

import io.gitlab.arturbosch.detekt.api.RuleExecutionListener
import io.gitlab.arturbosch.detekt.api.UnstableApi
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import io.gitlab.arturbosch.detekt.core.extensions.loadExtensions

/**
 * Locates and loads [RuleExecutionListener] implementations when profiling is enabled.
 *
 * Always includes the built-in [RuleProfilingListener] and any additional listeners
 * registered via ServiceLoader.
 */
class RuleExecutionListenerLocator(private val settings: ProcessingSettings) {

    /**
     * Loads rule execution listeners if profiling is enabled.
     *
     * @return list of listeners to notify during rule execution, empty if profiling is disabled
     */
    @OptIn(UnstableApi::class)
    fun load(): List<RuleExecutionListener> {
        if (!settings.spec.executionSpec.profiling) {
            return emptyList()
        }

        // Load any additional listeners via ServiceLoader
        val externalListeners = loadExtensions<RuleExecutionListener>(settings)

        // Always include the built-in profiling listener
        val builtInListener = RuleProfilingListener().apply {
            init(settings.config)
            init(settings)
        }

        return listOf(builtInListener) + externalListeners
    }
}
