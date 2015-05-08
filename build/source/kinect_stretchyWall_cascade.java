import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import org.openkinect.*; 
import org.openkinect.processing.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class kinect_stretchyWall_cascade extends PApplet {




//turns off the Kinect sensing, uses the mouse as input
Boolean debugMode = true;

//don't start off in correction mode
Boolean correctionMode = false;

// Showing how we can farm all the kinect stuff out to a separate class
KinectTracker tracker;
// Kinect Library object
Kinect kinect;

//the number of superPixels (things start to slow down quickly if this is increased
int xPixels = 100;
int yPixels = 76;

float xSize, ySize, x, y;
int pixelFill;
int backColor = 0xff9933FF;
boolean gravity = false;

float baseForce = 10;
float force = baseForce;

superPixel[][] pixelArray = new superPixel[xPixels][yPixels];

public void setup() {
  //a little small, so we don't get artifacts
  size(1000, 760);
  noStroke();

  //if we're not in debug mode, initialize the Kinect
  if (!debugMode){
    kinect = new Kinect(this);
  }

  tracker = new KinectTracker();

  //initialize each superPixel, with a nice blueish color
  for (int i = 0; i < xPixels; i++) {
    for (int j = 0; j < yPixels; j++) {

      xSize =  (float)width/xPixels;
      ySize =  (float)height/yPixels;
      x = (float)xSize * (i);
      y = (float)ySize * (j);

      int pixelFill = color(50);

      pixelArray[i][j] = new superPixel(x, y, pixelFill, xSize, ySize);
    }
  }
}

public void draw() {
  
  background(backColor);
  
  //if we're in correction mode
  if (correctionMode){
    //show us what the depth camera is collecting
    tracker.display();

    fill(25);
    text(tracker.getModeName() + " Correction", 10, 20);
    text("Offset: " + tracker.getOffset(), 10, 35);
  }

  if(!debugMode){
    tracker.track();
    if (tracker.tracking){
      float force = tracker.getForce();
      
        if (gravity){
          force = baseForce * 5;
        }
      
      PVector position = tracker.getPos();
      for (int i = 0; i < xPixels; i++) {
        for (int j = 0; j < yPixels; j++) {
          pixelArray[i][j].explode(force, position);
        }
      }
    }
  }  
  
  if (debugMode){
    PVector mouse = new PVector(mouseX, mouseY);
    if (mousePressed && (mouseButton == LEFT)) {
      for (int i = 0; i < xPixels; i++) {
        for (int j = 0; j < yPixels; j++) {
          pixelArray[i][j].explode(force, mouse);
        }
      } 
    }
  }

  for (int i = 0; i < xPixels; i++) {
    for (int j = 0; j < yPixels; j++) {
      pixelArray[i][j].run();
    }
  }


}

public void keyPressed() {
  //if we hit c, toggle between correction mode
  if (key == 'c') {
    int n = tracker.getCurrentMode();
    n += 1;
    tracker.setCurrentMode(n);
    if (n <= 3){
      correctionMode = true;
    }
    if (n > 3) {
      tracker.setCurrentMode(-1);
      correctionMode = false;
    }
  
  }

  if (correctionMode){
    if (key == CODED) {
      if (keyCode == UP || keyCode == RIGHT) {
        tracker.setOffset(1);
      } 
      else if (keyCode == DOWN || keyCode == LEFT) {
        tracker.setOffset(-1);
      }
    }
  }


  //make it easy to adjust our threshold
  if (!debugMode){
    int t = tracker.getThreshold();
    if (key == CODED) {
      if (keyCode == UP) {
        t+=1;
        println("Threshold: "+t);
        tracker.setThreshold(t);
      } 
      else if (keyCode == DOWN) {
        t-=1;
        println("Threshold: "+ t);
        tracker.setThreshold(t);
      }
    }
  }
    
  //make it easy to adjust our force while debugging
  if (debugMode &&! correctionMode){
    if (key == CODED) {
      if (keyCode == UP) {
        force += 50;
        println("force: "+force);
      } 
      else if (keyCode == DOWN) {
        force -= 50;
        println("force: "+force);
      }
    }
  }
}

public void stop() {
  tracker.quit();
  super.stop();
}

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

  public void track() {
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

  public void display() {
    
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
  public float getForce(){
    
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

  public PVector getPos() {
    return loc;
  }


  public int getThreshold() {
    return threshold;
  }

  public void setThreshold(int t) {
    threshold = t;
  }

  public int getCurrentMode(){
    return currentMode;
  }

  public void setCurrentMode(int m){
    currentMode = m;
  }

  public String getModeName(){
    return mode[currentMode];
  }

  public int getOffset(){
    return offset[currentMode];
  }

  public void setOffset(int offsetChange){
    offset[currentMode] += offsetChange;
  }

  public void quit() {
    kinect.quit();
  }
}
//the superPixels are the actual cubes that fly around.

class superPixel {
  PVector origin;
  PVector location;
  PVector velocity;
  PVector acceleration;
  int fillColor;
  float xSize, ySize;
  float topspeed;
  int timer = 0;

  //construct the superPixels!!
  superPixel(float x_, float y_, int fill_, float xSize_, float ySize_) {

    //remember where we started 
    origin = new PVector(x_, y_);
    
    //standard location/velocity/acceleration
    location = new PVector(x_, y_);
    velocity = new PVector();
    acceleration = new PVector();
    
    //how should we fill the square? how big are they
    fillColor = fill_;
    xSize = xSize_;
    ySize = ySize_;
    topspeed = 15;
  }

  //our main function, triggering subroutines
  public void run() {
    timer -= 1;
    gravity();
    checkEdges();
    update();
    display();
    //if the pixel isn't moving, there is no reason to ask it to move home/bounce off the wall/etc.
    if (velocity.mag() != 0 && timer < 1) {
      returnHome();
    }
  }


  // 
  //Dynamic Functions
  //
  
  
  //make the superPixels bounce off the edges. increase the velocity for some nice effects!  
  public void checkEdges() {
    if (location.x < 0) {
      location.x = 0;
      velocity.x *= -.9f;
    } 
    else if (location.x > width) {
      location.x = width;
      velocity.x *= -.9f;
    }

    if (location.y < 0) {
      location.y = 0;
      velocity.y *= -.9f;
    } 
    else if (location.y > height - 2) {
      location.y = height - 2;
      if (gravity){
        velocity.y *= random(-2,-4);
      } else {
        velocity.y *= -.9f;
      }
    }
  }
  
  //return the super pixels to their starting space
  public void returnHome() {
    //make a new vector, using the origin of the superPixel as a starting point
    PVector seek = origin.get();
    
    //the vector now points from the location to the origin of the superPixel
    seek.sub(location);
    
    //how far away are we from the start? useful for the next part
    
    float distance = seek.mag();
    float speed = velocity.mag();
    //the distance test here seems to control the wobble
    
    //if the superPixel is slow and close to it's origin, reset it
    if (distance < .5f && speed < 8) {
      location = origin.get();
      velocity.mult(0);
      acceleration.mult(0);
    }
    //otherwise, move towards home with a random acceleration 
    else {
      seek.normalize();
      seek.mult(random(.3f, .5f));
      acceleration.add(seek);
    }
  }

  //shoot out away from a location
  public void explode(float force, PVector mouse) {
    
    timer = 80;
    //make a new vector, starting at the mouse
    PVector gunpowder = mouse.get();
    
    //the vector now goes between the mouse and the superPixel
    gunpowder.sub(location);
    
    //check the distance between the two
    float distance = gunpowder.mag();
    
     if (distance < (force * 8)){
      gunpowder.normalize();

    
      gunpowder.mult((-1 * force) / (distance));
      applyForce(gunpowder);
     }
  }


  // 
  // Utility Functions
  // 
  
  //basic motion, with some damping to slow everything down
  public void update() {
    velocity.add(acceleration);
    velocity.mult(.96f);
    velocity.limit(topspeed);
    location.add(velocity);
    acceleration.mult(0);
  } 

  //apply some gravity, if it's turned on
  public void gravity() {
    if (gravity) {
      applyForce(new PVector(0,random(.6f,.8f)));
    }
  }
  
  //pass any forces to our object's acceleration
  public void applyForce(PVector force) {
    PVector f = force.get();
    acceleration.add(f);
  }

  //
  //Rendering Functions
  //
  
  //draw the superPixel
  public void display() {
    fill(fillColor);
    rect(location.x, location.y, xSize, ySize);
  }

} 
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "kinect_stretchyWall_cascade" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
