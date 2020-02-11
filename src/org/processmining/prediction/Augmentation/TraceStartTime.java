package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class TraceStartTime extends Augmentation {

	private Date firstEventTimeStamp;

	public TraceStartTime() {
		super("Trace Start Time");
	}

	public void reset(XTrace trace) {
		firstEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(0));
	}

	public Object returnAttribute(XEvent event) {
		return firstEventTimeStamp;
	}
 
	public void setLog(XLog log) {
		
	}
	
	public boolean isIncludInPrediction(){
		return false;
	}

}
