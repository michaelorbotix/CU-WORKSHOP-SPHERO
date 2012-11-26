![logo](http://update.orbotix.com/developer/sphero-small.png)

# Coin Collector Game TutorialCoin Collector is a simple game in which you use Sphero as a controller to move the logo around the screen and collect as many coins as you can in one minute.  To control the logo, we use asynchronous data streaming of the IMU values of **Roll and Pitch**.
## Starting from the StreamingExample Sample
Our SDK comes with a bunch of samples you can import to avoid redundant coding when starting a new app.  In this case, we are going to start with the `StreamingExample` sample, because it is already set up to stream data from Sphero to your Android device.
To import the SensorStreaming project into Eclipse, on the menu go to `"File->Import"`, select `General->Existing Project into Workspace` and navigate to the `samples` directory where you downloaded the Sphero-Android-SDK.  Select the `StreamingExample` directory and hit finish.## Refactoring/Renaming the Sample
We don't want our game to be called StreamingExample, we want it to be called CoinCollector.  It's easiest to do this refactoring at the beginning of a project before adding any classes. 

Right click the `StreamingExample` directory and click `"Refactor->Rename…"` and call it **CoinCollector**.  

Also Rename `StreamingActivity.java` to `CoinCollectorActivity.java`If you are feeling ambitious, you can also rename the package `com.orbotix.streamingexample` to `com.cu.coincollector` or something like that.  However, if you do this, you now have to change the package name in `AndroidManifest.xml` to `com.cu.coincollector`.  Then delete the line `import com.orbotix.streamingexample.R;` in all files (it will have a red x next to it now) and save the files.
At this point, you should run the app on a device, connect a Sphero, and see if it still works as expected after the changes.  
## Adding the UI Features
There are many game design methodologies, but one I would like to share is called front-end first.  The logic behind it is, if you make the UI first, then you will know exactly what to code on the back-end.

### Adding Resources
So let's begin by adding the resources we are going to need for our game.  This includes the Sphero image logo, the coin image, and a few hard coded Strings.  Copy the `/res/drawable` directory from the **completed CoinCollector** project and past it into the `/res` directory of your project.  The drawable directory is where you store all images that your project uses.  
Also, we want to add these Strings into the `/res/values/strings.xml` file.
	<string name="start">Start Game</string>
    <string name="sphero_image">Sphero Image</string>
    <string name="score">Score:</string>
    <string name="time">Time:</string>
    
At this point, you can change the `app_name` string to **CoinCollector**

### Adding UI Elements

Replace the nesting RelativeLayout in `main.xml` with this new layout that contains two TextViews, a Button, and an ImageView.

	  <RelativeLayout
        android:id="@+id/relative_layout"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent" >

      <TextView
          android:id="@+id/text_score"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_alignParentTop="true"
          android:layout_alignParentLeft="true"
          android:text="@string/score"
          android:textColor="#FFF"
          android:visibility="invisible"/>  
          
      <TextView
          android:id="@+id/text_time"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_alignParentTop="true"
          android:layout_alignParentRight="true"
          android:text="@string/time"
          android:textColor="#FFF"
          android:visibility="invisible"/> 
        
      <Button 
          android:id="@+id/button_start"
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:layout_centerHorizontal="true"
          android:layout_centerVertical="true"
          android:text="@string/start"
          android:onClick="onStartPressed"/>
      
      <ImageView
          android:id="@+id/image_sphero"
          android:layout_width="fill_parent"
          android:layout_height="fill_parent"
          android:src="@drawable/sphero_ball"
          android:scaleType="matrix"
          android:contentDescription="@string/sphero_image"
          android:visibility="invisible"/>

    </RelativeLayout>

The TextViews will display game statistics while you are playing (the score and time remaing) and we place them at the top left and top right of the screen.  The button is simple, and will be placed in the middle of the screen (note: the onStartPressed function) since it starts a round of the game.  The ImageView is a little bit more advanced, because of the `matrix` scale type.  This is telling Android that we will control the scale and rotation of the image ourselves using matrix transformations.  The only View that is initially visible is the Start Button. 

Now, delete these lines in `CoinCollectorActivity.java`, since we no longer need them and they cause an error:

        mImuView = (ImuView)findViewById(R.id.imu_values);
        mAccelerometerFilteredView = (CoordinateView)findViewById(R.id.accelerometer_filtered_coordinates);
        
Also, remove the call to `requestDataStreaming()` in the `onCreate()` method, otherwise there will be a crash.
        
Now when we run the app, we should see a single Start button in the middle of the screen.  If you see this, continue with the tutorial.

### Making the Game Fullscreen

In the `AndroidManifest.xml` replace the <Activity> tag for our CoinCollectorAcitivty with this:

	<activity android:name=".CoinCollectorActivity"
                  android:label="@string/app_name"
                  android:theme="@android:style/Theme.Black.NoTitleBar.Fullscreen">
                  
We simply added the theme attribute to make the game full screen (remove the titlebar).### Using the UI in Code
To access UI elements in code on Android, you must access then with the `findViewById(int id)` and store it in a variable.  
Define the View Objects at the top the `CoinCollectorActivity.java` class under the other View variables:
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
      * Start Button View
      */
     private Button mStartButton;
     /**
     * Sphero Image that collects coins
     */
    private ImageView mImageSphero;
    
Now, we grab the from the XML view with this code.  Place under the `setContentView(R.layout.main)` in the `onCreate()`:

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
        
## Code

These are the variables we need, add them to the `CoinCollectorActivity.java` class under the View variables:

	private Point mImageSpheroLoc; 		 		   // The X and Y position of Sphero image (pixels)
    private Point mImageSpheroBounds;			   // The width and height of Sphero image (pixels)
    private int mScreenWidth;					   // Phone Screen Width (pixels)
    private int mScreenHeight;					   // Phone Screen Height (pixels) 
    private long mStartTime;					   // The Time in milliseconds at start of game round 
    private final static int GAME_DURATION = 60; // length of a round in seconds
    
### Getting the Android Device Screen Size

Most Android devices have different screen sizes.  To get the correct width and height of the screen (in pixels) we use the `DisplayMetrics` class.  We will use this width and height to bound the Sphero image's movement, and also to bound the random location of the coins that will pop up later in the tutorial.  Add this code in the `onCreate()` method.

        // Get Screen width and height
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mScreenHeight = displaymetrics.heightPixels;
        mScreenWidth = displaymetrics.widthPixels;
        
### Starting the Game

Before, we added a start button to the UI, so now it is time to implement the code when it is pressed.  We used XML to register the onStartPressed(View v) callback with the button, so now we just create the function:

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
    }
    
