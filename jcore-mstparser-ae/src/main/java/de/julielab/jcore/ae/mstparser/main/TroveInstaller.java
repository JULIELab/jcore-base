package de.julielab.jcore.ae.mstparser.main;

import java.io.IOException;

public class TroveInstaller {
	final static String TROVE_JAR = "src/main/resources/repo/de/julielab/jules-trove/1.3/jules-trove-1.3.jar";
	
	public static void main(String[] args) throws IOException {
		System.out.println("[Dependency Info] Installing Trove...");
		Runtime.getRuntime().exec(
				"mvn install:install-file" + 
				" -Dfile=" + TROVE_JAR +
				" -DgroupId=de.julielab" +
				" -DartifactId=jules-trove" +
				" -Dversion=1.3" +
				" -Dpackaging=jar");
	}
}
