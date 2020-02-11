package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.prediction.Augmentation.Augmentation;

public class DecisionTreePanel extends JPanel {
	private VariablePanel varPanel = null;
	private ScalableViewPanel tvScalable = null;

	private ConfigurationPanel confPanel;
	private Predictor predictor;
	private ActivitiesGrouper actPanel;
	private DiscretizationPanel discrPanel = null;
	private Augmentation outputAttribute = null;
	private String traceFilterattributeName = null;
	private int numIntervals = 10;
	private DiscrMethod method = DiscrMethod.EQUAL_WIDTH;
	private boolean recomputeIntervals;
	private PluginContext proMContext;

	public DecisionTreePanel(PluginContext context, Predictor predictor) throws Exception {
		this.predictor = predictor;
		this.proMContext = context;
		this.setLayout(new BorderLayout());
		List<String> attributes = predictor.getAttributes();
		List<String> traceAttributes = predictor.getTraceAttributes();
		
		// 1_1_2019
		Map<String, ArrayList<String>> caseAttributeValues = predictor.getCaseAttributeValues();
		
	//	List<String> resources = predictor.getResources();
		confPanel = new ConfigurationPanel(this, 1000,traceAttributes,predictor.getLog());
		varPanel = new VariablePanel(attributes,traceAttributes, predictor.getActivities(), predictor.getResources(),predictor.getResourceGroups(),caseAttributeValues, confPanel, predictor.getResReplay(),predictor.getTypes());
		actPanel = new ActivityPanel(predictor.getActivities(), varPanel);
		createPanel(null);
	}

	public void resampleOutputAttribute(int numIntervals, DiscrMethod method) {
		this.numIntervals = numIntervals;
		this.method = method;
		setOutputAttribute(outputAttribute);
		this.setEnabled(false);
		
		TaskForProgressBar task1=new TaskForProgressBar(this,"Learning Decision Tree","",0,100) {

			protected Void doInBackground() throws Exception {
				((DecisionTreePanel)component).createPanel(this);
				return null;
			}

			protected void done() {
				((DecisionTreePanel)component).setEnabled(true);
			}

		};

		task1.execute();

	}
/*
	public void setTraceFilter(String attributeName)
	{
		this.traceFilterattributeName = attributeName;
		recomputeIntervals = true;		
	}
	*/
	public void setOutputAttribute(Augmentation attribute) {
		this.outputAttribute = attribute;
		recomputeIntervals = true;
	}

	public void setConsiderFirstActivityOnly(boolean considerFirstActivityOnly) {
		predictor.setConsiderFirstActivityOnly(considerFirstActivityOnly);
		recomputeIntervals = true;
	}
	
	public void setAttributeFilterName(String attributeFilterName){
		predictor.setAttributeFilterName(attributeFilterName);
		recomputeIntervals = true;
	}
	
	public void setAttributeFilterValue(Object attributeFilterValue){
		predictor.setAttributeFilterValue(attributeFilterValue);
		recomputeIntervals = true;
	}	
	
	public void setTraceStartTime(Date traceStartTime){
		predictor.setTraceStartTime(traceStartTime);
		recomputeIntervals = true;
	}
	
	public void setTraceEndTime(Date traceEndTime){
		predictor.setTraceEndTime(traceEndTime);
		recomputeIntervals = true;
	}
	
	
	
	public boolean augmentLog(TaskForProgressBar task) {
		


        final Augmentation[] augmentations=varPanel.getSelectedItems().toArray(new Augmentation[0]);
        if (augmentations==null)
        	return false;
        
        predictor.setActivitiesToConsider(actPanel.getActivitiesToConsider());
        return predictor.augmentLog(augmentations,varPanel.isMapDBSelected(),task);
        
	}
	
	public void setEnabled(boolean aFlag) {
		super.setEnabled(aFlag);
		confPanel.setEnabled(aFlag);
		if (discrPanel!=null)
		{
			discrPanel.setEnabled(aFlag);
			setEnabled(discrPanel,false);
		}
		if (varPanel!=null)
		{
			varPanel.setEnabled(aFlag);
			setEnabled(varPanel,aFlag);
		}
		if (tvScalable!=null)
		{
			tvScalable.setEnabled(aFlag);
			setEnabled(tvScalable,aFlag);
		}
		setEnabled(confPanel,aFlag);
		
	};
	
	

	private void setEnabled(Container container, boolean aFlag) {
		for(Component comp : container.getComponents())
		{
			comp.setEnabled(aFlag);
			if (comp instanceof Container)
				setEnabled((Container) comp,aFlag);
		}
		
	}

