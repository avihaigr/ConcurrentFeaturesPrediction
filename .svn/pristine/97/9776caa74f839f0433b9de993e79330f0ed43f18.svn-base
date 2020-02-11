package org.processmining.prediction.Augmentation;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;

class EventComparator implements Comparator<XEvent>
{

	public int compare(XEvent e1, XEvent e2) {
		Date t1=XTimeExtension.instance().extractTimestamp(e1);
		if (t1==null)
			t1=new Date(0);
		Date t2=XTimeExtension.instance().extractTimestamp(e2);
		if (t2==null)
			t2=new Date(0);
		return t1.compareTo(t2);
	}
	
}

public class ActivityDuration extends Augmentation implements ActivityLevelAugmentation {
	private Map<Pair<String,String>,Date> startedEvent=new HashMap<Pair<String,String>, Date>();
	private Date timestampLastCompletedEventInTrace=null;
	private Map<String,TreeSet<XEvent>> eventCompletedForResources = new HashMap<String, TreeSet<XEvent>>();
	private static final EventComparator comparator=new EventComparator();
	
	public ActivityDuration() {
		super("ActivityDuration");
	}

	public void reset(XTrace trace) {
		startedEvent.clear();
		timestampLastCompletedEventInTrace=null;
	}

	public void setLog(XLog log) {
		for (XTrace trace : log)
			for (XEvent event : trace)
			{
				String transition=XLifecycleExtension.instance().extractTransition(event);
				if (XTimeExtension.instance().extractTimestamp(event)!=null)
					if (transition==null || transition.equals("complete"))
						getResourceSet(event).add(event);
			}
	}

	private TreeSet<XEvent> getResourceSet(XEvent event) {
		String resName=XOrganizationalExtension.instance().extractResource(event);
		TreeSet<XEvent> eventSet=eventCompletedForResources.get(resName);
		if (eventSet==null)
		{
			eventSet=new TreeSet<XEvent>(comparator);
			eventCompletedForResources.put(resName, eventSet);
		}
		return eventSet;
	}

	public Object returnAttribute(XEvent event) {
		String resource=XOrganizationalExtension.instance().extractResource(event);
		String activityName=XConceptExtension.instance().extractName(event);
		String transitionName=XLifecycleExtension.instance().extractTransition(event);
		Date timestamp=XTimeExtension.instance().extractTimestamp(event);
		
		if (transitionName!=null && transitionName.equals("start"))
			startedEvent.put(new Pair<String,String>(resource,activityName), timestamp);
		else if (transitionName==null || transitionName.equals("complete"))
		{
			Pair<String, String> resourceActPair=new Pair<String,String>(resource,activityName);
			Date startEventTimestamp=startedEvent.get(resourceActPair);
			if (startEventTimestamp==null)
			{
				resourceActPair=new Pair<String,String>(null,activityName);
				startEventTimestamp=startedEvent.get(resourceActPair);
			}
			long duration;
			if (startEventTimestamp!=null)
			{
				duration=(timestamp.getTime()-startEventTimestamp.getTime());
				startedEvent.remove(resourceActPair);
				return duration;
			}
			else
			{	
				TreeSet<XEvent> eventSet = eventCompletedForResources.get(resource);
				SortedSet<XEvent> headSet = eventSet.headSet(event);
				XEvent lastEventCompletedByResource=null;
				if (headSet.size()>0)
					lastEventCompletedByResource=headSet.last();
				long timeLastEventCompletedByResource,timeLastCompletedEventInTrace;
				if (lastEventCompletedByResource!=null)
					timeLastEventCompletedByResource=XTimeExtension.instance().extractTimestamp(lastEventCompletedByResource).getTime();
				else
					timeLastEventCompletedByResource=Long.MAX_VALUE;
				if (timestampLastCompletedEventInTrace!=null)
					timeLastCompletedEventInTrace=timestampLastCompletedEventInTrace.getTime();
				else
					timeLastCompletedEventInTrace=Long.MAX_VALUE;
				if (timeLastCompletedEventInTrace!=Long.MAX_VALUE || timeLastEventCompletedByResource!=Long.MAX_VALUE)
				{
					if (timestamp!=null)
						duration=timestamp.getTime()-Math.min(timeLastEventCompletedByResource, timeLastCompletedEventInTrace);
					else
						duration=-1;
				}
				else
					duration=-1;
			}
			timestampLastCompletedEventInTrace=timestamp;
			if (duration>=0)
				return duration;
		}
		return null;
	}
	
	@Override
	public boolean isTimeInterval() {
		return true;
	}	
}
