package org.processmining.prediction.Augmentation;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Executor extends Augmentation implements ActivityLevelAugmentation {

	public Executor() {
		super("Executor");
		// TODO Auto-generated constructor stub
	}

	public void reset(XTrace trace) {

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event) {
		String resource=org.deckfour.xes.extension.std.XOrganizationalExtension.instance().extractResource(event);
		if (resource==null)
			return "NOT SET";
		else
			return resource;

	}

}
