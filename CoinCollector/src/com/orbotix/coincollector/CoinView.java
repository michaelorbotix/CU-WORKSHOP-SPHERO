package com.orbotix.coincollector;

import java.util.Random;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class CoinView extends ImageView {

	private int mRotation;      // Current rotation (in degrees)
	private int mRotationRate;  // Amount to rotate each iteration (in degrees)
	private Point mLocation;    // X, Y Position of the coin
	
	/**
	 * Initialize a Coin Object in a random location
	 * @param context Activity context to belong to
	 * @param screenWidth Screen Width (in pixels)
	 * @param screenHeight Screen Height (in pixels)
	 */
	public CoinView(Context context, int screenWidth, int screenHeight) {
		super(context);
		
		// Get the resource identifier to grab the bitmap from memory
		// Basically create the same properties as with the Sphero logo in xml, but in code
		int id = context.getResources().getIdentifier("coin", "drawable", context.getPackageName());
		RelativeLayout.LayoutParams vp = 
		    new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, 
		                    LayoutParams.FILL_PARENT);
		this.setLayoutParams(vp);        
		this.setImageResource(id);       
		this.setScaleType(ScaleType.MATRIX);
		
		// Drop the coin in a random location on the screen with a random rotation rate
		Random randomGenerator = new Random();
		mLocation = new Point(randomGenerator.nextInt(screenWidth-getCoinWidth()),
							  randomGenerator.nextInt(screenHeight-getCoinHeight()));
		mRotationRate = randomGenerator.nextInt(50)+1;   
		
		// Create Sphero translation matrix
	    Matrix matrix = new Matrix();
	    matrix.reset();
	    matrix.postTranslate(mLocation.x, mLocation.y);

	    // Apply translation matrix
	    this.setScaleType(ScaleType.MATRIX);
	    this.setImageMatrix(matrix);
		
		mRotation = 0;
	}
	
	/**
	 * Convenient method to get the actual width dimension of the coin
	 * @return the width of the coin
	 */
	public int getCoinWidth() {
		return getDrawable().getMinimumHeight();
	}
	
	/**
	 * Convenient method to get the actual height dimension of the coin
	 * @return the width of the coin
	 */
	public int getCoinHeight() {
		return getDrawable().getMinimumWidth();
	}
	
	/**
	 * Returns the X, Y Position of the coin
	 * @return X, Y Position of the Coin
	 */
	public Point getCoinLocation() {
		return mLocation;
	}
	
	/**
	 * Rotate the Coin View a little bit every frame update
	 */
	public void rotate() {
		mRotation = (mRotation + mRotationRate) % 360;
		// Create Sphero translation matrix
	    Matrix matrix = new Matrix();
	    matrix.reset();
	    matrix.postTranslate(mLocation.x, mLocation.y);
	    matrix.postRotate(mRotation, mLocation.x+(getCoinWidth()/2), mLocation.y+(getCoinHeight()/2));
	    // Apply rotation matrix
	    this.setImageMatrix(matrix);
	}
}
