package com.orbotix.coincollector;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import orbotix.robot.base.*;
import orbotix.robot.sensor.AttitudeData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.view.connection.SpheroConnectionView.OnRobotConnectionEventListener;
import java.util.List;

public class CoinCollectorActivity extends Activity
{
    /**
     * Sphero Connection Activity
     */
    private SpheroConnectionView mSpheroConnectionView;
    private Handler mHandler = new Handler();

    /**
     * Data Streaming Packet Counts
     */
    private final static int TOTAL_PACKET_COUNT = 200;
    private final static int PACKET_COUNT_THRESHOLD = 50;
    private int mPacketCounter;

    /**
     * Sphero Image that collects coins
     */
    private ImageView mImageSphero;
    
    /**
     * Start Button View
     */
    private Button mStartButton;
    
    /**
     * Relative Layout to hold Sphero image and coins
     */
    private RelativeLayout mLayout;
    
    /**
     * Display Coin Score and Time Remaining
     */
    private TextView mTextScore;
    private TextView mTextTime;
    
    /**
     * Coin View
     */
    private CoinView mCoin;
    private int mCoinsCollectedCtr;
    
    private Point mImageSpheroLoc; 				 // The X and Y position of Sphero image (pixels)
    private Point mImageSpheroBounds;			 // The width and height of Sphero image (pixels)
    private int mScreenWidth;					 // Phone Screen Width (pixels)
    private int mScreenHeight;					 // Phone Screen Height (pixels) 
    private long mStartTime;					 // The Time in milliseconds at start of game round 
    private final static int GAME_DURATION = 60; // length of a round in seconds
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Keep Screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Get Relative Layout
        mLayout = (RelativeLayout)findViewById(R.id.relative_layout);
        
        // Get Sphero image into a variable
        mImageSphero = (ImageView)findViewById(R.id.image_sphero);
        
        // Text Views
        mTextScore = (TextView)findViewById(R.id.text_score);
        mTextTime = (TextView)findViewById(R.id.text_time);
        
        // Start Button
        mStartButton = (Button)findViewById(R.id.button_start);
        
        // Get Screen width and height
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mScreenHeight = displaymetrics.heightPixels;
        mScreenWidth = displaymetrics.widthPixels;
        
