import org.openkinect.*;
import org.openkinect.processing.*;

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
color pixelFill;
color backColor = #9933FF;
boolean gravity = false;

float baseForce = 10;
float force = baseForce;

superPixel[][] pixelArray = new superPixel[xPixels][yPixels];

void setup() {
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

      color pixelFill = color(50);

      pixelArray[i][j] = new superPixel(x, y, pixelFill, xSize, ySize);
    }
  }
}

void draw() {
  
  background(backColor);
  

  
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
    if (tracker.tracking()){
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
  


}

void keyPressed() {
  
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
