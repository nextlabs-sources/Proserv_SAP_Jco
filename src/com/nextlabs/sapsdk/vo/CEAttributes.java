/*
 * Created on July 22, 2014
 *
 * All sources, binaries and HTML pages (C) copyright 2012 by NextLabs Inc.,
 * San Mateo CA, Ownership remains with NextLabs Inc, All rights reserved
 * worldwide.

 */

package com.nextlabs.sapsdk.vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An attribute is a key/value pair. CEAttributes is a group of attributes
 * 
 * @author skaranam
 * @version:
 */
public class CEAttributes implements Serializable {
	private static final long serialVersionUID = 1L;

	private List<CEAttribute> attributes;

	/**
	 * Create a new group of attributes
	 */
	public CEAttributes() {
		attributes = new ArrayList<CEAttribute>();
	}

	/**
	 * Create a and fill a group of attributes with an array of key/value
	 * 
	 * @param keyvalues
	 *            an array of alternating keys and values (see toArray)
	 */
	public CEAttributes(String[] keyvalues) {
		attributes = new ArrayList<CEAttribute>();

		for (int i = 0; i < keyvalues.length; i += 2) {
			attributes.add(new CEAttribute(keyvalues[i], keyvalues[i + 1]));
		}
	}

	/**
	 * @return the attributes
	 */
	public List<CEAttribute> getAttributes() {
		return attributes;
	}

	/**
	 * @get the attribute
	 */
	public void setAttributes(List<CEAttribute> attributes) {
		this.attributes = attributes;
	}

	/**
	 * Add new attribute key/value.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value
	 * @note keys do not have to be unique. This provides a way to pass
	 *       "multi-values" through the sdk
	 */
	public void add(String key, String value) {
		attributes.add(new CEAttribute(key, value));
	}

	/**
	 * Get the number of key/value pairs
	 * 
	 * @return the number of key/value pairs
	 */
	public int size() {
		return attributes.size();
	}

	/**
	 * Return the attributes as an array of strings
	 * 
	 * +-------+---------+-------+---------+ +-------+---------+ | key 1 | value
	 * 1 | key 2 | value 2 | .... | key n | value n |
	 * +-------+---------+-------+---------+ +-------+---------+
	 * 
	 * @note the length of the array will be twice size()
	 */
	public String[] toArray() {
		String[] ret = new String[attributes.size() * 2];

		int index = 0;
		for (CEAttribute attr : attributes) {
			ret[index++] = attr.key;
			ret[index++] = attr.value;
		}

		return ret;
	}

	/*
	 * Test if two Objects are the same CEUser object.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg) {
		if (this == arg)
			return true;
		if (!(arg instanceof CEAttributes))
			return false;
		CEAttributes that = (CEAttributes) arg;
		return ((this.size() == that.size()) && this.attributes
				.equals(that.attributes));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		return result;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
	}

}
