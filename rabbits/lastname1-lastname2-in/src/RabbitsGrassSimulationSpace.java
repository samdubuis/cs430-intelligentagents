import uchicago.src.sim.space.Object2DTorus;
/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	private Object2DTorus grid;

	public RabbitsGrassSimulationSpace(int xSize, int ySize) {
		grid = new Object2DTorus(xSize, ySize);
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				grid.putObjectAt(i, j, new Integer(0));
			}
		}
	}

	public void growGrass(int grassGrowthRate) {
		// looping through the number of grass to grow
		for (int i = 0; i < grassGrowthRate; i++) {

			//randomly choose coordinate
			int x = (int)(Math.random()*(grid.getSizeX()));
			int y = (int)(Math.random()*(grid.getSizeY()));		

			// Get the value of the object at those coordinates
			int currentValue = getGrassAt(x, y);
			
			// Replace the Integer object with another one with the new value
			grid.putObjectAt(x,y,currentValue+1);

		}
	}
	
	public int getGrassAt(int x, int y) {
		int i;
		if (grid.getObjectAt(x, y)!=null) {
			i = ((Integer)grid.getObjectAt(x, y)).intValue();
		}
		else { 
			i=0;
		}
		return i;
	}

}
