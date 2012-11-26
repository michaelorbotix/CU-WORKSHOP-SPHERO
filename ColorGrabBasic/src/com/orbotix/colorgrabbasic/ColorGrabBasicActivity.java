package com.orbotix.colorgrabbasic;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import orbotix.macro.BackLED;
import orbotix.macro.Delay;
import orbotix.macro.Fade;
import orbotix.macro.LoopEnd;
import orbotix.macro.LoopStart;
import orbotix.macro.MacroObject;
import orbotix.macro.MacroObject.MacroObjectMode;
import orbotix.macro.RGB;
import orbotix.macro.RotateOverTime;
import orbotix.robot.base.*;
import orbotix.robot.sensor.AccelerometerData;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.view.connection.SpheroConnectionView.OnRobotConnectionEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ColorGrabBasicActivity extends Activity
{
    /**
     * Sphero Connection Activity
     */
    private SpheroConnectionView mSpheroConnectionView;

    /**
     * Data Streaming Packet Counts
     */
    private final static int TOTAL_PACKET_COUNT = 200;
    private final static int PACKET_COUNT_THRESHOLD = 50;
    private int mPacketCounter;

    /**
     * Robot to from which we are streaming
     */
    private Robot mRobot = null;
    
    /**
     * Layout to change background color
     */
    private RelativeLayout mLayout;
    
    /**
     * Layout to change background color
     */
    private TextView mTextGameStatus;

    /**
     * Starting Colors
     * Red, Blue, Orange, Purple, Green, Yellow, White
     */
    private final int[] RAINBOW_COLORS_RED =     {255,    0,  255,    255,      0,    255,   255};
    private final int[] RAINBOW_COLORS_GREEN =   {  0,    0,  127,      0,    255,    255,   255};
    private final int[] RAINBOW_COLORS_BLUE =    {  0,  255,    0,    255,      0,      0,   255};
    
    private int mColorToGrab;      		// The Color the user has to grab
    private boolean mGameOn = false;    // Flag to tell if the color grab macro is running
    
    /**
     * Time that the macro was started and sent
     */
    private long mStartTime;
    /**
     * The List of Colors to display in one iteration of color grab
     */
    private ArrayList<Integer> mColorList;
    /**
     * The colors and times they appear so we can tell which color the player grabbed
     */
    private ArrayList<ColorTime> mColorTimes;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    
	    // Set Relative Layout
	    mLayout = (RelativeLayout)findViewById(R.id.relative_layout);
	    // Set Text View
	    mTextGameStatus = (TextView)findViewById(R.id.text_game_status);
        
        // Initialize Color List by adding the values from 0 to length of color list
        mColorList = new ArrayList<Integer>();
        for( int i = 0; i < RAINBOW_COLORS_BLUE.length; i++ ) {
        	mColorList.add(new Integer(i));
        }
        
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
				
                // register the async data listener
                DeviceMessenger.getInstance().addAsyncDataListener(mRobot, mDataListener);
			}
			
			@Override
			public void onBluetoothNotEnabled() {
				// See ButtonDrive Sample on how to show BT settings screen, for now just notify user
				Toast.makeText(ColorGrabBasicActivity.this, "Bluetooth Not Enabled", Toast.LENGTH_LONG).show();
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

    private void requestDataStreaming() {

        if(mRobot != null){

            // Set up a bitmask containing the sensor information we want to stream
            final long mask = SetDataStreamingCommand.DATA_STREAMING_MASK_ACCELEROMETER_Z_FILTERED;

            // Specify a divisor. The frequency of responses that will be sent is 400hz divided by this divisor.
            final int divisor = 20;

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

                	if( !mGameOn ) return;
                	
                    //Iterate over each frame
                    for(DeviceSensorsData datum : data_list){
                    	
                        AccelerometerData accel = datum.getAccelerometerData();
                        // Check for a "Grab" motion or large value in gravity up or down
                        if( Math.abs(accel.getFilteredAcceleration().z) >= 2.0 ) {
                        	
                        	// Stop animation and data streaming
                        	AbortMacroCommand.sendCommand(mRobot);
                        	SetDataStreamingCommand.sendCommand(mRobot, 0, 0, 0, 0);
                        	mGameOn = false;
                        	
                        	// Get the time difference between the time grabbed
                        	long timeDiff = System.currentTimeMillis()-mStartTime;
                        	
                        	// Search for which color was the grabbed one
                        	for( int i = 1; i < mColorTimes.size(); i++ ) {
                        		// Check if we grabbed it in between these two colors
                        		if( timeDiff >= mColorTimes.get(i-1).getColorTime() && 
                        		    timeDiff <= mColorTimes.get(i).getColorTime() ) {
                        			// Check if this was the color we were supposed to pick
                        			if( mColorTimes.get(i-1).getColorIndex() == mColorToGrab ) {
                        				sendVictoryMacro();
                        				mTextGameStatus.setText(getString(R.string.won)+"\n"+getString(R.string.points)+(20000-mColorTimes.get(i).getColorTime()));
                        			}
                        			else {
                        				sendLossMacro(mColorTimes.get(i-1).getColorIndex());
                        				mTextGameStatus.setText(getString(R.string.lost));
                        			}
                        		}
                        	}
                        }
                    }
                }
            }
        }
    };
    
    /**
     * Called when the start game button is pressed
     * @param v
     */
    public void onStartPressed(View v) {
        // Start streaming data
        requestDataStreaming();
        // Start Color grab game
    	sendColorGrabMacro();
    	// Start Game
    	mGameOn = true;
    	
    	mTextGameStatus.setText(getString(R.string.pick));
    }
    
    /**
     * Send the macro that changes colors and spins the ball
     */
    private void sendColorGrabMacro() {
    	mColorTimes = new ArrayList<ColorTime>();
    	
    	// Choose color goal and set background to that color
    	mColorToGrab = (new Random()).nextInt(RAINBOW_COLORS_BLUE.length);
    	mLayout.setBackgroundColor(getHexColorFromRainbowIndex(mColorToGrab));
    	
    	// Create the macro object
    	MacroObject macro = new MacroObject();
    	macro.setMode(MacroObjectMode.Normal);
    	macro.addCommand(new BackLED(100,0));
    	
    	int timeCtr = 0;
    	int frequency = 500;  // Start at 400 ms
    	
    	// Spin for the length of the colors
    	macro.addCommand(new RotateOverTime(20000, frequency*RAINBOW_COLORS_BLUE.length*8));
    	
    	for( int i = 0; i < 4; i++ ) {
	    	Collections.shuffle(mColorList);   // Built-in function to shuffle colors for us
	    	// Loop through and add colors
	    	for( Integer index: mColorList ) {
	    		mColorTimes.add(new ColorTime(index.intValue(),timeCtr));
	    		macro.addCommand(new RGB(RAINBOW_COLORS_RED[index.intValue()],
	    								 RAINBOW_COLORS_GREEN[index.intValue()],
	    								 RAINBOW_COLORS_BLUE[index.intValue()], 0));
	    		macro.addCommand(new Delay(frequency));
	    		timeCtr += frequency;
	    	}
	    	frequency += 250;  // Add 100 ms
    	}
    	
    	// Set Robot and play macro
    	macro.setRobot(mRobot);
    	macro.playMacro();
    	
    	// Set Start Time
    	mStartTime = System.currentTimeMillis();
    }
    
    /**
     * Send the macro animation when a player grabbed the right color
     */
    private void sendVictoryMacro() {
    	// Create the macro object
    	MacroObject macro = new MacroObject();
    	macro.setMode(MacroObjectMode.Normal);
    	
    	// Loop Fading 10 times
    	macro.addCommand(new LoopStart(10));
    	// Fade to winning color
    	macro.addCommand(new Fade(RAINBOW_COLORS_RED[mColorToGrab],
								 RAINBOW_COLORS_GREEN[mColorToGrab],
								 RAINBOW_COLORS_BLUE[mColorToGrab], 200));
    	// 200 ms delay
    	macro.addCommand(new Delay(200)); 
    	// Fade to black
    	macro.addCommand(new Fade(0,0,0, 200));
    	macro.addCommand(new Delay(200)); 
    	macro.addCommand(new LoopEnd());
    	
    	// Set Robot and play macro
    	macro.setRobot(mRobot);
    	macro.playMacro();
    }
    
    /**
     * Send the macro when a player grabbed the wrong color
     */
    private void sendLossMacro(int colorIndex) {
    	// Create the macro object
    	MacroObject macro = new MacroObject();
    	macro.setMode(MacroObjectMode.Normal);
    	
    	// Loop Flashing 10 times
    	macro.addCommand(new LoopStart(10));
    	// Change to the color the player actually grabbed
    	macro.addCommand(new RGB(RAINBOW_COLORS_RED[colorIndex],
								 RAINBOW_COLORS_GREEN[colorIndex],
								 RAINBOW_COLORS_BLUE[colorIndex], 0));
    	// 200 ms delay
    	macro.addCommand(new Delay(200)); 
    	// Change to red color
    	macro.addCommand(new RGB(RAINBOW_COLORS_RED[0],
								 RAINBOW_COLORS_GREEN[0],
								 RAINBOW_COLORS_BLUE[0], 0));
    	macro.addCommand(new Delay(200));
    	macro.addCommand(new LoopEnd());
    	
    	// Set Robot and play macro
    	macro.setRobot(mRobot);
    	macro.playMacro();
    }
    
    /**
     * Performs a bitwise operand to create a color based on R,G,B values
     * @param index of rainbow colors
     * @return hex color value
     */
    private int getHexColorFromRainbowIndex(int index) {
    	int color = 0;
    	color += RAINBOW_COLORS_BLUE[index];
    	color += RAINBOW_COLORS_GREEN[index] << 8;
    	color += RAINBOW_COLORS_RED[index] << 16;
    	color += 0xFF << 24;
    	return color;
    }
}
