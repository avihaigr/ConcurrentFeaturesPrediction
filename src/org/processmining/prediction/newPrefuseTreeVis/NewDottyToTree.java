package org.processmining.prediction.newPrefuseTreeVis;

import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;

import org.processmining.prediction.PredictionType;

import weka.gui.treevisualizer.Edge;
import weka.gui.treevisualizer.Node;
import weka.gui.treevisualizer.TreeBuild;

public class NewDottyToTree {
	/**
	 * Replaces certain characters with their character entities.
	 * 
	 * @param s
	 *            the string to process
	 * @return the processed string
	 */
	protected String sanitize(String s) {

		String result = s;
		if (s.contains(":")) {
			result = s.split(":")[1].trim();
		}
		result = result.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;")
				.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		// in addition, replace some other entities as well
		result = result.replaceAll("\n", "&#10;").replaceAll("\r", "&#13;").replaceAll("\t", "&#9;");

		return result;
	}

	protected String convertTime(String time, int nrDecimals, PredictionType classType) {
		try {
			BigDecimal bg = new BigDecimal(time);
			
			if (classType == PredictionType.TIMESTAMP)
				time = new Date(bg.longValue()).toString();
			else if (classType == PredictionType.TIMEINTERVAL) {
				float value = bg.floatValue();
				if (value == 0){
				} else if (value < 1000) {
					time = bg.longValue() + " millisecs";
				} else if (value < 60000) {
					BigDecimal bd = new BigDecimal(value / 1000F);
					value = bd.setScale(nrDecimals, RoundingMode.UP).floatValue();
					time = value + " secs";
					//split[1] = value / 1000F + " secs";
				} else if (value < 3600000) {
					BigDecimal bd = new BigDecimal(value / 60000F);
					value = bd.setScale(nrDecimals, RoundingMode.UP).floatValue();
					time = value + " mins";
					//split[1] = value / 60000F + " mins";
				} else if (value < 86400000F) {
					BigDecimal bd = new BigDecimal(value / 3600000F);
					value = bd.setScale(nrDecimals, RoundingMode.UP).floatValue();
					time = value + " hours";
					//split[1] = value / 3600000F + " hours";
				} else {
					BigDecimal bd = new BigDecimal(value / 86400000F);
					value = bd.setScale(nrDecimals, RoundingMode.UP).floatValue();
					time = value + " days";
					//split[1] = value / 86400000F + " days";
				}
			}
		} catch (Exception err) {
			//err.printStackTrace();
		}
		return time;
	}

	/**
	 * Check is value is in highest possible time format
	 * 
	 * @param b
	 * 
	 * @param nrDecimals
	 * @param classType
	 * 
	 * @param s
	 *            the string to process
	 * @return the processed string
	 */
	protected String checkTime(int nrDecimals, PredictionType classType, String label) {
		//label=label.replace(']', '[');
		String[] split = label.trim().split(" ");
		String[] split1 = split[0].split(",");
		
		label = "";
		//If length is larger than one it means that the label contains at least one space and equally at least 2 'words'
		if(split.length > 1){	
			if (split1.length > 1) {
				split1[0] = split1[0].replace("[", "");
				split1[1] = split1[1].replace("[", "");
				split1[0] = convertTime(split1[0], nrDecimals, classType);
				split1[1] = convertTime(split1[1], nrDecimals, classType);
				label += "[ "+split1[0] + "," + split1[1]+"[ " + split[1];
				label = label.trim();
			} else {
				split[0] = convertTime(split[0], nrDecimals, classType);
				split[1] = convertTime(split[1], nrDecimals, classType);
				for (String string : split)
					label += string + " ";
				label = label.trim();
			}
		} else {
			label = split[0];
		}
			
		

		return label;
	}

	/**
	 * Writes the header of the GraphML file.
	 * 
	 * @param writer
	 *            the writer to use
	 * @throws Exception
	 *             if an error occurs
	 */
	protected void writeHeader(BufferedWriter writer) throws Exception {
		writer.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		writer.newLine();
		writer.newLine();
		writer.write("<!-- This file was generated by Weka (http://www.cs.waikato.ac.nz/ml/weka/). -->");
		writer.newLine();
		writer.newLine();
		//writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.nomencurator.org/InfoVis2003/download/treeml.dtd\">");
		writer.newLine();
		writer.write("<tree>");
		writer.newLine();
		writer.write("<declarations>");
		writer.newLine();
		writer.write("<attributeDecl name=\"name\" type=\"String\"/>");
		writer.newLine();
		writer.write("<attributeDecl name=\"edge\" type=\"String\"/>");
		writer.newLine();
		writer.write("</declarations>");
		writer.newLine();
	}

