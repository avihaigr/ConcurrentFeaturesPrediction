package org.processmining.prediction.newPrefuseTreeVis;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.processmining.prediction.PredictionType;

import prefuse.data.Tree;
import prefuse.data.io.TreeMLReader;

public class GraphMLParser {
	  /**
	   * Parses the graph in GraphML and returns the built graph.
	   * 
	   * @param graphml	the graph in GraphML
	   * @return		the graph or null in case of an error
	   */
	  protected Tree parse(String graphml) {
	    ByteArrayInputStream	inStream;
	    Tree			result;
	    try {
	      inStream = new ByteArrayInputStream(graphml.getBytes());
	      result   = (Tree) new TreeMLReader().readGraph(inStream);
	    }
	    catch ( Exception e ) {
	      result = null;
	      e.printStackTrace();
	      JOptionPane.showMessageDialog(null, e.toString(), "Error displaying graph", JOptionPane.ERROR_MESSAGE);
	    }
	    return result;	
	  }
	  /**
	   * Converts the dotty format to TreeML.
	   * 
	   * @param dotty	the graph in dotty format
	 * @param predictionTypes 
	 * @param classType 
	 * @param predictionTypes 
	 * @param predictionTypes 
	 * @param config 
	   * @return		the graph in tree XML or null in case of an error
	   */
	  protected String convert(String dotty, PredictionType classType, HashMap<String, PredictionType> predictionTypes, VisConfigurables config) {
	    String	result;
	    NewDottyToTree d2gml = new NewDottyToTree();
	    try {
	      result = d2gml.convert(dotty, classType, predictionTypes,config);
	    }
	    catch (Exception e) {
	      result = null;
	      e.printStackTrace();
	      JOptionPane.showMessageDialog(null, e.toString(), "Error displaying graph", JOptionPane.ERROR_MESSAGE);
	    }
	    
	    return result;
	  }
}