The comments in the code explain most of what is going on here.  We are simply initializing the views, score, and time to be the start of a round of the game.

### Moving Sphero LogoThe algorithm that moves the logo around, using the Sphero ball as a controller, is simple.  However, you need to know about the orientation units of **roll and pitch** before the algorithm will make sense.  Here is an image that will help you understand these values which are in degrees.![android.jpg](https://github.com/orbotix/Sphero-Android-SDK/raw/master/assets/IMU.png)
The algorithm treats the roll value as the x-axis velocity and the pitch value as y-axis velocity and updates the image x and y position based on these values.  The function to update the sphero motion is below:
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
    
### Calling the updateSpheroPosition Function

The `StreamingExample` set up the data for us, so all we need to do is get it to the function we created.  We do this by replacing the code in `onDataReceived(DeviceAsyncData data)` to 
 
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
        }
    }
    
## Adding End of Game State

At this point, the game has no ending.  However, you should run the app, and test it out.  At this point, you should be able to press the start button and control the on screen logo using a tilt and twist of the Sphero ball.

To add an end of game state, we need to reset the visibility of the views after a certain time period.  The time check should take place in the `onDataReceived(DeviceAsyncData data)` and it should call the `transisitionToEndOfGame()` function:

(In `onDataReceived(DeviceAsyncData data)`):

    // Calculate the new image position
    updateSpheroPosition(roll, pitch, yaw);
    
    // Check for end of game state
    if( (GAME_DURATION-getTimeElapsed()) <= 0 ) {
    	transitionToEndOfGame();
    }
    // Update Time UI
    mTextTime.setText(CoinCollector.this.getString(R.string.time)+(GAME_DURATION-getTimeElapsed()));
    
Below is a useful function you must include to get elapsed time:
    
    /**
     * Conveniant function to get elapsed time in seconds
     * @return The Time Elapsed since start in seconds
     */
    private long getTimeElapsed() {
    	return (System.currentTimeMillis()-mStartTime)/1000;
    }
    
Transition function:    
    
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
    }
    
## Adding the Coins

The last step of creating the game is adding the Coin Objects to collect in 60 seconds.  This the hardest part of the tutorial, so I saved it for last.  It involves creating a custom ImageView in code and performing rectangular collision detection on the logo and the coin.

### The CoinView Class

To create a new class, right click on the package we renamed before, and click `New->Class`.  Name the class `CoinView` and have the superclass be `android.widget.ImageView`. The most important part of this class is its constructor.

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
	
This dynamically creates the same ImageView we created before in XML, but this one is created at runtime.    The interesting part about this, is we can manage new Coins with random location and rotation velocities programatically.

Other functions that are needed in the class, are below:

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
	
## USing the CoinView

We are going to need to keep track of the Coin that's on the screen and also the amount of Coins that were collected.  Add these variables to the `CoinCollectorActivity.java` class.

	/**
     * Coin View
     */
    private CoinView mCoin;
    private int mCoinsCollectedCtr;
    
We are going to initialize the CoinView when the start button is clicked, and add it to the RelativeLayout.  Add this code to the `onStartPressed(View v)` function.

    // Create new Coin View
    mCoin = new CoinView(this, mScreenWidth, mScreenHeight);
    
    // Add coin view to our layout
    mLayout.addView(mCoin);
    
Now that we have a Coin displayed on the screen, we need to do collision detection to determine if the logo has intersected with a coin.  We will use the `Rect` class that does collision detection for us.  The collision detection algorithm is based on rectangles, and both these objects are circular, so it will not be perfect, but good enough for the tutorial.

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
    
If the logo and the coin intersect, then we remove the old coin from the layout, create a new one, add that to the layout, and incrament the coins collected counter.

The last step is to call the `updateCount()` function from within the `mDataListener` object.  Put it below the `updateSpheroPostition(roll, pitch, yaw)` function:

    // Calculate the new image position
    updateSpheroPosition(roll, pitch, yaw);
    updateCoin();
    
## Conclusion

At this point, if you run the app, you will be able to play rounds of **CoinColletor** and try and collect as many coins as you can in 60 seconds.  

Congratulations!  You have made your first mini-game with Sphero.  Hopefully this was an informative enough tutorial to be able to take this game to the next level by adding settings, multiple coins, a help menu, or whatever your creative mind can come up with. ## Questions

For questions, please visit our developer's forum at [http://forum.gosphero.com/](http://forum.gosphero.com/) or email me at michael@orbotix.com

	  