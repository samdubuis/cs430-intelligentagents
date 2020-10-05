package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CsvWriter {
	/**
	 * Class for easily writing (action, profit, profit/km) tuples to a CSV file for export
	 */
	// CSV-writing logic inspired from here: https://stackoverflow.com/a/30074268
	private PrintWriter csvWriter;

	public CsvWriter(String filename) {
		// Opens a file for printing profit information
		try {
			this.csvWriter = new PrintWriter(new File(filename));
			this.csvWriter.println("Round,Total profit,Profit per km");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void add(int round, long profit, double profit_distance) {
		if (csvWriter != null) {
			csvWriter.println(round + "," + profit + "," + profit_distance);
			csvWriter.flush();
		}
	}
}
