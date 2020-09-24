import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

import java.awt.*;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgSpace;
//	private boolean hasBirthed = false;


	public void draw(SimGraphics arg0) {
		arg0.drawFastCircle(Color.white);
	}

	public RabbitsGrassSimulationAgent(int rabbitLifespan) {
		x = -1;
		y = -1;
		energy = rabbitLifespan;
		setVxVy();
		IDNumber++;
		ID = IDNumber;
	}

	public void setXY(int newX, int newY) {
		x = newX;
		y = newY;
	}

	private void setVxVy() {
		vX = 0;
		vY = 0;
		int choice = (int) Math.floor(Math.random() * 4);
		// the rabbit can only move in the cardinal directions NESW
		switch (choice) {
		case 0:
			vX = 1;
			vY = 0;
			break;
		case 1:
			vX = -1;
			vY = 0;
			break;
		case 2:
			vX = 0;
			vY = 1;
			break;
		case 3:
			vX = 0;
			vY = -1;
			break;
		}
	}

	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgs) {
		rgSpace = rgs;
	}

	public void report() {
		System.out.println(getID() + " at " + x + ", " + y + " and " + getEnergy() + " steps to live.");
	}

	public String getID() {
		return "A-" + ID;
	}

	public void setID(int iD) {
		ID = iD;
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

	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public int getVX() {
		return vX;
	}

	public void setVX(int vX) {
		this.vX = vX;
	}

	public int getVY() {
		return vY;
	}

	public void setVY(int vY) {
		this.vY = vY;
	}

	public static int getIDNumber() {
		return IDNumber;
	}

	public static void setIDNumber(int iDNumber) {
		IDNumber = iDNumber;
	}

	public RabbitsGrassSimulationSpace getRgSpace() {
		return rgSpace;
	}

	public void setRgSpace(RabbitsGrassSimulationSpace rgSpace) {
		this.rgSpace = rgSpace;
	}

//	public boolean isHasBirthed() {
//		return hasBirthed;
//	}
//
//	public void setHasBirthed(boolean hasBirthed) {
//		this.hasBirthed = hasBirthed;
//	}

	public void step() {
		// we call setVxVy at the beginning of each step so that the rabbit gets a new vector to move to
		setVxVy();
		int newX = x + vX;
		int newY = y + vY;

		Object2DGrid grid = rgSpace.getCurrentAgentSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();

		tryMove(newX, newY);
		energy += rgSpace.eatGrassAt(x, y);
		// no need to reset direction after collision, direction is reset at every tick

		energy--;
	}

	private boolean tryMove(int newX, int newY) {
		return rgSpace.moveAgentAt(x, y, newX, newY);
	}

}
