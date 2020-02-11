package org.processmining.prediction.newPrefuseTreeVis;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import prefuse.Constants;
import prefuse.render.EdgeRenderer;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

public class NewEdgeRenderer extends EdgeRenderer {
	protected Ellipse2D m_box = new Ellipse2D.Double();
	protected EdgeRenderer m_edgeRenderer;
	private String m_labelName = "name";

	public NewEdgeRenderer(int edgeTypeCurve) {
		m_edgeRenderer = new EdgeRenderer(edgeTypeCurve, Constants.EDGE_ARROW_FORWARD);
	}

	public void render(Graphics2D g, VisualItem item) {
		m_edgeRenderer.render(g, item);
		//		String text = getText(item);
		//		// render text
		//		//int textColor = ColorLib.color(Color.BLACK); // item.getTextColor() 
		//		int textColor = item.getTextColor(); 
		//		if (text != null && ColorLib.alpha(textColor) > 0 ) {
		//			
		//		}
	}

	protected String getText(VisualItem item) {
		EdgeItem edge = (EdgeItem) item;
		VisualItem item1 = edge.getSourceItem();
		VisualItem item2 = edge.getTargetItem();

		String t1 = null, t2 = null;
		if (item1.canGetString(m_labelName)) {
			t1 = item1.getString(m_labelName).substring(0, 1);
		}
		;
		if (item2.canGetString(m_labelName)) {
			t2 = item2.getString(m_labelName).substring(0, 1);
		}
		;
		if (t1 != null && t2 != null)
			return t1 + "-" + t2;
		else
			return null;
	}

	protected void getAlignedPoint(Point2D p, VisualItem item, double w, double h, int xAlign, int yAlign) {
		double x = 0, y = 0;

		EdgeItem edge = (EdgeItem) item;
		VisualItem item1 = edge.getSourceItem();
		VisualItem item2 = edge.getTargetItem();

		// label is positioned to the center of the edge
		x = (item1.getX() + item2.getX()) / 2;
		y = (item1.getY() + item2.getY()) / 2;
	}
}
