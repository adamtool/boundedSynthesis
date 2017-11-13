package uniolunisaar.adam.bounded.qbfapproach.petrigame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class QCIRconsistency {
	
	public static boolean checkConsistency(File qcir) throws IOException {
		Set<Integer> used = new HashSet<>();
		Set<Integer> defined = new HashSet<>();
		int numberAndTrue = 0;
		int numberOrFalse = 0;
		BufferedReader br = new BufferedReader(new FileReader(qcir));
		String everything = "";
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    everything = sb.toString();
		} finally {
		    br.close();
		}
		
		for (String line : everything.split(System.lineSeparator())) {
			if (!line.startsWith("#") && !line.startsWith("exists()") && !line.startsWith("forall()") && !line.startsWith("output(")) {	// ignore 1 from output(1) 
				String[] split;
				if (!line.startsWith("exists(") && !line.startsWith("forall(")) {
					split = line.split(" = ");
					if (Math.abs(Integer.parseInt(split[0])) != 1) {
						defined.add(Math.abs(Integer.parseInt(split[0])));
					}
				} else {
					split = new String[2];
					split[1] = line;
				}
				if (!split[1].startsWith("and()") && !split[1].startsWith("or()")) {
					String[] split2 = split[1].split(",");
					if (split2[0].startsWith("and(") || split2[0].startsWith("or(")) {
						split2[0] = split2[0].replace("and(", "");
						split2[0] = split2[0].replace("or(", "");
						
						split2[0] = split2[0].replace("" + ")", "");			// Note that the "" +  is necessary to not work on chars
						used.add(Math.abs(Integer.parseInt(split2[0])));
					
						
						for (int i = 1; i < split2.length - 1; ++i) {
							used.add(Math.abs(Integer.parseInt(split2[i])));
						}
						
						split2[split2.length - 1] = split2[split2.length - 1].replace("" + ")", "");			// Note that the "" +  is necessary to not work on chars
						used.add(Math.abs(Integer.parseInt(split2[split2.length - 1])));
					} else if (split2[0].startsWith("exists(") || split2[0].startsWith("forall(")) {
						split2[0] = split2[0].replace("exists(", "");
						split2[0] = split2[0].replace("forall(", "");
						
						split2[0] = split2[0].replace("" + ")", "");			// Note that the "" +  is necessary to not work on chars
						defined.add(Math.abs(Integer.parseInt(split2[0])));
					
						for (int i = 1; i < split2.length - 1; ++i) {
							defined.add(Math.abs(Integer.parseInt(split2[i])));
						}
						
						split2[split2.length - 1] = split2[split2.length - 1].replace("" + ")", "");			// Note that the "" +  is necessary to not work on chars
						defined.add(Math.abs(Integer.parseInt(split2[split2.length - 1])));
					} else {
						System.out.println("SOME PARSING WENT WRONG " + line);
						return false;
					}
				} else {
					if (split[1].startsWith("and()")) {
						numberAndTrue++;
					} else if (split[1].startsWith("or()")) {
						numberOrFalse++;
					}
				}
			}
		}
		
		if (!defined.equals(used)) {
			for (int def : defined) {
				if (!used.contains(def)) {
					System.out.println(def + " is defined but never used.");
				}
			}
			return false;
		}
		if (numberAndTrue > 1 || numberOrFalse > 1) {
			System.out.println("and() " + numberAndTrue + " or or() " + numberOrFalse + " is defined redundantly.");
			return false;
		}
		return true;
	}
}