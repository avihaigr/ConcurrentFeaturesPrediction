package org.processmining.prediction.Augmentation;

import java.util.ListIterator;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.DataConformance.Alignment;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.plugins.DataConformance.framework.ExecutionStep;
import org.processmining.plugins.DataConformance.visualization.DataAwareStepTypes;


public class AlignmentMove extends Augmentation implements ActivityLevelAugmentation 
{
	private ResultReplay resReplay;
	private Alignment align;
	private int numMoves;
	private String moveTypes;
	private ListIterator<ExecutionStep> logIter;
	private ListIterator<ExecutionStep> processIter;
	private ListIterator<DataAwareStepTypes> stepIter;
	private String activityName;
	public static final String MOVE_MODEL="Model Move";
	public static final String MOVE_LOG="Log Move";
	public static final String MOVE_BOTH_OK="Synchronous Move";
	public static final String MOVE_BOTH_NOK="Synchronous Move with wrong write operations";
	

	public AlignmentMove(ResultReplay resReplay,String activity, String moveTypes) {
		super(moveTypes+" for "+activity);
		this.resReplay=resReplay;
		this.moveTypes=moveTypes;
		this.activityName=activity;
	}

	public void reset(XTrace trace) {
		String traceName=XConceptExtension.instance().extractName(trace);
		align=resReplay.getAlignmentByTraceName(traceName);
		numMoves=0;

		logIter=align.getLogTrace().listIterator();
		processIter=align.getProcessTrace().listIterator();
		stepIter=align.getStepTypes().listIterator();
	}

	public void setLog(XLog log) {
	}

	public Object returnAttribute(XEvent event) {
		String lifeCycleTransition=XLifecycleExtension.instance().extractTransition(event);
		if (lifeCycleTransition!=null && lifeCycleTransition.equalsIgnoreCase("start"))
		{
			return null;
		}
		DataAwareStepTypes stepType;
		String activityName=null;

			do
			{
				if (!stepIter.hasNext())
					break;
				stepType = stepIter.next();
				ExecutionStep logMove = logIter.next();
				ExecutionStep processMove = processIter.next();
				
				switch(stepType)
				{
					case L :
					case LMGOOD :
					case LMNOGOOD :
						activityName=logMove.getActivity();
						break;
					case MINVI :
					case MREAL :
						activityName=processMove.getActivity();
						break;
					default : 
						assert(false);
				}
				if (this.activityName.equals(activityName))
				{
					if ((stepType==DataAwareStepTypes.MINVI || stepType==DataAwareStepTypes.MREAL) && moveTypes==MOVE_MODEL)
						numMoves++;	
					else if (stepType==DataAwareStepTypes.LMGOOD  && moveTypes==MOVE_BOTH_OK)
						numMoves++;	
					else if (stepType==DataAwareStepTypes.LMNOGOOD  && moveTypes==MOVE_BOTH_NOK)
						numMoves++;	
					else if (stepType==DataAwareStepTypes.L  && moveTypes==MOVE_LOG)
						numMoves++;

					
				}
				
			}
			while(stepType==DataAwareStepTypes.MINVI || stepType==DataAwareStepTypes.MREAL);

		return numMoves;
	}
}
