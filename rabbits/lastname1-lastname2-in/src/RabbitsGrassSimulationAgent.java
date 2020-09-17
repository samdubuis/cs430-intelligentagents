import java.awt.Color;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int stepsToLive;
	private static int IDNumber = 0;
	private int ID;
	
	
	public void draw(SimGraphics arg0) {
		if (stepsToLive>10) {
			arg0.drawFastRoundRect(Color.white);			
		}
		else arg0.drawFastRoundRect(Color.blue);
				
	}
	
	public RabbitsGrassSimulationAgent(int rabbitLifespan) {
		x = -1;
	    y = -1;
	    stepsToLive =rabbitLifespan;
	    IDNumber++;
	    ID = IDNumber;
	}
	
	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}
	

	public String getID(){
	    return "A-" + ID;
	  }
	
	public void report(){
	    System.out.println(getID() + " at " + x + ", " + y + " and " + getStepsToLive() + " steps to live.");
	  }
	
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getStepsToLive() {
		return stepsToLive;
	}

	public void setStepsToLive(int stepsToLive) {
		this.stepsToLive = stepsToLive;
	}

	public void step() {
		stepsToLive--;
	}

}
