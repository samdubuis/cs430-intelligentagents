import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int grass;
	private int stepsToLive;
	
	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public RabbitsGrassSimulationAgent(int rabbitLifespan) {
		x = -1;
	    y = -1;
	    grass = 0;
	    stepsToLive =rabbitLifespan;
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

	public int getGrass() {
		return grass;
	}

	public void setGrass(int grass) {
		this.grass = grass;
	}

	public int getStepsToLive() {
		return stepsToLive;
	}

	public void setStepsToLive(int stepsToLive) {
		this.stepsToLive = stepsToLive;
	}

	

}
