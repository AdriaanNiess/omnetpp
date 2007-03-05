package org.omnetpp.common.canvas;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Composite;

/**
 * Extends CachingCanvas with zoom handling capabilities. Dragging and
 * mouse wheel zooming behaviours also added.
 *
 * @author andras
 */
//FIXME add tooltip support
public abstract class ZoomableCachingCanvas extends CachingCanvas {

	private static final int MAXPIX = Integer.MAX_VALUE / 2;
	
	private double zoomX = 0; // pixels per coordinate unit
	private double zoomY = 0; // pixels per coordinate unit
	
	private double minX = 0, maxX = 1;
	private double minY = 0, maxY = 1;

	/**
     * Constructor.
     */
	public ZoomableCachingCanvas(Composite parent, int style) {
		super(parent, style);
	}

	public void setArea(double minX, double minY, double maxX, double maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = Math.max(minX, maxX);
		this.maxY = Math.max(minY, maxY);
		
		// don't allow zero width/height (as it will cause division by zero)
		if (this.minX == this.maxX)  this.maxX = this.minX + 1;
		if (this.minY == this.maxY)  this.maxY = this.minY + 1;
		
		updateVirtualSize();
		clearCanvasCache();
		redraw();
		//System.out.printf("Area set: (%g, %g, %g, %g) - virtual size: (%d, %d)\n", this.minX, this.maxX, this.minY, this.maxY, getVirtualWidth(), getVirtualHeight());
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}

	public double getMinX() {
		return minX;
	}

	public double getMinY() {
		return minY;
	}

	protected int getLeftInset() {
		return getViewportRectangle().x - getClientArea().x;
	}

	protected int getTopInset() {
		return getViewportRectangle().y - getClientArea().y;
	}
	
	protected Insets getInsets() {
		org.eclipse.swt.graphics.Rectangle clientArea = getClientArea();
		org.eclipse.swt.graphics.Rectangle viewport = getViewportRectangle();
		return new Insets(
				viewport.y - clientArea.y, 
				viewport.x - clientArea.x,
				clientArea.y + clientArea.height - viewport.y - viewport.height,
				clientArea.x + clientArea.width - viewport.x - viewport.width);
	}

	public double fromCanvasX(int x) {
		return (x + getViewportLeft() - getLeftInset()) / zoomX + minX;
	}

	public double fromCanvasY(int y) {
		return maxY - (y + getViewportTop() - getTopInset()) / zoomY;
	}

	public double fromCanvasDistX(int x) {
		return x / zoomX;
	}

	public double fromCanvasDistY(int y) {
		return y / zoomY;
	}
	
	public int toCanvasX(double xCoord) {
		double x = (xCoord - minX)*zoomX - getViewportLeft() + getLeftInset();
		return x<-MAXPIX ? -MAXPIX : x>MAXPIX ? MAXPIX : (int)x;
	}

	public int toCanvasY(double yCoord) {
		double y = (maxY - yCoord)*zoomY - getViewportTop() + getTopInset();
		return y<-MAXPIX ? -MAXPIX : y>MAXPIX ? MAXPIX : (int)y;
	}

	public int toCanvasDistX(double xCoord) {
		double x = xCoord * zoomX;
		return x<-MAXPIX ? -MAXPIX : x>MAXPIX ? MAXPIX : (int)x;
	}

	public int toCanvasDistY(double yCoord) {
		double y = yCoord * zoomY;
		return y<-MAXPIX ? -MAXPIX : y>MAXPIX ? MAXPIX : (int)y;
	}

	public long toVirtualX(double xCoord) {
		double x = (xCoord - minX)*zoomX;
		return (long)x;
	}

	public long toVirtualY(double yCoord) {
		double y = (maxY - yCoord)*zoomY;
		return (long)y;
	}

	public double fromVirtualX(long x) {
		return x / zoomX + minX;
	}

	public double fromVirtualY(long y) {
		return maxY - y / zoomY;
	}
	
	public double getViewportCenterCoordX() {
		int middleX = getViewportWidth() / 2;
		return fromCanvasX(middleX + getLeftInset());
	}

	public double getViewportCenterCoordY() {
		int middleY = getViewportHeight() / 2;
		return fromCanvasY(middleY + getTopInset());
	}
	
