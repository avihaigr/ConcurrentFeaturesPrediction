package org.processmining.prediction;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
//import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.prediction.Augmentation.Augmentation;

import com.fluxicon.slickerbox.components.NiceDoubleSlider;
import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.factory.SlickerFactory;

public class ConfigurationPanel extends JPanel implements ViewInteractionPanel {

	private final JCheckBox pruneBox;
	private final NiceDoubleSlider confThreshold;
	private final NiceIntegerSlider minNumInstancePerLeaf;
	private final NiceIntegerSlider numFoldErrorPruning;
	private final NiceIntegerSlider numFoldCrossValidation;
	private final JCheckBox saveDataBox;
	private final JCheckBox arffFile;
	private final JCheckBox considerFirstActivityOnly;
	private final JCheckBox binaryBox;
	
	private final String traceFilterCbxDefaultValue = "Filter traces by...";
	private final String traceFilterValueCbxDefaultValue = "Filter Value...";
	private JComboBox outputAttribCbx=	SlickerFactory.instance().createComboBox(new String[]{"Please augment the log"});
	
	private JComboBox traceFilterCbx=	SlickerFactory.instance().createComboBox(new String[]{traceFilterCbxDefaultValue});
	private JComboBox traceFilterValueCbx=	SlickerFactory.instance().createComboBox(new String[]{traceFilterValueCbxDefaultValue});
	
	private ProMTextField traceStartTime;
	private ProMTextField traceEndTime;
	
	private Augmentation lastSelectedAttribute=null;
	
	private String lastSelectTraceFilter=null;
	
	private DecisionTreePanel frame;
	private JRadioButton decRBtn;
	private JRadioButton regRBtn;
	private JRadioButton normVisBtn;
	private JRadioButton prefuseVisBtn;
	private XLog log;
	private Collection<String> traceAttributeSet;
	//private ArrayList<String> attributesFilterValues;
	private Map<String,ArrayList<Object>> attributesFilterValues;
	
