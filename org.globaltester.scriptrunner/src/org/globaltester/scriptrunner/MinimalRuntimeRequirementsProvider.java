package org.globaltester.scriptrunner;

import org.globaltester.base.UserInteraction;
import org.globaltester.sampleconfiguration.SampleConfig;

public class MinimalRuntimeRequirementsProvider implements SampleConfigProvider, UserInteractionProvider {

	private UserInteraction userInteraction;
	private SampleConfig sampleConfig;

	@Override
	public UserInteraction getUserInteraction() {
		return userInteraction;
	}

	public MinimalRuntimeRequirementsProvider(UserInteraction userInteraction, SampleConfig sampleConfig) {
		super();
		this.userInteraction = userInteraction;
		this.sampleConfig = sampleConfig;
	}

	@Override
	public SampleConfig getSampleConfig() {
		return sampleConfig;
	}

}
