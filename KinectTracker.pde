class KinectTracker {
  // Size of kinect image
  int kw = 640;
  int kh = 480;
  int threshold = 765;
  int depthMax = 0;
  int deepX;
  int deepY;
  float force;
  boolean tracking = false;

  // Raw location
  PVector loc;

  // Interpolated location
  PVector lerpedLoc;

  // Depth data
  int[] depth;

  //how much does the kinect tilt?
  float deg = 0;

  PImage display;

  KinectTracker() {
    kinect.start();
    kinect.enableDepth(true);
    kinect.tilt(deg);

    // We could skip processing the grayscale image for efficiency
    // but this example is just demonstrating everything
    kinect.processDepthImage(true);

    display = createImage(kw,kh,PConstants.RGB);

    loc = new PVector(0,0);
    lerpedLoc = new PVector(0,0);
  }

  void track() {
      
    tracking = false;
    
    // Get the raw depth as array of integers
    depth = kinect.getRawDepth();

    // Being overly cautious here
    if (depth == null) return;

    float sumX = 0;
    float sumY = 0;
    float count = 0;

    depthMax = 99999;
    

    for(int x = 0; x < kw; x++) {
      for(int y = 0; y < kh; y++) {
        // Mirroring the image
        int offset = kw-x-1+y*kw;
        // Grabbing the raw depth
        int rawDepth = depth[offset];
        // Testing against threshold
        if (rawDepth < threshold) {
          tracking = true;
          if (rawDepth < depthMax) {
            depthMax = rawDepth;
            deepY = y;
            deepX = x;
            count += 1;
          }
        }
      }
    }
    // As long as we found something
    if (count != 0) {
      deepY = (int)map(deepY, 30, 370, 0, kh);
      deepX = (int)map(deepX, 140, 550, 0, kw);
      loc = new PVector(deepX,deepY);
    }

    // Interpolating the location, doing it arbitrarily for now
    lerpedLoc.x = PApplet.lerp(lerpedLoc.x, loc.x, 0.3f);
    lerpedLoc.y = PApplet.lerp(lerpedLoc.y, loc.y, 0.3f);
  }

  PVector getLerpedPos() {
    return lerpedLoc;
  }

  PVector getPos() {
    loc.x = map(loc.x,0,kw,0,width);
    loc.y = map(loc.y,0,kh,0,height);
    return loc;
  }

  void display() {
    PImage img = kinect.getDepthImage();

    // Being overly cautious here
    if (depth == null || img == null) return;

    // Going to rewrite the depth image to show which pixels are in threshold
    // A lot of this is redundant, but this is just for demonstration purposes
    display.loadPixels();
    for(int x = 0; x < kw; x++) {
      for(int y = 0; y < kh; y++) {
        // mirroring image
        int offset = kw-x-1+y*kw;
        // Raw depth
        int rawDepth = depth[offset];

        int pix = x+y*display.width;
        if (rawDepth < threshold) {
          // A red color instead
          display.pixels[pix] = color(150,50,50);
        } 
        else {
          display.pixels[pix] = img.pixels[offset];
        }
      }
    }
    display.updatePixels();

    // Draw the image
    image(display,0,0);
  }

  void quit() {
    kinect.quit();
  }

  int getThreshold() {
    return threshold;
  }

  void setThreshold(int t) {
    threshold =  t;
  }


  float getForce(){
    
    //we need to determine what the second number should be.
    float minForce = 5;
    int maxForce = 50;
    int distancePastThreshold = 75;

    force = constrain(map(force, 0, distancePastThreshold, minForce, maxForce),minForce,maxForce);
    return force;
  }

  boolean tracking(){
    return tracking;
  }

}
