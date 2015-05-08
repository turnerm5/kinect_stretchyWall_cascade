class KinectTracker {
  
  // Size of kinect depth image
  int kw = 640;
  int kh = 480;
  
  //depth threshold
  int threshold = 765;
  
  //how hard is someone pushing into the screen?
  float force;
  
  //Are we tracking something?
  boolean tracking;

  // location of tracked point
  PVector loc;

  // Depth data
  int[] depth;
  
  //how much does the kinect tilt?
  float deg = 0;

  // how far past the trigger threshold can someone push in?
  int distancePastThreshold = 70;

  //a layer to nicely display our depth data
  PImage display;

  //misalignment correction settings
  int currentMode; //-1 = no correction
  int[] offset = {0,0,0,0};
  String[] mode = {"Top", "Bottom", "Left", "Right"};


  //Construct!

  KinectTracker() {
    
    if (!debugMode){
      kinect.start();
      kinect.enableDepth(true);
      kinect.tilt(deg);
      kinect.processDepthImage(false);
    }
    
    display = createImage(width,height,PConstants.RGB);
    loc = new PVector(0,0);

    //our screen correction variables
    correctionMode = false;
    currentMode = -1;

  }


  //primary functions

  void track() {
    //Main tracking function.
    //Finds closest point past a threshold
    //Returns nothing.
    
    //track where we found the deepest value
    int deepX = 0;
    int deepY = 0;

    // Get the raw depth as array of integers
    depth = kinect.getRawDepth();

    // Being overly cautious here
    if (depth == null) return;

    //reset our closest depth
    int depthMax = 99999;
    
    //default to false, unless we're tracking something
    tracking = false;    

    //for every value in the Kinect depth array.
    for(int x = 0; x < kw; x++) {
      for(int y = 0; y < kh; y++) {
        
        // Mirror the image
        int pixelOffset = kw-x-1+y*kw;
        
        // Grab the raw depth value
        int rawDepth = depth[pixelOffset];
        
        // Test against threshold
        if (rawDepth < threshold) {
          //if we found something, we're tracking!
          tracking = true;
          
          //if it's the closest value, remember it, and its coordinates
          if (rawDepth < depthMax) {
            depthMax = rawDepth;
            deepY = y;
            deepX = x;
          }
        }
      }
    }

    // If we found something...
    if (tracking) {
      
      //correct the location point for the misalignment of the kinect and proj.
      //should we be correcting the whole image, not just the point?
      //this will only work for one point.
      int correctedY = (int)map(deepY, offset[0], offset[1], 0, height);
      int correctedX = (int)map(deepX, offset[2], offset[3], 0, width);
      
      //save the location, corrected to the screen
      loc = new PVector(correctedX,correctedY);    
    }
  }

  void display() {
    
    // Being overly cautious here
    if (!debugMode){
      if (depth == null) return;
    }

    //Load all of the displayed pixels
    display.loadPixels();
    
    for(int x = 0; x < display.width; x++) {
      for(int y = 0; y < display.height; y++) {
        
        //Running through all of the pixels on the big screen and getting 
        //their corresponding locations in the depth array
        int mappedX = (int)map(x,0,display.width,0,kw);
        int mappedY = (int)map(y,0,display.height,0,kh);

        // mirroring image
        int offset = kw-mappedX-1+mappedY*kw;
        
        if (!debugMode){
          // Raw depth
          int rawDepth = depth[offset];

          //What is the index of the pixel array?
          int pix = x + y * display.width;

          if (rawDepth < threshold) {

            int redValue = (int)map(rawDepth, threshold, threshold - distancePastThreshold, 0, 255);
            int greenValue = 0;
            int blueValue = (int)map(rawDepth, threshold, threshold - distancePastThreshold, 255, 0);


            display.pixels[pix] = color(redValue,greenValue,blueValue);

          } else {
            //A dark gray
            display.pixels[pix] = color(100);
          }
        }

        if (debugMode) {
          //What is the index of the pixel array?
          int pix = x + y * display.width;
          display.pixels[pix] = color(100);
        }
      }
    }
    
    //Always update the pixels at the end
    display.updatePixels();

    // Draw the image
    image(display,0,0);
  }


  //remaps the force, so when you push in more, the force returned is greater
  float getForce(){
    
    //what is the range of forces that are allowed?
    int minForce = 200;
    int maxForce = 600;
    
    //remap
    force = constrain(
      map(
        force, 
        0, distancePastThreshold, 
        minForce, maxForce
      ),
      minForce,maxForce
    );
    
    return force;
  }


  //utility functions

  PVector getPos() {
    return loc;
  }


  int getThreshold() {
    return threshold;
  }

  void setThreshold(int t) {
    threshold = t;
  }

  int getCurrentMode(){
    return currentMode;
  }

  void setCurrentMode(int m){
    currentMode = m;
  }

  String getModeName(){
    return mode[currentMode];
  }

  int getOffset(){
    return offset[currentMode];
  }

  void setOffset(int offsetChange){
    offset[currentMode] += offsetChange;
  }

  void quit() {
    kinect.quit();
  }
}
