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
Boolean debugMode = false;

// Showing how we can farm all the kinect stuff out to a separate class
KinectTracker tracker;
// Kinect Library object
Kinect kinect;

//the number of superPixels (things start to slow down quickly if this is increased
int xPixels = 100;
int yPixels = 76;

float xSize, ySize, x, y;
int pixelFill;
int backColor = 0xffCCEAE3;
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
    tracker = new KinectTracker();
  }

  //code this
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
  
  if (gravity){
    force = baseForce * 5;
  }

  for (int i = 0; i < xPixels; i++) {
    for (int j = 0; j < yPixels; j++) {
      pixelArray[i][j].run();
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

  if(!debugMode){
    tracker.track();
    float force = tracker.getForce();
    
    if (tracking){
      PVector position = tracker.getPos();

      for (int i = 0; i < xPixels; i++) {
        for (int j = 0; j < yPixels; j++) {
          pixelArray[i][j].explode(force, position);
        }
      }
    }
  }  
}

public void keyPressed() {
  
  if (!debugMode){
    
    //make it easy to adjust our threshold
    int t = tracker.getThreshold();
    if (key == CODED) {
      if (keyCode == UP) {
        t+=5;
        tracker.setThreshold(t);
      } 
      else if (keyCode == DOWN) {
        t-=5;
        tracker.setThreshold(t);
      }
    }
  }
  
  //if we hit space, change the gravity!
  if (key == ' ') {
    gravity = !gravity;
    if (!gravity){
      force = baseForce;
    }
    println("gravity: "+gravity);
  }

   
  //make it easy to adjust our force while debugging
  if (debugMode){
    if (key == CODED) {
      if (keyCode == UP) {
        baseForce += 1;
        force += 1;
        println("force: "+force);
      } 
      else if (keyCode == DOWN) {
        baseForce -= 1;
        force -= 1;
        println("force: "+force);
      }
    }
  }

}
class KinectTracker {



  // Size of kinect image
  int kw = 640;
  int kh = 480;
  int threshold = 765;
  int depthMax = 0;
  int deepX;
  int deepY;
  float force;
  boolean tracking;

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

  public void track() {

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

  public PVector getLerpedPos() {
    return lerpedLoc;
  }

  public PVector getPos() {
    loc.x = map(loc.x,0,kw,0,width);
    loc.y = map(loc.y,0,kh,0,height);
    return loc;
  }

  public void display() {
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

  public void quit() {
    kinect.quit();
  }

  public int getThreshold() {
    return threshold;
  }

  public void setThreshold(int t) {
    threshold =  t;
  }


  public float getForce(){
    
    //we need to determine what the second number should be.
    int minForce = 50;
    int maxForce = 300;
    int distancePastThreshold = 100;

    force = constrain(map(force, 0, distancePastThreshold, minForce, maxForce),minForce,maxForce);
    return force;
  }

  public boolean tracking(){
    return tracking;
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
    topspeed = 9;
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
      seek.mult(random(0, .1f));
      acceleration.add(seek);
    }
  }

  //shoot out away from a location
  public void explode(float force, PVector mouse) {
    //make a new vector, starting at the mouse
    PVector gunpowder = mouse.get();
    
    //the vector now goes between the mouse and the superPixel
    gunpowder.sub(location);
    
    //check the distance between the two
    float distance = gunpowder.mag();
    
    // if (distance < (force * 8)){
      gunpowder.normalize();
      timer = 80;
    
      gunpowder.mult((-1 * force) / (distance));
      applyForce(gunpowder);
    // }
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