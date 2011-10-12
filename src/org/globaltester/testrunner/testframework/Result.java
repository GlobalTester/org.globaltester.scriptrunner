package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Represents the test result of an executable unit, e.g. of a
 * TestCaseExecution.
 * 
 * The result may consist of several subResults. In this case the overall result
 * will reflect the worst subResult. This behavior may be changed by subclasses.
 * 
 * @author amay
 * 
 */
public class Result {

	public enum Status {
		PASSED, FAILURE, WARNING, NOT_APPLICABLE, UNDEFINED;

	}

	private Status status; // status this result represents
	private String comment; // comment associated with this result
	private ArrayList<Result> subResults = new ArrayList<Result>();

	public Result(Status newStatus) {
		this.status = newStatus;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Status getStatus() {
		return status;
	}

	/**
	 * Add a new subResult and rebuild the overall status
	 * 
	 * @param result
	 */
	public void addSubResult(Result subResult) {
		subResults.add(subResult);
		rebuildStatus();
	}

	/**
	 * Recalculate the overall status of this result from the subResults.
	 * 
	 * By default this returns the worst status given by an sub result.
	 */
	private void rebuildStatus() {
		Status tmpStatus = Status.PASSED;

		//search worst status in sub results
		Iterator<Result> subResultIter = subResults.iterator();
		while (subResultIter.hasNext()) {
			Result curResult = (Result) subResultIter.next();
			if (curResult.getStatus() == Status.WARNING) {
				tmpStatus = Status.WARNING;
			} else if (curResult.getStatus() == Status.FAILURE) {
				tmpStatus = Status.FAILURE;
				break;
			}
		}
		
		//change status of this result
		status = tmpStatus;

	}

}