	public void centerXOn(double xCoord) {
		scrollHorizontalTo(toVirtualX(xCoord) - getViewportWidth()/2);
	}

	public void centerYOn(double yCoord) {
		scrollVerticalTo(toVirtualY(yCoord) - getViewportHeight()/2);
	}
	
	public void setZoomX(double zoomX) {
		checkAreaAndViewPort();
		double minZoomX = getViewportWidth() / (maxX - minX);
		double newZoomX = Math.max(zoomX, minZoomX);
		if (newZoomX != this.zoomX) {
			double oldX = getViewportCenterCoordX();
			this.zoomX = newZoomX;
			updateVirtualSize(); // includes clearCache + redraw
			centerXOn(oldX);
			System.out.println("zoomX set to "+zoomX);
		}
	}

	public void setZoomY(double zoomY) {
		checkAreaAndViewPort();
		double minZoomY = getViewportHeight() / (maxY - minY);
		double newZoomY = Math.max(zoomY, minZoomY);
		if (newZoomY != this.zoomY) {
			double oldY = getViewportCenterCoordY();
			this.zoomY = newZoomY;
			updateVirtualSize(); // includes clearCache + redraw
			centerYOn(oldY);
			System.out.println("zoomY set to "+zoomY);
		}
	}

	public double getZoomX() {
		return zoomX;
	}

	public double getZoomY() {
		return zoomY;
	}
	
	public void zoomXBy(double zoomFactor) {
		setZoomX(zoomX * zoomFactor);	
	}

	public void zoomYBy(double zoomFactor) {
		setZoomY(zoomY * zoomFactor);	
	}

	public void zoomBy(double zoomFactor) {
		zoomXBy(zoomFactor);
		zoomYBy(zoomFactor);
	}

	public void zoomXBy(double zoomFactor, int canvasX) {
		zoomXBy(zoomFactor);
		centerXOn(fromCanvasX(canvasX));
	}

	public void zoomYBy(double zoomFactor, int canvasY) {
		zoomYBy(zoomFactor);
		centerYOn(fromCanvasY(canvasY));
	}
	
	public void zoomBy(double zoomFactor, int canvasX, int canvasY) {
		zoomXBy(zoomFactor, canvasX);
		zoomYBy(zoomFactor, canvasY);
	}
	
	public void zoomToFitX() {
		setZoomX(getViewportWidth() / (maxX - minX));
	}

	public void zoomToFitY() {
		setZoomY(getViewportHeight() / (maxY - minY));
	}
	
	public void zoomToFit() {
		zoomToFitX();
		zoomToFitY();
	}

	public void zoomToRectangle(Rectangle r) {
		// remember top-left corner
		double x = fromCanvasX(r.x);
		double y = fromCanvasY(r.y);

		// adjust zoom
		zoomXBy((getViewportWidth()) / r.width);
		zoomYBy((getViewportHeight()) / r.height);
		
		// position to original top-left corner
		scrollHorizontalTo(toVirtualX(x));
		scrollVerticalTo(toVirtualY(y));
	}
	
	public boolean isZoomedOutX() {
		return zoomX == getViewportWidth() / (maxX - minX);
	}

	public boolean isZoomedOutY() {
		return zoomY == getViewportHeight() / (maxY - minY);
	}

	/**
	 * Ensure canvas is not zoomed out more than possible (area must fill viewport).  
	 */
	public void validateZoom() {
		setZoomX(getZoomX());
		setZoomY(getZoomY());
	}
	
	/**
	 * Called internally whenever zoom or the area changes.
	 */
	private void updateVirtualSize() {
		double w = (maxX - minX)*zoomX;
		double h = (maxY - minY)*zoomY;
		setVirtualSize((long)w, (long)h);
		clearCanvasCache();
		redraw();
	}

	public void clearCanvasCacheAndRedraw() {
		clearCanvasCache();
		redraw();
	}

	private void checkAreaAndViewPort() {
		if (minX == maxX || minY == maxY)
			throw new IllegalStateException("area width/height is zero (setArea() not called yet?)");
		if (getViewportWidth() == 0 || getViewportHeight() == 0)
			throw new IllegalStateException("viewport size is zero (not yet set?)");
	}
}
