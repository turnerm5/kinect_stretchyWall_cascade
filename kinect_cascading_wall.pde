//the number of superPixels (things start to slow down quickly if this is increased
int xPixels = 200;
int yPixels = 100;


float xSize, ySize, x, y;
color pixelFill;
float isMoving;
color backColor;
boolean gravity = false;
boolean mouseLocked = false;

superPixel[][] pixelArray = new superPixel[xPixels][yPixels];

void setup() {
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

      color pixelFill = color(40);

      pixelArray[i][j] = new superPixel(x, y, pixelFill, xSize, ySize);
    }
  }
}

void draw() {
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
        pixelArray[i][j].applyForce(random(.2, .5));
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

