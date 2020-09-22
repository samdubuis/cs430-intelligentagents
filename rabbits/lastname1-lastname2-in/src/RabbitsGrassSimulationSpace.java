import uchicago.src.sim.space.Object2DTorus;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 *
 * @author
 */

public class RabbitsGrassSimulationSpace {
	private final Object2DTorus grassGrid;
	private final Object2DTorus agentGrid;

	public RabbitsGrassSimulationSpace(int xSize, int ySize) {
		grassGrid = new Object2DTorus(xSize, ySize);
		agentGrid = new Object2DTorus(xSize, ySize);

		// place "integer(0)" everywhere
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				grassGrid.putObjectAt(i, j, new Integer(0));
			}
		}
	}

	public Object2DTorus getCurrentRabbitGrassSpace() {
		return grassGrid;
	}

	public Object2DTorus getCurrentAgentSpace() {
		return agentGrid;
	}

	public void spreadGrass(int numInitGrass) {
		// looping through the number of grass to grow
		for (int i = 0; i < numInitGrass; i++) {

			//randomly choose coordinate
			int x = (int) (Math.random() * (grassGrid.getSizeX()));
			int y = (int) (Math.random() * (grassGrid.getSizeY()));

			// Get the value of the object at those coordinates
			int currentValue = getGrassAt(x, y);

			// limit the grass count on one spot to 16 (0-15)
			if (currentValue <= 15) {
				// Replace the Integer object with another one with the new value
				grassGrid.putObjectAt(x, y, new Integer(currentValue + 1));
			}		

		}
	}

	// function that returns the number of grass at a defined position
	public int getGrassAt(int x, int y) {
		if (grassGrid.getObjectAt(x, y) != null) {
			return ((Integer) grassGrid.getObjectAt(x, y)).intValue();
		} else {
			return 0;
		}
	}

	// function for eating grass, meaning replacing the integer value by 0
	public int eatGrassAt(int x, int y) {
		int grass = getGrassAt(x, y);
		grassGrid.putObjectAt(x, y, new Integer(0));
		return grass;
	}

	public int countGrass() {
		int totalGrass = 0;
		for (int i = 0; i < grassGrid.getSizeX(); i++) {
			for (int j = 0; j < grassGrid.getSizeY(); j++) {
				totalGrass += getGrassAt(i, j);
			}
		}
		return totalGrass;
	}

	public boolean isCellOccupied(int x, int y) {
		return agentGrid.getObjectAt(x, y) != null;
	}

	// 10 is the number of times tried to add an agent, it return false if all the grid is occupied
	public boolean addAgent(RabbitsGrassSimulationAgent agent) {
		int countLimit = 10 * agentGrid.getSizeX() * agentGrid.getSizeY();

		for (int count = 0; count < countLimit; count++) {
			int x = (int) (Math.random() * (agentGrid.getSizeX()));
			int y = (int) (Math.random() * (agentGrid.getSizeY()));
			if (!isCellOccupied(x, y)) {
				agentGrid.putObjectAt(x, y, agent);
				agent.setXY(x, y);
				agent.setRabbitsGrassSimulationSpace(this);
				return true;
			}
		}

		return false;
	}

	public void removeAgentAt(int x, int y) {
		agentGrid.putObjectAt(x, y, null);
	}

	public boolean moveAgentAt(int x, int y, int newX, int newY) {
		if (isCellOccupied(newX, newY))
			return false;

		RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent) agentGrid.getObjectAt(x, y);
		removeAgentAt(x, y);
		rga.setXY(newX, newY);
		agentGrid.putObjectAt(newX, newY, rga);
		return true;
	}

}