	/**
	 * Writes the node as GraphML.
	 * 
	 * @param writer
	 *            the writer to use
	 * @param node
	 *            the node to write as GraphML
	 * @param classType
	 * @param predictionTypes
	 * @param config
	 * @param nrDecimals
	 * @param nullLeaves
	 * @throws Exception
	 *             if an error occurs
	 */
	protected void writeNode(BufferedWriter writer, Node node, PredictionType classType,
			HashMap<String, PredictionType> predictionTypes, VisConfigurables config) throws Exception {
		int i;
		String tag;
		boolean leaf;
		// leaf?
		leaf = (node.getChild(0) == null);
		if (leaf) {
			if (!(config.nullLeaves && node.getLabel().contains("(0.0)"))) {
				tag = "leaf";
				// the node itself
				writer.write("<" + tag + ">");
				writer.newLine();
				writer.write("<attribute name=\"name\" value=\""
						+ checkTime(config.nrDecimals, classType, sanitize(node.getLabel())) + "\">");
				writer.newLine();
				writer.write("</attribute>");
				writer.newLine();
				writer.write("</" + tag + ">");
				writer.newLine();
			}
		} else {
			tag = "branch";
			// the node itself
			writer.write("<" + tag + ">");
			writer.newLine();
			writer.write("<attribute name=\"name\" value=\"" + checkTime(config.nrDecimals,classType,  sanitize(node.getLabel())) + "\">");
			writer.newLine();
			writer.write("</attribute>");
			writer.newLine();

			// the node's children, if any
			for (i = 0; (node.getChild(i) != null); i++) {
				writeEdge(writer, node.getChild(i), classType, predictionTypes, config);
			}
			writer.write("</" + tag + ">");
			writer.newLine();

		}

	}

	/**
	 * Writes the edge as GraphML. Since prefuse doesn't seem to offer edge
	 * labels, the edges get inserted as nodes as well.
	 * 
	 * @param writer
	 *            the writer to use
	 * @param edge
	 *            the edge to write
	 * @param classType
	 * @param predictionTypes
	 * @param config
	 * @throws Exception
	 *             if an error occurs
	 */
	protected void writeEdge(BufferedWriter writer, Edge edge, PredictionType classType,
			HashMap<String, PredictionType> predictionTypes, VisConfigurables config) throws Exception {
		if (edge.getLabel().length() > 0 && !(config.nullLeaves && edge.getTarget().getLabel().contains("(0.0)"))) {
			writer.write("<branch>");
			writer.newLine();
			//writer.write("<attribute name=\"name\" value=\"" + sanitize(edge.getLabel()) + "\"/>");
			PredictionType variableType = predictionTypes.get(sanitize(edge.getSource().getLabel()));
			writer.write("<attribute name=\"name\" value=\""
					+ checkTime(config.nrDecimals, variableType, sanitize(edge.getLabel())) + "\"/>");
			writer.newLine();
			writeNode(writer, edge.getTarget(), classType, predictionTypes, config);
			writer.write("</branch>");
			writer.newLine();
		} else {
			writeNode(writer, edge.getTarget(), classType, predictionTypes, config);
		}
	}

	/**
	 * Writes the footer of the GraphML file.
	 * 
	 * @param writer
	 *            the writer to use
	 * @throws Exception
	 *             if an error occurs
	 */
	protected void writeFooter(BufferedWriter writer) throws Exception {
		writer.write("</tree>");
		writer.newLine();
	}

	/**
	 * Parses the incoming data and writes the generated output.
	 * 
	 * @param dotty
	 *            the graph in dotty format
	 * @param classType
	 * @param predictionTypes
	 * @param config
	 * @return the TreeML data
	 * @throws Exception
	 *             if parsing or writing fails
	 */
	public String convert(String dotty, PredictionType classType, HashMap<String, PredictionType> predictionTypes,
			VisConfigurables config) throws Exception {
		Node root;
		TreeBuild tree;
		StringWriter output;
		BufferedWriter writer;
		// parse dotty format
		tree = new TreeBuild();
		root = tree.create(new StringReader(dotty));

		// generate GraphML output
		output = new StringWriter();
		writer = new BufferedWriter(output);
		writeHeader(writer);
		writeNode(writer, root, classType, predictionTypes, config);
		writeFooter(writer);
		writer.flush();

		return output.toString();
	}
}