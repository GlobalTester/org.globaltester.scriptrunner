package org.globaltester.scriptrunner;

public class AssertionFailure extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AssertionFailure(String message) {
		super(message);
	}
}
