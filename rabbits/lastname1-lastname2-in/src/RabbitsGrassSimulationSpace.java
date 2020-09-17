import uchicago.src.sim.space.Object2DTorus;
/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DTorus grassGrid;
	private Object2DTorus agentGrid;

	public RabbitsGrassSimulationSpace(int xSize, int ySize) {
		grassGrid = new Object2DTorus(xSize, ySize);
		agentGrid = new Object2DTorus(xSize, ySize);

		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				grassGrid.putObjectAt(i, j, new Integer(0));
			}
		}
	}

	public void spreadGrass(int numInitGrass) {
		// looping through the number of grass to grow
		for (int i = 0; i < numInitGrass; i++) {

			//randomly choose coordinate
			int x = (int)(Math.random()*(grassGrid.getSizeX()));
			int y = (int)(Math.random()*(grassGrid.getSizeY()));		

			// Get the value of the object at those coordinates
			int currentValue = getGrassAt(x, y);
			
			// Replace the Integer object with another one with the new value
			grassGrid.putObjectAt(x,y,currentValue+1);

		}
	}
	
	public int getGrassAt(int x, int y) {
		int i;
		if (grassGrid.getObjectAt(x, y)!=null) {
			i = ((Integer)grassGrid.getObjectAt(x, y)).intValue();
		}
		else { 
			i=0;
		}
		return i;
	}
	
	public Object2DTorus getCurrentRabbitGrassSpace() {
		return grassGrid;
	}

}
