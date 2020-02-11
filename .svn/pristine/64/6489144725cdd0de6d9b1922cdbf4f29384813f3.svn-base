package org.processmining.prediction.newPrefuseTreeVis;


import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenuItem;

import org.processmining.prediction.PredictionType;

import prefuse.data.Tree;
import weka.gui.visualize.plugins.TreeVisualizePlugin;

public class NewPrefuseTreeVis
implements Serializable, TreeVisualizePlugin {
	 GraphMLParser parser = new GraphMLParser();
	 
	 /** the constant for "tree". */
	 public final static String TREE = "tree";

	 /** the constant for "tree.nodes". */
	 public final static String TREE_NODES = "tree.nodes";

	 /** the constant for "tree.edges". */
	 public final static String TREE_EDGES = "tree.edges";

	 /** the constant for "label". */
	 public final static String LABEL = "label";
  
	 /** for serialization. */
	 private static final long serialVersionUID = 7485599985684890717L;
	  
	 public TreePanel display(String dotty, String name, Map<String,Color> variableColor, HashMap<String, PredictionType> predictionTypes, PredictionType classType, VisConfigurables config) {
		 String	treeml = null;
		 Tree	tree = null;
		 TreePanel	panel = null;
		 try {
			// convert dotty graph
			treeml = parser.convert(dotty, classType, predictionTypes,config);
			if (treeml == null) return null;
			
			// parse graph
			tree = parser.parse(treeml);
			if (tree == null)  return null;

			// display graph
			panel = new TreePanel(tree, variableColor, config);
			} catch (Exception e) {
				//System.out.printf(treeml, tree, panel); //For debugging purposes
				System.out.println(e.toString());
				e.printStackTrace();
			}
		 	
			System.out.println("Display finished");
			return panel;
	 }

	 /**
	   * Get the minimum version of Weka, inclusive, the class
	   * is designed to work with.  eg: <code>3.5.0</code>
	   * 
	   * @return		the minimum version
	   */
	  public String getMinVersion() {
	    return "3.5.9";
	  }

	  /**
	   * Get the maximum version of Weka, exclusive, the class
	   * is designed to work with.  eg: <code>3.6.0</code>
	   * 
	   * @return		the maximum version
	   */
	  public String getMaxVersion() {
	    return "3.7.0";
	  }

	  /**
	   * Get the specific version of Weka the class is designed for.
	   * eg: <code>3.5.1</code>
	   * 
	   * @return		the version the plugin was designed for
	   */
	  public String getDesignVersion() {
	    return "3.5.9";
	  }

	  /**
	   * Get a JMenu or JMenuItem which contain action listeners
	   * that perform the visualization of the tree in GraphViz's dotty format.  
	   * Exceptions thrown because of changes in Weka since compilation need to 
	   * be caught by the implementer.
	   *
	   * @see NoClassDefFoundError
	   * @see IncompatibleClassChangeError
	   *
	   * @param dotty 	the tree in dotty format
	   * @param name	the name of the item (in the Explorer's history list)
	   * @return menuitem 	for opening visualization(s), or null
	   *         		to indicate no visualization is applicable for the input
	   */
	  public JMenuItem getVisualizeMenuItem(String dotty, String name) {
	    JMenuItem	result = null;
	    
//	    final String dottyF = dotty;
//	    final String nameF = name;
//	    result = new JMenuItem("Prefuse tree");
//	    result.addActionListener(new ActionListener() {
//	      public void actionPerformed(ActionEvent e) {
//		display(dottyF, nameF);
//	      }
//	    });
//	    
	    return result;
	  }
}