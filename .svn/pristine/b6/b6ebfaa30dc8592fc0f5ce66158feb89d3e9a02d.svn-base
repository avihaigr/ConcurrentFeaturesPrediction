package org.processmining.prediction.Augmentation;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;

public class EventNumber extends Augmentation {

	private int numExecution=1;

	public EventNumber(boolean caseLevel)
	{
		super(caseLevel ? "Trace Length" : "Event_Number");
	}	
	
	public void reset(XTrace trace) {
		numExecution=1;

	}

	public void setLog(XLog log) {

	}

	public Integer returnAttribute(XEvent event) {
		if (!Predictor.CASE_ACTIVITY.equals(XConceptExtension.instance().extractName(event).equals(Predictor.CASE_ACTIVITY)))
			return(++numExecution);
		else
			return null;
	}

}
