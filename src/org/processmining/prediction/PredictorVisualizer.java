package org.processmining.prediction;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;


public class PredictorVisualizer {
	@Plugin(
			name="Prediction", 
			level=PluginLevel.PeerReviewed,
			parameterLabels={"Prediction"}, 
			returnLabels={"JComponent"}, 
			returnTypes={JComponent.class}, 
			userAccessible=true
			)
	@Visualizer
	
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Massimiliano", 
            email = "m.d.leoni@tue.nl")		
	public JComponent visualizePrediction(PluginContext context, Predictor prediction) throws Exception{
		return new DecisionTreePanel(context,prediction);
	}
}
