package org.globaltester.scriptrunner;

import java.util.HashMap;
import java.util.Map;

import org.globaltester.base.UserInteraction;
import org.globaltester.sampleconfiguration.SampleConfig;

/**
 * {@link GtRuntimeRequirements} provide a container for all objects required by
 * testcases during execution. It is filled and propagated by the execution
 * engine before testexecution and available from within GT testcases through
 * FIXME
 * 
 * @author amay
 *
 */
public class GtRuntimeRequirements {

	private Map<Class<?>, Object> map = new HashMap<>();

	/**
	 * Minimal constructor for GtRuntimeRequirements.
	 * <p/>
	 * At least a {@link SampleConfig} and a {@link UserInteraction} are
	 * required to execute tests
	 * 
	 * @param userInteraction
	 * @param sampleConfig
	 */
	public GtRuntimeRequirements(UserInteraction userInteraction, SampleConfig sampleConfig) {
		put(UserInteraction.class, userInteraction);
		put(SampleConfig.class, sampleConfig);
	}

	/**
	 * Returns true iff an {@link Object} is registered for the given
	 * {@link Class}
	 * 
	 * @param clazz
	 * @return
	 */
	public boolean containsKey(Class<?> clazz) {
		return map.containsKey(clazz);
	}

	/**
	 * Returns the value to which the specified key is mapped, or null if this
	 * map contains no mapping for the key.
	 * 
	 * @see {@link Map#get(Object)}
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked") // content of map is checked within this
									// class
	public <T> T get(Class<T> clazz) {
		return (T) map.get(clazz);
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 * 
	 * @see {@link Map#put(Object, Object)}
	 * 
	 * @param clazz
	 * @param value
	 */
	public <T> void put(Class<T> clazz, T value) {
		map.put(clazz, value);
	}

}
