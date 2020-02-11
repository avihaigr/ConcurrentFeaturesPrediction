package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class TraceEndTime extends Augmentation {

	private Date lastEventTimeStamp;

	public TraceEndTime() {
		super("Trace End Time");
	}

	public void reset(XTrace trace) {
		lastEventTimeStamp=XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1));
	}

	public Object returnAttribute(XEvent event) {
		return lastEventTimeStamp;
	}
 
	public void setLog(XLog log) {
		
	}

}
