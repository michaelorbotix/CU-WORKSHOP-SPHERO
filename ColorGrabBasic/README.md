![logo](http://update.orbotix.com/developer/sphero-small.png)

# Color Grab Basic TutorialColor Grab Basic is a simple game in which you use Sphero in a multiplayer table-top game to grab the Sphero when its color matches the one showing on the screen.  To make the Sphero animate, we use commands called Macros and we use a simple time-based algorithm to determine which color was chosen.## Starting from the StreamingExample Sample
Our SDK comes with a bunch of samples you can import to avoid redundant coding when starting a new app.  In this case, we are going to start with the `StreamingExample` sample, because it is already set up to stream data from Sphero to your Android device.
To import the SensorStreaming project into Eclipse, on the menu go to `"File->Import"`, select `General->Existing Project into Workspace` and navigate to the `samples` directory where you downloaded the Sphero-Android-SDK.  Select the `StreamingExample` directory and hit finish.## Refactoring/Renaming the Sample
We don't want our game to be called StreamingExample, we want it to be called CoinCollector.  It's easiest to do this refactoring at the beginning of a project before adding any classes. 

Right click the `StreamingExample` directory and click `"Refactor->Rename…"` and call it **ColorGrabBasic**.  

Also Rename `StreamingActivity.java` to `ColorGrabBasicActivity.java`If you are feeling ambitious, you can also rename the package `com.orbotix.streamingexample` to `com.orbotix.colorgrabbasic` or something like that.  However, if you do this, you now have to change the package name in `AndroidManifest.xml` to `com.orbotix.colorgrabbasic`.  Then delete the line `import com.orbotix.colorgrabbasic.R;` in all files (it will have a red x next to it now) and save the files.  Which will automatically rebuild the resource ( R ) file for you.
At this point, you should run the app on a device, connect a Sphero, and see if it still works as expected after the changes.  
## Adding the UI Features
There are many game design methodologies, but one I would like to share is called front-end first.  The logic behind it is, if you make the UI first, then you will know exactly what to code on the back-end.

### Adding Resources
So let's begin by adding the resources we are going to need for our game. We want to add these Strings into the `/res/values/strings.xml` file.  
    <string name="start">Spin</string>
    <string name="lost">You Lost:</string>
    <string name="won">You Won:</string>
    <string name="points">Points:</string>
    <string name="pick">Pick Background Color</string>
    
This is standard practice for Android apps, to avoid hard-coding String values that are used in the UI.  At this point, you can also change the `app_name` string to **ColorGrabBasic**

### Adding UI Elements

Replace the nesting RelativeLayout in `main.xml` with this new layout that contains two TextViews, a Button, and an ImageView.

    <RelativeLayout
        android:id="@+id/relative_layout"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent" 
        android:background="#FFF">

        <Button
            android:id="@+id/button_start"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerHorizontal="true"
            android:layout_alignParentBottom="true"
            android:onClick="onStartPressed"
            android:text="@string/start" />
        
        <TextView
            android:id="@+id/text_game_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerHorizontal="true"
            android:layout_centerVertical="true"
            android:textSize="20sp"
            android:textColor="#000"
            android:text="Spin"/>
        
    </RelativeLayout>

The TextView will display messages to the user while you are playing (i.e. the score) and we place it in the middle of the screen.  The button is simple, and will be placed at the bottom center of the screen (note: the onStartPressed function) since it starts a round of the game.
        
Now when we run the app, we should see a single Start button in the bottom center of the screen, and the word "Spin" in the center of the screen.  If you see this, continue with the tutorial.

### Making the Game Fullscreen

In the `AndroidManifest.xml` replace the <Activity> tag for our CoinCollectorAcitivty with this:

	<activity android:name=".ColorGrabBasicActivity"
                  android:label="@string/app_name"
                  android:theme="@android:style/Theme.Black.NoTitleBar.Fullscreen">
                  
We simply added the theme attribute to make the game full screen (remove the titlebar).### Using the UI in Code
To access UI elements in code on Android, you must access then with the `findViewById(int id)` and store it in a variable.  
Define the View Objects at the top the `ColorGrabBasicActivity.java` class under the other View variables:    /**
     * Layout to change background color
     */
    private RelativeLayout mLayout;
    /**
     * Layout to change background color
     */
    private TextView mTextGameStatus;
    
Now, we grab the from the XML view with this code.  Place under the `setContentView(R.layout.main)` in the `onCreate()`:

    setContentView(R.layout.main);
    
    // Set Relative Layout
    mLayout = (RelativeLayout)findViewById(R.id.relative_layout);
    // Set Text View
    mTextGameStatus = (TextView)findViewById(R.id.text_game_status);
        
## Code

These are the variables we need, add them to the `ColorGrabBasicActivity.java` class under the View variables:

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
    
We are also going to be using our own custom class variable, which is a container for an int and a long variable.  Define it as well:

    /**
     * Private class to store the color and time at which that color occurs
     * This makes our reverse lookup of the color easier
     */
    private class ColorTime {
    	
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

        
### Starting the Game

Before, we added a start button to the UI, so now it is time to implement the code when it is pressed.  We used XML to register the `onStartPressed(View v)` callback with the button, so now we just create the function:

    /**
     * Called when the start game button is pressed
     * @param v
     */
    public void onStartPressed(View v) {
    
        // Initialize Color List by adding the values from 0 to length of color list
    	mColorList = new ArrayList<Integer>();
    	for( int i = 0; i < RAINBOW_COLORS_BLUE.length; i++ ) {
        	mColorList.add(new Integer(i));
    	}
    
        // Start streaming data
        requestDataStreaming();
        // Start Color grab game
    	sendColorGrabMacro();
    	// Start Game
    	mGameOn = true;
    	
    	mTextGameStatus.setText(getString(R.string.pick));
    }
    
The comments in the code explain most of what is going on here.  Note, we have not created the `sendColorGrabMacro()` function yet, so there will be a syntax error.  This is the next step.

## Macros

Macros were created by our firmware team and were intended as a way to automate and accurately reproduce actions and behaviors, with both high and low client interaction.  In this case, the benefit is we can create a set of spins and color transitions, and send one command that runs this behavior over 10 or so seconds.  

## Creating Macros

There are 5 main steps to running a macro on a Sphero.
	
1. To create a macro 

		MacroObject macro = new MacroObject();      

2. Add commands to the object

		macro.addCommand(…);
		
3. Set the Macro transmission mode.  Almost always use Normal, unless your Macro is longer than 254 bytes, in which case, use Chunky

		macro.setMode(MacroObject.MacroObjectMode.Normal);

4. Set the Robot to transmit the macro to

		macro.setRobot(mRobot);

5. Lastly, play the macro

		macro.playMacro(); 
		
### The Color Grab Macro

The goal of this macro is to make Sphero spin around, transition between a set of colors, and slowly increase the time between color transitions over time.  We also need to create the system in a way that when the user grabs the Sphero, we know which color the macro was on at that particular time.

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
    	
    	// Spin for the length of the colors 20 seconds
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
    
This function may seem daunting if you have never used a Sphero before.  It creates a macro object, and adds a set of commands to it.  We add the commands to turn on the Back LED with     `macro.addCommand(new BackLED(100,0))` and then make it spin 20,000 degrees over about 20 seconds.  The inside of the for loop builds together 4 different random sets of colors from the hard-coded RGB values we defined at the beginning.  These RGB values get put in a certain order, and then each one is added to a list for future access.

The macro is then sent to the Sphero with `macro.playMacro()` and we save the time we sent it for future use.

We will also need a helper function for this to work called `getHexColorFromRainbowIndex(int index)` which converts a RGB value to the format of a single int variable with (Alpha,Red,Green,Blue) format.

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

## Detecting User Grabs

The Sphero SDK allows you to stream data from the ball's sensors.  In this case we want to detect the user grabbing the ball.  We will use a sensor called a 3-axis accelorometer.  If you never worked with an accelerometer before, this image should help you understand the acceleration vectors it describes:

![android.jpg](https://github.com/orbotix/Sphero-Android-SDK/raw/master/assets/accelerometer.png)

We will be looking at the positive z-axis and determining if the value ever goes over a certain threshold, or in this case 2Gs (G=9.81 m/s^2).

### Data Streaming Code

Inside the `onDataReceived(DeviceAsyncData data)` function there is a section where we get the accelerometer values, and print them to the screen.  In our case, we want to just check if the current value of the z-axis of the accelerometer is greater than 2.0 or less than -2.0. We do this below with the "if statement".

	 //get the frames in the response
    List<DeviceSensorsData> data_list = ((DeviceSensorsAsyncData)data).getAsyncData();
    if(data_list != null){

    	if( !mGameOn ) return;
    	
        //Iterate over each frame
        for(DeviceSensorsData datum : data_list){
        	
            AccelerometerData accel = datum.getAccelerometerData();
            // Check for a "Grab" motion or large value in gravity up or down
            if( Math.abs(accel.getFilteredAcceleration().z) >= 2.0 ) {
            	
            	            	}
            }
        }
    }
    
### End Game State

The color grab is our end game state.  Once we detect this gesture, we want to determine which color the user grabbed.  To do this, we need to first get the time duration that happened since we sent the macro.  We do a simple calculation of `timeDiff = System.currentTimeMillis()-mStartTime`.  Now that we have the time difference, we can loop through the ArrayList of ColorTime objects that we added so we can find which color we were on at the time.  The code below performs this algorithm and either sends a win macro and computes a score, or shows a losing macro.

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
    
### Victory Macro

The Victory macro will fade the winning color in and out 10 times over a period of time.  The code is below: 

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
    
### Losing Macro

The losing macro takes the losing color as a parameter and strobes back and forth between the losing color and red.  The creates a warning effect that is good for conveying a loss.  The code is below:

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
    
## Conclusion

At this point, if you run the app, you will be able to play rounds of **ColorGrabBasic** and to score as many high scores as you can.  

Congratulations!  You have made your first mini-game with Sphero.  Hopefully this was an informative enough tutorial to be able to take this game to the next level by adding settings, sound effects, a help menu, or whatever your creative mind can come up with. ## Questions

For questions, please visit our developer's forum at [http://forum.gosphero.com/](http://forum.gosphero.com/) or email me at michael@orbotix.com

	  