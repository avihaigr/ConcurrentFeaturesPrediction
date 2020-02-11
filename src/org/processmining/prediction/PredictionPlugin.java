package org.processmining.prediction;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.DataConformance.ResultReplay;

	@Plugin(
			name="Perform Predictions of Business Process Features", 
			level=PluginLevel.PeerReviewed,
			parameterLabels={"XESLog","Result Replay"}, 
			returnLabels={"Predictor"}, 
			returnTypes={Predictor.class}, 
			userAccessible=true,
			categories=PluginCategory.Analytics,
			help="Provides a correlation of different process characteristics. It "
			+" unifies a number of approaches for correlation analysis proposed in literature, "
			+ "proposing a general solution that can perform those analyses and many more. "
			+ "This plug-in is the reference implementation of the framework discussed in paper "
			+ "\"A general process mining framework for correlating, predicting and clustering dynamic behavior based on event logs\""
			+ " (doi:10.1016/j.is.2015.07.003), which, in turn, extends the initial framework proposed in paper "
			+ "\"A General Framework for Correlating Business Process Characteristics\" accepted for BPM '14. "
			)
public class PredictionPlugin {

	@PluginVariant(variantLabel="Without Replay Result",requiredParameterLabels = {0})
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Massimiliano de Leoni", 
            email = "m.d.leoni@tue.nl")		
	public Predictor performPrediction(PluginContext context, XLog log) throws Exception{
		Predictor predict=new Predictor(log);
		return predict;
	}

	@PluginVariant(variantLabel="With Replay Result",requiredParameterLabels = {0, 1})
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Massimiliano de Leoni", 
            email = "m.d.leoni@tue.nl")		
	
	public Predictor performPrediction(PluginContext context, XLog log,ResultReplay resReplay) throws Exception{
		Predictor predict=new Predictor(log,resReplay);
		return predict;
	}	
	
	/*@PluginVariant(variantLabel="With Data-Aware Declare Compliance Check Result",requiredParameterLabels = {0, 1})
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Massimiliano de Leoni, M.H.M. Schouten", 
            email = "m.d.leoni@tue.nl, m.h.m.schouten0@gmail.com")		
	
	public Predictor performPrediction(PluginContext context, XLog log,ComplianceCheckerOutput compOut) throws Exception{
		Predictor predict=new Predictor(Predictor.updateLog(log, compOut),compOut);
		return predict;
	}*/	

}
