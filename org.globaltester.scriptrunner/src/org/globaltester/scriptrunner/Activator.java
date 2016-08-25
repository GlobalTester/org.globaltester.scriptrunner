package org.globaltester.scriptrunner;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	public static String PREFERENCE_ID_LAST_USED_SAMPLE_CONFIG_PROJECT = "org.globaltester.scriptrunner.lastUsedSampleConfig";
	
	private static BundleContext context;
	
	public static BundleContext getContext() {
		return context;
	}
	
	private static void setContext(BundleContext bundleContext) {
		context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.setContext(bundleContext);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.setContext(null);
	}

}