	public ConfigurationPanel(final DecisionTreePanel frame, int numInstances,Collection<String> traceAttributeSet,final XLog log)
	{
		this.frame=frame;
		this.log = log;
		SlickerFactory instance=SlickerFactory.instance();
		decRBtn=instance.createRadioButton("Decision Tree");
		decRBtn.setSelected(true);
		this.traceAttributeSet = traceAttributeSet;
	//	attributesFilterValues = new ArrayList<String>();
		attributesFilterValues  = new HashMap<String, ArrayList<Object>>();
		for(String traceAttrib : traceAttributeSet)
		{
			traceFilterCbx.addItem(traceAttrib);
		}
		
		decRBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				binaryBox.setEnabled(true);
				saveDataBox.setEnabled(true);
				//arffFile.setEnabled(false);
				//considerFirstActivityOnly.setEnabled(false);
				frame.setRegressionTree(false);
			}
		});
		
		
		regRBtn=instance.createRadioButton("Decision/Regression Tree");
		regRBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				binaryBox.setEnabled(false);
				binaryBox.setSelected(false);
				saveDataBox.setEnabled(false);
				saveDataBox.setSelected(false);
				arffFile.setEnabled(false);
				arffFile.setSelected(false);
			//	considerFirstActivityOnly.setEnabled(false);
			//	considerFirstActivityOnly.setSelected(false);
				frame.setRegressionTree(true);
			}
		});	
	
		ButtonGroup bgrp=new ButtonGroup();
		bgrp.add(decRBtn);
		bgrp.add(regRBtn);
		
		normVisBtn=instance.createRadioButton("Helicopter Visualization");
		normVisBtn.setSelected(true);
		
		prefuseVisBtn=instance.createRadioButton("Explorative Visualization");
		prefuseVisBtn.setSelected(true);
		
		bgrp=new ButtonGroup();
		bgrp.add(normVisBtn);
		bgrp.add(prefuseVisBtn);
		
		pruneBox= instance.createCheckBox("Prune Tree", true);
		binaryBox= instance.createCheckBox("Binary Tree", false);
		saveDataBox=	instance.createCheckBox("Instance data to be associated with tree's elements (slow!)", false);
		arffFile=	instance.createCheckBox("Write ARFF file", false);
		considerFirstActivityOnly=instance.createCheckBox("Consider First Activity Only",true);
		
		considerFirstActivityOnly.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				frame.setConsiderFirstActivityOnly(considerFirstActivityOnly.isSelected());
				
			}
		});
		considerFirstActivityOnly.setSelected(false);
		
		
		confThreshold = instance.createNiceDoubleSlider("Set confidence threshold for pruning", 0.1, 1, 0.25, Orientation.HORIZONTAL);
		minNumInstancePerLeaf=	instance.createNiceIntegerSlider("Minimum Number of instances per leaf", 2, numInstances, 3, Orientation.HORIZONTAL);
		numFoldErrorPruning=		instance.createNiceIntegerSlider("Number of folds for reduced error pruning", 2, 10, 2, Orientation.HORIZONTAL);
		numFoldCrossValidation = instance.createNiceIntegerSlider("Number of folds for cross validation", 5, 20, 10, Orientation.HORIZONTAL);
		setLayout(new GridLayout(11,1));
		add(SlickerFactory.instance().createLabel("Dependent characteristic:"));
		add(outputAttribCbx);
		RoundedPanel algorithmPnl=new RoundedPanel();
		algorithmPnl.setLayout(new FlowLayout(FlowLayout.CENTER));
		algorithmPnl.add(decRBtn);
		algorithmPnl.add(regRBtn);
		add(algorithmPnl);
		RoundedPanel visMethodPnl = new RoundedPanel();
		visMethodPnl.setLayout(new FlowLayout(FlowLayout.CENTER));
		visMethodPnl.add(normVisBtn);
		visMethodPnl.add(prefuseVisBtn);
		add(visMethodPnl);
		add(pruneBox);
		add(binaryBox);
		add(saveDataBox);
		add(arffFile);
		add(considerFirstActivityOnly);
		add(confThreshold);
		add(minNumInstancePerLeaf);
		add(numFoldErrorPruning);
		add(numFoldCrossValidation);
		
		add(SlickerFactory.instance().createLabel("Filter traces by attribute:"));
		add(traceFilterCbx);
		
		add(SlickerFactory.instance().createLabel("Value of filter attribute:"));
		add(traceFilterValueCbx);
		
		add(SlickerFactory.instance().createLabel("Only Traces that starts after:"));
		traceStartTime = new ProMTextField("dd/MM/yyyy");
		add(traceStartTime);
		
		add(SlickerFactory.instance().createLabel("Only Traces that ends before:"));
		traceEndTime = new ProMTextField("dd/MM/yyyy");
		add(traceEndTime);
		
		JPanel panel=new JPanel();
		JButton button=SlickerFactory.instance().createButton("Update Decision Tree");
		button.addActionListener(new ActionListener() {
			
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent arg0) {
				try {
					frame.setEnabled(false);
					frame.setTraceStartTime(getTraceStartTime());
					frame.setTraceEndTime(getTraceEndTime());

					TaskForProgressBar task1=new TaskForProgressBar(null,"Learning Decision Tree","",0,100) {

						protected Void doInBackground() throws Exception {
							frame.createPanel(this);
							return null;
						}

						protected void done() {
							frame.setEnabled(true);
						}

					};

					task1.execute();					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		});;
		panel.add(button);
		add(panel);
		
		outputAttribCbx.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				if (outputAttribCbx.getSelectedItem() instanceof Augmentation)
					setOutputAttribute((Augmentation) outputAttribCbx.getSelectedItem());
				
			}
		});
		
		traceFilterCbx.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				traceFilterValueCbx.removeAllItems();
				
				lastSelectTraceFilter = traceFilterCbx.getSelectedItem().toString();
				if (lastSelectTraceFilter.equals(traceFilterCbxDefaultValue))
				{
					frame.setAttributeFilterName(null);
					traceFilterValueCbx.removeAllItems();
					traceFilterValueCbx.addItem(traceFilterValueCbxDefaultValue);
				}
				else{
					frame.setAttributeFilterName(lastSelectTraceFilter);	
					ArrayList<Object> values = attributesFilterValues.get(lastSelectTraceFilter);
					Object value;
					if (values == null){
						values = new ArrayList<Object>();	
						for (XTrace trace : log) {
							XAttribute attributeObj = trace.getAttributes().get(lastSelectTraceFilter);
							if (attributeObj==null)
								value=null;
							else
								value=getAttributeValues(attributeObj);
							if (!values.contains(value))
								values.add(value);												
						}
						attributesFilterValues.put(lastSelectTraceFilter, values);
					}				
					for(Object traceAttrib : values)
					{
						traceFilterValueCbx.addItem(traceAttrib);
					}
				}
			}
		});
		
		traceFilterValueCbx.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				frame.setAttributeFilterValue(traceFilterValueCbx.getSelectedItem());
				
			}
		});
		
		
		
	}
	
	private Object getAttributeValues(XAttribute xAttrib) 
	{
		if (xAttrib instanceof XAttributeBoolean)
			return((XAttributeBoolean)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeContinuous)
			return((XAttributeContinuous)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeDiscrete)
			return((XAttributeDiscrete)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeTimestamp)
			return((XAttributeTimestamp)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeLiteral)
			return((XAttributeLiteral)xAttrib).getValue();
		return null;
	}
	
	/*
	public void setTraceFilter(String attributeName)
	{
		if (attributeName!=lastSelectTraceFilter)
		{
			frame.setTraceFilter(attributeName);
			lastSelectTraceFilter=attributeName;
			traceFilterValueCbx.removeAllItems();
			traceFilterValueCbx.addItem("Please select the filter value");
		}		
	}
	*/
	public void setOutputAttribute(Augmentation attribute)
	{
		if (attribute!=lastSelectedAttribute)
		{
			frame.setOutputAttribute(attribute);
			lastSelectedAttribute=attribute;
		}		
	}
	
	public void setAttributeAugmentation(final Augmentation[] array)
	{
		try {
			frame.setEnabled(false);
			
			if (array.length==0 || !frame.configureAugmentation(array))
			{
				outputAttribCbx.removeAllItems();
				outputAttribCbx.addItem("Please augment the event log");
				setOutputAttribute(null);
				frame.createPanel(null);
				frame.setEnabled(true);
				return;
			}
			
			
			final TaskForProgressBar task2=new TaskForProgressBar(frame,"Learning Decision Tree","",0,100) {

				protected Void doInBackground() throws Exception {
					outputAttribCbx.removeAllItems();
					boolean lastSelectAttributeIsPresent=false;
					try
					{
						Arrays.sort(array, new Comparator<Augmentation>() {

							public int compare(Augmentation o1, Augmentation o2) {
								return o1.getAttributeName().compareToIgnoreCase(o2.getAttributeName());
							}

						});
					}catch(NullPointerException err)
					{
						err.printStackTrace();
					}
					ActionListener listener=outputAttribCbx.getActionListeners()[0];
					outputAttribCbx.removeActionListener(listener);
					for(Augmentation aug : array)
					{
						if (aug==lastSelectedAttribute)
							lastSelectAttributeIsPresent=true;
						outputAttribCbx.addItem(aug);
					}
					outputAttribCbx.addActionListener(listener);
					if (lastSelectAttributeIsPresent)
					{
						Augmentation aux = lastSelectedAttribute;
						lastSelectedAttribute=null;
						outputAttribCbx.setSelectedItem(aux);
					}
					else if (array.length > 0)
						outputAttribCbx.setSelectedIndex(0);
					frame.createPanel(this);
					frame.setEnabled(true);
					return null;
				}				

			};

			TaskForProgressBar task1=new TaskForProgressBar(frame,"Creating decision-tree training instances","",0,100) {

				private boolean outcome;

				protected Void doInBackground() throws Exception {
					outcome=frame.augmentLog(this);
					return null;
				}

				protected void done() {
					if (outcome)
						task2.execute();
					else
					{
						frame.setEnabled(true);
						myProgress(100);
					}

				}

			};

			task1.execute();




		} catch (Exception e) {
			e.printStackTrace();
			frame.setEnabled(true);
		}
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		// TODO Auto-generated method stub
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Configuration";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth();
	}

	public void setParent(ScalableViewPanel viewPanel) {

	}

	public void setScalableComponent(ScalableComponent scalable) {

	}

	public void willChangeVisibility(boolean to) {
		
	}

	public void updated() {

	}

	public boolean prunedTree() {
		return pruneBox.isSelected();
	}

	public float getConfidenceThreshold() {
		return (float) confThreshold.getValue();
	}

	public int getMinNumInstancePerLeaf() {
		return minNumInstancePerLeaf.getValue();
	}

	public int getNumFoldErrorPruning() {
		return numFoldErrorPruning.getValue();
	}
	
	public int getNumFoldCrossValidation() {
		return numFoldCrossValidation.getValue();
	}
	private Date getDate(String strDate){
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date retDate;
		try{
	        retDate = df.parse(strDate);
	    }
	    catch ( Exception ex ){
	    	retDate = null;
	    }
		return retDate;
	}
	
	public Date getTraceStartTime(){
		String dateString = traceStartTime.getText();
		Date retDate = getDate(dateString);
		return retDate;
	}
	
	public Date getTraceEndTime(){
		String dateString = traceEndTime.getText();
		Date retDate = getDate(dateString);
		return retDate;
	}
	
	public boolean isRegressionTree()
	{
		return regRBtn.isSelected();
	}

	public boolean binaryTree() {
		return binaryBox.isSelected();
	}

	public boolean saveData() {
		return saveDataBox.isSelected();
	}

	public boolean arffFile(){
		return arffFile.isSelected();
	}
	
	public String attributeFilterName(){
		//return traceFilterCbx.getSelectedItem().toString();
		String s = traceFilterCbx.getSelectedItem().toString();
		if (s.equals(traceFilterCbxDefaultValue))
			return null;
		else
			return traceFilterCbx.getSelectedItem().toString();
	}
	
	public Object attributeFilterValue(){
		Object o = traceFilterValueCbx.getSelectedItem();
		if (o.toString().equals(traceFilterValueCbxDefaultValue))
			return null;
		else
			return traceFilterValueCbx.getSelectedItem();
	}
	
	public boolean considerFirstActivityOnly(){
		return considerFirstActivityOnly.isSelected();
	}
	public boolean isNormalVisualizationSelected() {
		return normVisBtn.isSelected();
	}

}