		// Find Sphero Connection View from layout file
		mSpheroConnectionView = (SpheroConnectionView)findViewById(R.id.sphero_connection_view);
		// This event listener will notify you when these events occur, it is up to you what you want to do during them
		mSpheroConnectionView.setOnRobotConnectionEventListener(new OnRobotConnectionEventListener() {
			@Override
			public void onRobotConnectionFailed(Robot arg0) {}
			@Override
			public void onNonePaired() {}
			
			@Override
			public void onRobotConnected(Robot arg0) {
				// Set the robot
				mRobot = arg0;
				// Hide the connection view. Comment this code if you want to connect to multiple robots
				mSpheroConnectionView.setVisibility(View.GONE);
				
				// Calling Stream Data Command right after the robot connects, will not work
				// You need to wait a second for the robot to initialize
				mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // turn rear light on
                        FrontLEDOutputCommand.sendCommand(mRobot, 1.0f);
                        // turn stabilization off
                        StabilizationCommand.sendCommand(mRobot, false);
                        // register the async data listener
                        DeviceMessenger.getInstance().addAsyncDataListener(mRobot, mDataListener);
                    }
                }, 1000);
			}
			
			@Override
			public void onBluetoothNotEnabled() {
				// See ButtonDrive Sample on how to show BT settings screen, for now just notify user
				Toast.makeText(CoinCollectorActivity.this, "Bluetooth Not Enabled", Toast.LENGTH_LONG).show();
			}
		});
		mSpheroConnectionView.showSpheros();
    }

    @Override
    protected void onStop() {
        super.onStop();

		// Shutdown Sphero connection view
        if(mRobot != null){

            StabilizationCommand.sendCommand(mRobot, true);
            FrontLEDOutputCommand.sendCommand(mRobot, 0f);

            RobotProvider.getDefaultProvider().removeAllControls();
        }
    }
    
    /**
     * Called when the start button is clicked
     * Show the Sphero Image, and start the thread to add coins and check for grabbing coins
     * 
     * @param v
     */
    public void onStartPressed(View v) {
    	// Hide Start button
    	mStartButton.setVisibility(View.GONE);
    	
    	// Send a calibrate command so the current configurization is 0,0
    	CalibrateCommand.sendCommand(mRobot, 0);
        // Start streaming data
        requestDataStreaming();
        
        // Show Sphero image and set width and height and starting position
        mImageSpheroBounds = new Point(mImageSphero.getDrawable().getBounds().width(),
        							   mImageSphero.getDrawable().getBounds().height());
        mImageSpheroLoc = new Point(0, 0);
        mImageSphero.setVisibility(View.VISIBLE);
        
        // Reset Coin counter
        mCoinsCollectedCtr = 0;
    	mTextScore.setText(this.getString(R.string.score)+mCoinsCollectedCtr);
    	mTextScore.setVisibility(View.VISIBLE);
        
    	// Set Time UI
    	mStartTime = System.currentTimeMillis();
    	mTextTime.setText(this.getString(R.string.time)+(GAME_DURATION-getTimeElapsed()));
    	mTextTime.setVisibility(View.VISIBLE);
    	
	    // Create new Coin View
	    mCoin = new CoinView(this, mScreenWidth, mScreenHeight);
	    
	    // Add coin view to our layout
	    mLayout.addView(mCoin);
    }

    private void requestDataStreaming() {

        if(mRobot != null){

            // Set up a bitmask containing the sensor information we want to stream
            final long mask = SetDataStreamingCommand.DATA_STREAMING_MASK_ACCELEROMETER_FILTERED_ALL |
                    SetDataStreamingCommand.DATA_STREAMING_MASK_IMU_ANGLES_FILTERED_ALL;

            // Specify a divisor. The frequency of responses that will be sent is 400hz divided by this divisor.
            final int divisor = 50;

            // Specify the number of frames that will be in each response. You can use a higher number to "save up" responses
            // and send them at once with a lower frequency, but more packets per response.
            final int packet_frames = 1;

            // Reset finite packet counter
            mPacketCounter = 0;

            // Count is the number of async data packets Sphero will send you before
            // it stops.  You want to register for a finite count and then send the command
            // again once you approach the limit.  Otherwise data streaming may be left
            // on when your app crashes, putting Sphero in a bad state 
            final int response_count = TOTAL_PACKET_COUNT;

            //Send this command to Sphero to start streaming
            SetDataStreamingCommand.sendCommand(mRobot, divisor, packet_frames, mask, response_count);
        }
    }
    
    
    /**
     * Robot to from which we are streaming
     */
    private Robot mRobot = null;
    
    /**
     * AsyncDataListener that will be assigned to the DeviceMessager, listen for streaming data, and then do the
     */
    private DeviceMessenger.AsyncDataListener mDataListener = new DeviceMessenger.AsyncDataListener() {
        @Override
        public void onDataReceived(DeviceAsyncData data) {

            if(data instanceof DeviceSensorsAsyncData){

                // If we are getting close to packet limit, request more
                mPacketCounter++;
                if( mPacketCounter > (TOTAL_PACKET_COUNT - PACKET_COUNT_THRESHOLD) ) {
                    requestDataStreaming();
                }

			    //get the frames in the response
			    List<DeviceSensorsData> data_list = ((DeviceSensorsAsyncData)data).getAsyncData();
			    if(data_list != null){
			
			        //Iterate over each frame
			        for(DeviceSensorsData datum : data_list){
			
			            //Show attitude data
			            AttitudeData attitude = datum.getAttitudeData();
			            
			            // Get the values of roll and yaw
			            int roll = attitude.getAttitudeSensor().roll;
			            int pitch = attitude.getAttitudeSensor().pitch;
			            int yaw = attitude.getAttitudeSensor().yaw;
			            
					    // Calculate the new image position
					    updateSpheroPosition(roll, pitch, yaw);
					    updateCoin();
					    
					    // Check for end of game state
					    if( (GAME_DURATION-getTimeElapsed()) <= 0 ) {
					    	transitionToEndOfGame();
					    }
					    // Update Time UI
					    mTextTime.setText(CoinCollectorActivity.this.getString(R.string.time)+(GAME_DURATION-getTimeElapsed()));
			        }
			    }
            }
        }
    };
    
    /**
     * Update the Sphero Logo Location
     * @param roll in degrees from data streaming
     * @param pitch in degrees from data streaming
     * @param yaw in degrees from data streaming
     */
    private void updateSpheroPosition(double roll, double pitch, double yaw) {
    	
    	// Find the length of the velocity vector and the angle
	    double length = Math.sqrt(pitch * pitch + roll * roll);
	    double moveAngle = -Math.atan2(-pitch, roll);
	    
	    // Compute the velocity of the Sphero image
	    double adjustedX = length * Math.cos(moveAngle);
	    double adjustedY = length * Math.sin(moveAngle);
	    
	    // Add new distance to the Sphero image
	    mImageSpheroLoc.x += adjustedX;
	    mImageSpheroLoc.y += adjustedY;
	    
	    // Check boundaries
	    if( (mImageSpheroLoc.x + mImageSpheroBounds.x) > mScreenWidth ) {
	    	mImageSpheroLoc.x = mScreenWidth - mImageSpheroBounds.x;
	    }
	    if( (mImageSpheroLoc.y + mImageSpheroBounds.y) > mScreenHeight ) {
	    	mImageSpheroLoc.y = mScreenHeight - mImageSpheroBounds.y;
	    }
	    if( mImageSpheroLoc.x < 0 ) {
	    	mImageSpheroLoc.x = 0;
	    }
	    if( mImageSpheroLoc.y < 0 ) {
	    	mImageSpheroLoc.y = 0;
	    }
	    
	    // Create Sphero translation matrix
	    Matrix matrix = new Matrix();
	    matrix.reset();
	    matrix.postTranslate(mImageSpheroLoc.x, mImageSpheroLoc.y);

	    // Apply translation matrix
	    mImageSphero.setScaleType(ScaleType.MATRIX);
	    mImageSphero.setImageMatrix(matrix);
    }
    
    /**
     * Function that updates the coin rotation and checks if we collected one
     */
    private void updateCoin() {
	    // Create a Sphero Rect object, (Left, Top, Right, Bottom)
	    Rect sphero = new Rect(mImageSpheroLoc.x, mImageSpheroLoc.y, 
	    		mImageSpheroLoc.x + mImageSpheroBounds.x,  mImageSpheroLoc.y + mImageSpheroBounds.y);
	    Rect coin = new Rect(mCoin.getCoinLocation().x,mCoin.getCoinLocation().y,
	    		mCoin.getCoinLocation().x + mCoin.getCoinWidth(), mCoin.getCoinLocation().y + mCoin.getCoinHeight());
	    // Check for coin collision
	    if( sphero.intersect(coin) ) {
	    	mCoinsCollectedCtr++;
	    	// Remove old coin, and add new coin
	    	mLayout.removeViewInLayout(mCoin);
	    	mCoin = new CoinView(this, mScreenWidth, mScreenHeight);
	    	mLayout.addView(mCoin);
	    	// Update Score UI
	    	mTextScore.setText(this.getString(R.string.score)+mCoinsCollectedCtr);
	    }
	    else {
	    	// Slowly rotate coin
	    	mCoin.rotate();
	    }
    }
    
    /**
     * Conveniant function to get elapsed time in seconds
     * @return The Time Elapsed since start in seconds
     */
    private long getTimeElapsed() {
    	return (System.currentTimeMillis()-mStartTime)/1000;
    }
    
    /**
     * Transition to the state where the score is showing,
     * and the game can be started again
     */
    private void transitionToEndOfGame() {
    	// Send this command to Sphero to stop streaming
        SetDataStreamingCommand.sendCommand(mRobot, 0, 0, 0, 0);
        // Show Button again, and keep score visible
        mStartButton.setVisibility(View.VISIBLE);
        mTextTime.setVisibility(View.GONE);
        mImageSphero.setVisibility(View.GONE);
        mCoin.setVisibility(View.GONE);
    }
}
