package uniolunisaar.adam.logic.synthesis.bounded.qbfapproach.petrigame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * This checks the written qcir file for correct structure and no redundancy.
 * Unused but defined variables are identified.
 * Undefined but used variables are already identified by the QBF solver.
 * 
 * @author Jesko Hecking-Harbusch
 */

// TODO should read and use information on the fly

public class QCIRconsistency {

	public static boolean checkConsistency(File qcirFile) throws IOException {
		Set<Integer> used = new HashSet<>();
		Set<Integer> defined = new HashSet<>();
		int numberAndTrue = 0;
		int numberOrFalse = 0;
		BufferedReader reader = new BufferedReader(new FileReader(qcirFile));
		String qcirString;
		try {
			StringBuilder builder = new StringBuilder();
			String line = reader.readLine();

			while (line != null) {
				builder.append(line);
				builder.append(System.lineSeparator());
				line = reader.readLine();
			}
			qcirString = builder.toString();
		} finally {
			reader.close();
		}

		for (String line : qcirString.split(System.lineSeparator())) {
			if (!line.startsWith("#") && !line.startsWith("exists()") && !line.startsWith("forall()") && !line.startsWith("output()")) {
				String[] split;
				if (!line.startsWith("exists(") && !line.startsWith("forall(") && !line.startsWith("output(")) {
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

						split2[0] = split2[0].replace("" + ")", ""); // Note that the "" + is necessary to not work on chars
						used.add(Math.abs(Integer.parseInt(split2[0])));

						for (int i = 1; i < split2.length - 1; ++i) {
							used.add(Math.abs(Integer.parseInt(split2[i])));
						}

						split2[split2.length - 1] = split2[split2.length - 1].replace("" + ")", ""); // Note that the "" + is necessary to not work on chars
						used.add(Math.abs(Integer.parseInt(split2[split2.length - 1])));
					} else if (split2[0].startsWith("exists(") || split2[0].startsWith("forall(")) {
						split2[0] = split2[0].replace("exists(", "");
						split2[0] = split2[0].replace("forall(", "");

						split2[0] = split2[0].replace("" + ")", ""); // Note that the "" + is necessary to not work on chars
						defined.add(Math.abs(Integer.parseInt(split2[0])));

						for (int i = 1; i < split2.length - 1; ++i) {
							defined.add(Math.abs(Integer.parseInt(split2[i])));
						}

						split2[split2.length - 1] = split2[split2.length - 1].replace("" + ")", ""); // Note that the "" + is necessary to not work on chars
						defined.add(Math.abs(Integer.parseInt(split2[split2.length - 1])));
					} else if (split2[0].startsWith("output(")) {
						split2[0] = split2[0].replace("output(", "");

						split2[0] = split2[0].replace("" + ")", ""); // Note that the "" + is necessary to not work on chars
						split2[0] = split2[0].replace(" ", ""); // Specific to output extension for nice order of variables (i.e., no "exposed" 1)
						used.add(Math.abs(Integer.parseInt(split2[0])));
					} else {
						System.out.println("Parsing error at line: " + line);
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
					return false;
				}
			}
		}
		if (numberAndTrue > 1 || numberOrFalse > 1) {
			System.out.println("and() " + numberAndTrue + " or or() " + numberOrFalse + " is defined redundantly.");
			return false;
		}
		return true;
	}
}
