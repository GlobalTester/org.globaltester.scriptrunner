package org.globaltester.scriptrunner;

public class AssertionWarning extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AssertionWarning(String message) {
		super(message);
	}
}
