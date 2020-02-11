package org.processmining.prediction.Augmentation;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class Group extends Augmentation implements ActivityLevelAugmentation {

	public Group() {
		super("Executor Group");
	}

	public void reset(XTrace trace) {

	}

	public void setLog(XLog log) {
		// TODO Auto-generated method stub

	}

	public Object returnAttribute(XEvent event) {
		String group=org.deckfour.xes.extension.std.XOrganizationalExtension.instance().extractGroup(event);
		if (group!=null)
			return group;
		else
			return "NOT SET";

	}

}
