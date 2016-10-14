package org.globaltester.scriptrunner;

import org.globaltester.base.UserInteraction;
import org.globaltester.sampleconfiguration.SampleConfig;

/**
 * This provides the minimal environment needed for test execution.
 * @author mboonk
 *
 */
public class MinimalRuntimeRequirementsProvider implements SampleConfigProvider, UserInteractionProvider {

	private UserInteraction userInteraction;
	private SampleConfig sampleConfig;

	public MinimalRuntimeRequirementsProvider(UserInteraction userInteraction, SampleConfig sampleConfig) {
		super();
		this.userInteraction = userInteraction;
		this.sampleConfig = sampleConfig;
	}

	@Override
	public UserInteraction getUserInteraction() {
		return userInteraction;
	}

	@Override
	public SampleConfig getSampleConfig() {
		return sampleConfig;
	}

}
