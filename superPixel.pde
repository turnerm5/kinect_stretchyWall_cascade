//the superPixels are the actual cubes that fly around.

class superPixel {
  PVector origin;
  PVector location;
  PVector velocity;
  PVector acceleration;
  color fillColor;
  float xSize, ySize;
  float topspeed;

  //construct the superPixels!!
  superPixel(float x_, float y_, color fill_, float xSize_, float ySize_) {

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

  void checkEdges() {
  //make the superPixels bounce off the edges. increase the velocity for some nice effects!  
    if (location.x < 0) {
      location.x = 0;
      velocity.x *= -.9;
    } 
    else if (location.x > width) {
      location.x = width;
      velocity.x *= -.9;
    }

    if (location.y < 0) {
      location.y = 0;
      velocity.y *= -.9;
    } 
    else if (location.y > height - 2) {
      location.y = height - 2;
      velocity.y *= -.9;
    }
  }
  
  void applyForce(float force) {
    PVector f = new PVector(0,force);
    acceleration.add(f);
  }
  

  //a function to return the super pixels to their starting space
  void returnHome() {
    //make a new vector, using the origin of the superPixel as a starting point
    PVector seek = origin.get();
    
    //the vector now points from the location to the origin of the superPixel
    seek.sub(location);
    
    //how far away are we from the start? useful for the next part
    
    float distance = seek.mag();
    float speed = velocity.mag();
    //the distance test here seems to control the wobble
    
    //if the superPixel is slow and close to it's origin, reset it
    if (distance < .35 && speed < 4) {
      location = origin.get();
      velocity.mult(0);
      acceleration.mult(0);
    }
    //otherwise, move towards home with a random acceleration 
    else {
      seek.normalize();
      seek.mult(random(.05, .2));
      acceleration.add(seek);
    }
  }

  //basic motion, with some damping to slow everything down
  void update() {
    velocity.add(acceleration);
    velocity.mult(.96);
    velocity.limit(topspeed);
    location.add(velocity);
    acceleration.mult(0);
  } 
  
  //draw the superPixel
  void display() {
    fill(fillColor);
    rect(location.x, location.y, xSize, ySize);
  }

  //shoot out away from the mouse
  void explode(int force_) {
    
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
