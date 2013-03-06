/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.nonmodifiable;

public final class NMDate extends java.util.Date {
	public NMDate() {
		super();
	}
	
    public NMDate(long date) {
		super(date);
	}

	public void setDate(int date) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}

	public void setHours(int hours) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}

	public void setMinutes(int minutes) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}

	public void setMonth(int month) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}

	public void setSeconds(int seconds) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}

	public void setTime(long date) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}

	public void setYear(int year) {
		throw new UnsupportedOperationException("This date cannot be modified");
	}
}
