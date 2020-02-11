package org.processmining.prediction;

import java.io.Serializable;

import org.processmining.framework.util.Pair;

public class DiscretizationInterval extends Pair<Double, Double> implements Serializable, Cloneable {

	private static final long serialVersionUID = -7018538256206819734L;
	private String name;
	private boolean secExtremeIncluded=false;

	public DiscretizationInterval(Double first, Double second) {
		super(first, second);
		setName("["+first+","+second+"[");
	}

	public DiscretizationInterval(Double first, Double second, boolean secExtremeIncluded) {
		super(first, second);
		if (secExtremeIncluded)
		{
			setName("["+first+","+second+"]");
			this.secExtremeIncluded=secExtremeIncluded;
		}
		else
			setName("["+first+","+second+"[");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toString()
	{
		return name;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}

	public boolean isSecExtremeIncluded() {
		return secExtremeIncluded;
	}

}
