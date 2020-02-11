package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Timestamp extends Augmentation{
	public static final String TIMESTAMP_NAME="Timestamp";

	public Timestamp() {
		super(TIMESTAMP_NAME);
	}

	public void reset(XTrace trace) {
		
	}

	public void setLog(XLog log) {
		
	}

	public Object returnAttribute(XEvent event) {
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		if (timestamp!=null)
			return timestamp;
		else
			return null;
	}

}
