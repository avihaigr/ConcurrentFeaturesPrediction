package org.processmining.prediction.Augmentation;

import java.util.Date;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class DayOfWeek extends Augmentation{
	public static final String TIMESTAMP_NAME="Activity Day Of Week";

	public DayOfWeek() {
		super(TIMESTAMP_NAME);
	}

	public void reset(XTrace trace) {
		
	}

	public void setLog(XLog log) {
		
	}

	public Integer returnAttribute(XEvent event) {
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		

		if (timestamp!=null){
			//Returns the day of the week represented by this date. 
			//The returned value (0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday) 
			//represents the day of the week that contains or begins with the instant in time represented by this Date object, 
			//as interpreted in the local time zone.
			int dayofweek = timestamp.getDay();
			return dayofweek;
		}
		else
			return null;
	}

}