	@SuppressWarnings("unchecked")
	public void createPanel(TaskForProgressBar task) throws Exception {
		JComponent tvaux;
		boolean ok = false;
		if(task!=null)
			task.myProgress(0);
		if (tvScalable != null)
			this.remove(tvScalable);
		if (predictor.getTypes().isEmpty()) {
			tvaux = new JLabel("Impossible to build a decision tree if no output feature is chosen");
			tvaux.setFont(tvaux.getFont().deriveFont(24F).deriveFont(Font.BOLD));
			discrPanel = null;
		} else {
			try {
				/*
				predictor.setConsiderFirstActivityOnly(confPanel.considerFirstActivityOnly());
				predictor.setAttributeFilterName(confPanel.attributeFilterName());
				predictor.setAttributeFilterValue(confPanel.attributeFilterValue());
				*/	
				if (recomputeIntervals) {
					DiscretizationInterval[] intervals = predictor.setOutputAttribute(outputAttribute, numIntervals,
							method, confPanel.isRegressionTree());
					if (intervals != null) {
						discrPanel = new DiscretizationPanel(this, intervals, numIntervals, method);
					} else
						discrPanel = null;
					recomputeIntervals = false;
				}
				predictor.setUnPruned(!confPanel.prunedTree());
				predictor.setConfidenceThreshold(confPanel.getConfidenceThreshold());
				predictor.setMinNumInstancePerLeaf(confPanel.getMinNumInstancePerLeaf());
				predictor.setNumFolds(confPanel.getNumFoldErrorPruning());
				predictor.setNumFoldCrossValidation(confPanel.getNumFoldCrossValidation());
				predictor.setBinarySplit(confPanel.binaryTree());
				predictor.setSaveData(confPanel.saveData());
				predictor.setArffFile(confPanel.arffFile());
								
				predictor.setActivitiesToConsider(actPanel.getActivitiesToConsider());
				predictor.makePrediction(task);
				if (confPanel.isNormalVisualizationSelected())
					tvaux = predictor.getNormalTreeVisualization();
				else
					tvaux = predictor.getPrefuseTreeVisualization();
				ok = true;
			} catch (weka.core.UnsupportedAttributeTypeException e) {
				tvaux = new JLabel(
						"Impossible to construct a decision tree: the dependent feature has only taken on one single value");
				tvaux.setFont(tvaux.getFont().deriveFont(24F).deriveFont(Font.BOLD));
			} catch (Exception e) {
				if (e.getMessage()!=null)
				{
					e.printStackTrace();
					tvaux = new JLabel(e.getMessage());
				}
				else
				{
					if (outputAttribute==null)
						tvaux = new JLabel("Impossible to construct a decision tree if the event log is not augmented.");
					else
					{
						tvaux = new JLabel("Impossible to construct a decision tree because of some internal error. Try to change"
								+ " the dependent feature");	
						e.printStackTrace();
					}
				}
				tvaux.setFont(tvaux.getFont().deriveFont(24F).deriveFont(Font.BOLD));
			}
			if (task!=null)
				task.myProgress(100);
		}
		final JComponent tv = tvaux;
		//tv.setBackground(Color.GRAY);
		tvScalable = new ScalableViewPanel(new ScalableComponent() {

			public void setScale(double newScale) {
			}

			public void removeUpdateListener(UpdateListener listener) {
			}

			public double getScale() {
				return 1;
			}

			public JComponent getComponent() {
				return tv;
			}

			public void addUpdateListener(UpdateListener listener) {
			}
		});
		if (varPanel != null)
			tvScalable.addViewInteractionPanel(varPanel, SwingConstants.EAST);
		if (confPanel != null)
			tvScalable.addViewInteractionPanel(confPanel, SwingConstants.EAST);
		if (ok)
			tvScalable.addViewInteractionPanel(new Summary(predictor.getEvaluation()), SwingConstants.SOUTH);
		if (actPanel != null)
			tvScalable.addViewInteractionPanel(actPanel, SwingConstants.WEST);
		if (actPanel.getActivitiesToConsider().contains(Predictor.CASE_ACTIVITY))
			tvScalable.addViewInteractionPanel(new LogClusterPanel(proMContext, predictor), SwingConstants.WEST);
		this.add(tvScalable, BorderLayout.CENTER);
		if (discrPanel != null)
			tvScalable.addViewInteractionPanel(discrPanel, SwingConstants.EAST);
		this.validate();
		this.repaint();
	}

	public void setRegressionTree(boolean b) {
		if (b != predictor.isRegressionTree()) {
			predictor.setRegression(b);
			recomputeIntervals = true;
		}

	}

	public boolean configureAugmentation(Augmentation[] array) {
		return predictor.configureAugmentation(array);
		
	}
}
