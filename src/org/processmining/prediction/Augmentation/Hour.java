package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Hour extends Augmentation{
	public static final String TIMESTAMP_NAME="Activity Hour";

	public Hour() {
		super(TIMESTAMP_NAME);
	}

	public void reset(XTrace trace) {
		
	}

	public void setLog(XLog log) {
		
	}

	public Integer returnAttribute(XEvent event) {
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		

		if (timestamp!=null){
			//0 through 23
			int hour = timestamp.getHours();
			return hour;
		}
		else
			return null;
	}

}
