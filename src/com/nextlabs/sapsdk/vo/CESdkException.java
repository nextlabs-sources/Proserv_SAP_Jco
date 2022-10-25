/*
 * Created on July 22, 2014
 *
 * All sources, binaries and HTML pages (C) copyright 2012 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.
 */
package com.nextlabs.sapsdk.vo;

public class CESdkException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * The default constructor
	 */
	public CESdkException() {
		super();
	}

	/**
	 * Construct a <code>CESdkException</code> with the given message
	 * 
	 * @param message
	 *            the message for the <code>CESdkException</code>
	 */
	public CESdkException(String message) {
		super(message);
	}

	/**
	 * Construct a <code>CESdkException</code> with the given cause
	 * 
	 * @param cause
	 *            the cause for the <code>CESdkException</code>
	 */
	public CESdkException(Throwable cause) {
		super(cause);
	}

	/**
	 * Construct a <code>CESdkException</code> with the given message and cause
	 * 
	 * @param message
	 *            the message for the <code>CESdkException</code>
	 * @param cause
	 *            the cause for the <code>CESdkException</code>
	 */
	public CESdkException(String message, Throwable cause) {
		super(message, cause);
	}
}
