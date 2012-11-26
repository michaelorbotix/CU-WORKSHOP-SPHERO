package com.orbotix.colorgrabbasic;

public class ColorTime {
	
	private int mColorIndex;   // Index to Rainbow color array
	private long mColorTime;   // Time in which the color appears
	
	/**
	 * Default Constructor
	 * @param index the index to rainbow color array
	 * @param time the time in ms that the color first appears
	 */
	public ColorTime(int index, long time) {
		mColorIndex = index;
		mColorTime = time;
	}
	
	/**
	 * Get the index to Rainbow color array
	 * @return the index to Rainbow color array
	 */
	public int getColorIndex() {
		return mColorIndex;
	}
	
	/**
	 * Get the time in which the color appears
	 * @return the time in milliseconds which the color appears
	 */
	public long getColorTime() {
		return mColorTime;
	}
}
