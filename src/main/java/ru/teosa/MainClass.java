package ru.teosa;

import java.io.File;

public class MainClass {

	public static void main(String args[]) {
		if(args == null || args[0] == null || args[0].trim().length() == 0) {
			System.out.println("Error. Path to .yml config file required.");
			return;
		}

		File ymlConfig = new File(args[0]);

		if(!checkExtension(ymlConfig)) {
			System.out.println("Error. Input file must have .yml extension.");
			return;
		}

		new Parser(ymlConfig).parse();

	}

	private static boolean checkExtension(File ymlConfig) {
		return "yml".equalsIgnoreCase(ymlConfig.getName().split("\\.")[1]);
	}

}
