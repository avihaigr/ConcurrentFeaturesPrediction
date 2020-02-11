package org.processmining.prediction.Augmentation;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import javax.swing.JOptionPane;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;
import org.processmining.prediction.Predictor;



public class ResourceWorkload extends Augmentation implements ActivityLevelAugmentation {
	private class PairComparator implements java.util.Comparator<Pair<String,Long>>
	{
		public int compare(Pair<String, Long> arg0, Pair<String, Long> arg1) {
			return arg0.getSecond().compareTo(arg1.getSecond());
		}
		
	}
	
	//If execution is supposed to be sequential, trw=null;
	private TotalResourceWorkload trw=null;
	private Map<Long,Map<String,Integer>> workload=new HashMap<Long,Map<String,Integer>>();
	private XLog log;
	private boolean workloadToBeComputed=true;

	public ResourceWorkload(TotalResourceWorkload trw) {
		super("Resource-Workload");
		if (trw==null)
			throw new IllegalArgumentException("The parameter cannot be null");
		this.trw=trw;
	}

	public void reset(XTrace trace) {
		
	}

	public void setLog(XLog log) {
		this.log=log;
		workloadToBeComputed=true;
	}
	
	private Pair<Integer, Integer> computeTimestamps()
	{
		//if (trw.isStartEventPresent())
			return computeTimestampsNoStartEvent();
		//else
		//	return computeTimestampsNoStartEvent();
	}
	
	
	private Pair<Integer,Integer> computeTimestampsNoStartEvent()
	{
		PriorityQueue<Pair<String,Long>> sortedTimestamps=
				new PriorityQueue<Pair<String,Long>>(log.size()*log.get(0).size(),new PairComparator());
		int i=0;
		int noResource=0;
		int noTime=0;
		for(XTrace trace : log)
		{
			for (XEvent event : trace)
			{
				String transitionName=XLifecycleExtension.instance().extractTransition(event);
				if (transitionName!=null && transitionName.equals("complete"))
				{
					String resource=XOrganizationalExtension.instance().extractResource(event);
					Date time = XTimeExtension.instance().extractTimestamp(event);
					if (time==null)
						noTime++;
					else
					{
						long timestamp=time.getTime();
						String activityName=XConceptExtension.instance().extractName(event);
						
						if(activityName==null || !activityName.equals(Predictor.CASE_ACTIVITY))
						{
							if (resource!=null)
								sortedTimestamps.add(new Pair<String,Long>(resource,timestamp));
							else
								noResource++;
						}
					}
				}
			}
		}
			Pair<String,Long>[] array=sortedTimestamps.toArray(new Pair[0]);
			int j,k;
			for(i=0;i<array.length;i++)
			{
				int resWorkLoad=1;
				
				//debug
				if (array[i].getSecond() ==1326790800000L){
					int kkkk =0;
				}
				for(j=(i-1);j>=0;j--)
				{
					if (array[i].getSecond()-array[j].getSecond()>trw.getTimeWindow())
						break;
					else if (array[i].getFirst().equals(array[j].getFirst()))
						resWorkLoad++;

				}
				for(k=(i+1);k < array.length ;k++)
				{
					if (array[k].getSecond()-array[i].getSecond()>trw.getTimeWindow())
						break;
					else if (array[i].getFirst().equals(array[k].getFirst()))
						resWorkLoad++;
				}
				Map<String, Integer> map = workload.get(array[i].getSecond()); 
				if (map==null)
				{
					map=new HashMap<String,Integer>();
					workload.put(array[i].getSecond(), map);
				}
				map.put(array[i].getFirst(),resWorkLoad);
			}
		return new Pair<Integer,Integer>(noResource,noTime);
	}

	public Object returnAttribute(XEvent event) {
		if (workloadToBeComputed)
		{
			Pair<Integer, Integer> retValue=computeTimestamps();
			if(retValue.getFirst()>0)
			{
				JOptionPane.showMessageDialog(null, 
						"The workload computation is approximate since "+retValue.getFirst()+" events have no associated resource and "
						+retValue.getSecond()+" have no timestamps.");
			}
			workloadToBeComputed=false;
		}
		String resource=XOrganizationalExtension.instance().extractResource(event);
		if (XTimeExtension.instance().extractTimestamp(event)==null)
			return null;
		long timestamp=XTimeExtension.instance().extractTimestamp(event).getTime();
		Map<String, Integer> element = this.workload.get(timestamp);
		if (element!=null && resource!=null)
		{
			if (element.containsKey(resource)){
				int workload=element.get(resource);
				return workload;
			}
			else{
				//for debug
				String transition=XLifecycleExtension.instance().extractTransition(event);
				return null;
			}
		}
		return null;
	}


}
