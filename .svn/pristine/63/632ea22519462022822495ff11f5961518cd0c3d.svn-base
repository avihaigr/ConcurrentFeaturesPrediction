package org.processmining.prediction;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryBufferedImpl;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.prediction.Augmentation.Timestamp;


public class ExtractLog {
	@Plugin(
			name="Extract Augmented Log", 
			parameterLabels={"Predictor"}, 
			returnLabels={"XESLog"}, 
			returnTypes={XLog.class}, 
			userAccessible=true
			)
	
	@UITopiaVariant(
			affiliation = "TU/e", 
            author = "Massimiliano de Leoni", 
            email = "m.d.leoni@tue.nl")		
	public XLog performPrediction(PluginContext context, Predictor predictor) throws Exception{
		XFactory factory=new XFactoryBufferedImpl();
		XLog oldLog=predictor.getLog();
		XLog newLog=factory.createLog(oldLog.getAttributes());
		for(XTrace oldTrace : oldLog)
		{
			XTrace newTrace=factory.createTrace(oldTrace.getAttributes());
			for(XEvent oldEvent : oldTrace)
			{
				if (!XConceptExtension.instance().extractName(oldEvent).equals(Predictor.CASE_ACTIVITY))
				{
					XEvent newEvent=factory.createEvent(oldEvent.getAttributes());
					XAttributeTimestamp attr=(XAttributeTimestamp) oldEvent.getAttributes().get(Timestamp.TIMESTAMP_NAME);
					if (attr!=null)
						XTimeExtension.instance().assignTimestamp(newEvent, attr.getValue());
					newTrace.add(newEvent);
				}
			}
			newLog.add(newTrace);
		}
		return newLog;
	}
}
