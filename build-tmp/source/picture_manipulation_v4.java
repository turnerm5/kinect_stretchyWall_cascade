import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class picture_manipulation_v4 extends PApplet {

//the number of superPixels (things start to slow down quickly if this is increased
int xPixels = 200;
int yPixels = 100;


float xSize, ySize, x, y;
int pixelFill;
float isMoving;
int backColor;
boolean gravity = false;
boolean mouseLocked = false;

superPixel[][] pixelArray = new superPixel[xPixels][yPixels];

public void setup() {
  size(800, 400);
  noStroke();

  //code this
  //initialize each superPixel, with a nice blueish color
  for (int i = 0; i < xPixels; i++) {
    for (int j = 0; j < yPixels; j++) {

      xSize =  (float)width/xPixels;
      ySize =  (float)height/yPixels;
      x = (float)xSize * (i);
      y = (float)ySize * (j);

      int pixelFill = color(40);

      pixelArray[i][j] = new superPixel(x, y, pixelFill, xSize, ySize);
    }
  }
}

public void draw() {
  fill(80);
  rect(0, 0, width, height);
  for (int i = 0; i < xPixels; i++) {
    for (int j = 0; j < yPixels; j++) {

      pixelArray[i][j].update();
      pixelArray[i][j].display();

      //if the pixel isn't moving, there is no reason to ask it to move home/bounce off the wall/etc.
      if (pixelArray[i][j].velocity.mag() != 0) {
        pixelArray[i][j].returnHome();
        pixelArray[i][j].checkEdges();
      }
    }
  }

  if (mousePressed && (mouseButton == LEFT)) {
    for (int i = 0; i < xPixels; i++) {
      for (int j = 0; j < yPixels; j++) {
        pixelArray[i][j].explode(20);
      }
    }
  }

  if (gravity) {
    for (int i = 0; i < xPixels; i++) {
      for (int j = 0; j < yPixels; j++) {
        pixelArray[i][j].applyForce(random(.2f, .5f));
      }
    }
  }
  
  if (keyPressed && (keyCode == UP)) {
   gravity = false; 
  }

  if (keyPressed && (keyCode == DOWN)) {
   gravity = true; 
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

  public void checkEdges() {
  //make the superPixels bounce off the edges. increase the velocity for some nice effects!  
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
      velocity.y *= -.9f;
    }
  }
  
  public void applyForce(float force) {
    PVector f = new PVector(0,force);
    acceleration.add(f);
  }
  

  //a function to return the super pixels to their starting space
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
    if (distance < .35f && speed < 4) {
      location = origin.get();
      velocity.mult(0);
      acceleration.mult(0);
    }
    //otherwise, move towards home with a random acceleration 
    else {
      seek.normalize();
      seek.mult(random(.05f, .2f));
      acceleration.add(seek);
    }
  }

  //basic motion, with some damping to slow everything down
  public void update() {
    velocity.add(acceleration);
    velocity.mult(.96f);
    velocity.limit(topspeed);
    location.add(velocity);
    acceleration.mult(0);
  } 
  
  //draw the superPixel
  public void display() {
    fill(fillColor);
    rect(location.x, location.y, xSize, ySize);
  }

  //shoot out away from the mouse
  public void explode(int force_) {
    
    //make a new vector, starting at the mouse
    PVector gunpowder = new PVector(mouseX, mouseY);
    
    //the vector now goes between the mouse and the superPixel
    gunpowder.sub(location);
    
    //check the distance between the two
    float distance = gunpowder.mag();
    
    //if it's far away, no need to affect it (save CPU!)
    if (distance < 60) {
      gunpowder.normalize();
      float amount = -1 * random(force_ / 2, force_);
      gunpowder.mult(amount/distance);
      acceleration.add(gunpowder);
    }
  }
} 
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "picture_manipulation_v4" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
