package org.processmining.prediction.Augmentation;

import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.prediction.Predictor;

public class PreviousPath extends Augmentation implements ActivityLevelAugmentation {

	private XTrace trace=null;
	private int currPos=0;
	private int neededPathLength = 0;
//	private String prevPath = "";
	private LinkedList<String> pathElements = null;
	
	public PreviousPath() {
		super("PreviousPath");
	}

	@Override
	public String[] getDefaultValueForParameter(int i)
	{
		String[] defaultValues=new String[] {String.valueOf(neededPathLength)};
		return(defaultValues);
	}
	
	@Override
	public String[] getPossibleValuesForParameter(int i)
	{
		return(null);
	}	
	
	@Override
	public String[] getParameterNames()
	{
		return new String[] {"Previous Path Length"};
	}
	
	@Override
	public boolean setParameter(int param, String value[])
	{
		try {
			neededPathLength =Integer.parseInt(value[0]);

		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public void reset(XTrace trace) {
		this.trace=trace;
		currPos=0;
	//	prevPath = "";
		pathElements = new LinkedList<String>();
	}

	public Object returnAttribute(XEvent event) {
		String attrValue;
		String pathArray= null;
		if (currPos == 0 )
		{
			attrValue="NOTHING";
		}
		else
		{
			XEvent previousEvent=trace.get(currPos-1);		
			attrValue=XConceptExtension.instance().extractName(previousEvent);
			if (attrValue==null)
				attrValue="";
			if (attrValue.equals(Predictor.CASE_ACTIVITY))
				attrValue="NOTHING";
			
			//if 0 then there is no limit to the path length
			if (neededPathLength != 0)
				if (pathElements.size() == neededPathLength){
					//remove the first element in the path
					pathElements.poll();
				}
			
			pathElements.add(attrValue);
			//String[] pathArray = pathElements.toArray(new String[0]);
			pathArray = StringUtils.join(pathElements,",");
			//pathArray.
			//prevPath.spl
			//prevPath.join(",", attrValue);	
		}
		currPos++;
		return pathArray;
	}

	public void setLog(XLog log) {
		
	}

}
