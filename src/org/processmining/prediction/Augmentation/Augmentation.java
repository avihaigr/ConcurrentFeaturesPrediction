package org.processmining.prediction.Augmentation;

import java.util.Collection;
import java.util.Date;

import javax.swing.tree.DefaultMutableTreeNode;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;

public abstract class Augmentation extends DefaultMutableTreeNode
{
	protected String attributeName;
	@SuppressWarnings("unchecked")
	public final static String notAllowedChars=".()&!|=<>-+*/% ";

	public Augmentation(String attributeName)
	
	{
		char[] array=attributeName.toCharArray();
		for(int i=0;i<array.length;i++)
		{
			if (notAllowedChars.indexOf(array[i])!=-1)
				array[i]='_';
		}
		this.attributeName=new String(array);
	}

	public void setActivitiesToConsider(Collection<String> activitiesToConsider){}
	
	public boolean isIncludInPrediction(){
		return true;
	}
	
	public abstract void reset(XTrace trace);
	
	public abstract void setLog(XLog log);
	
	public abstract Object returnAttribute(XEvent event);

	//public abstract void augmentEvent(XEvent newEvent, XEvent event);
	
	@Override
	public String toString()
	{
		return attributeName;
	}
	
	protected static XAttribute createXAttribute(String attribute, Object value)
	{
		XAttribute xAttribute=null;
		if (value instanceof Boolean)
			xAttribute=new XAttributeBooleanImpl(attribute,(Boolean) value);
		else if (value instanceof Double)
			xAttribute=new XAttributeContinuousImpl(attribute,(Double) value);
		else if (value instanceof Float)
			xAttribute=new XAttributeContinuousImpl(attribute,(Float) value);
		else if (value instanceof Long)
			xAttribute=new XAttributeDiscreteImpl(attribute,(Long) value);			
		else if (value instanceof Integer)
			xAttribute=new XAttributeDiscreteImpl(attribute,(Integer) value);
		else if (value instanceof Date)
			xAttribute=new XAttributeTimestampImpl(attribute,(Date) value);
		else if (value instanceof String)
			xAttribute=new XAttributeLiteralImpl(attribute,(String) value);
		assert(xAttribute!=null);
		return xAttribute;
	}
	
	protected static Object getAttributeValues(XAttribute xAttrib) 
	{
		if (xAttrib instanceof XAttributeBoolean)
			return((XAttributeBoolean)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeContinuous)
			return((XAttributeContinuous)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeDiscrete)
			return((XAttributeDiscrete)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeTimestamp)
			return((XAttributeTimestamp)xAttrib).getValue();
		else if (xAttrib instanceof XAttributeLiteral)
			return((XAttributeLiteral)xAttrib).getValue();
		return null;
	}

	public String getAttributeName() {
		return attributeName;
	}
	
	public boolean multipleValuesForParameter(int i)
	{
		return false;
	}
	
	public String[] getDefaultValueForParameter(int i)
	{
		throw(new IllegalArgumentException("No Parameters"));
	}
	
	public String[] getPossibleValuesForParameter(int i)
	{
		throw(new IllegalArgumentException("No Parameters"));	
	}
	

	public String[] getParameterNames() {
		return new String[0];
	}

	public boolean setParameter(int i,String value[]) {
		return true;
	}

	public boolean isTimeInterval() {
		return false;
	}	
}
