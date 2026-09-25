package com.vectorium.desktop;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JPanel;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
public final class VectorCanvas extends JPanel {
    private static final long serialVersionUID=1L;
    private static final int MARGIN=24;
    private static final int CHECKER_SIZE=12;
    private static final double MIN_ZOOM=0.1;
    private static final double MAX_ZOOM=16.0;
    private final MouseHandler mouseHandler;
    private VectorDocument document;
    private BufferedImage rasterImage;
    private BufferedImage vectorPreview;
    private double zoom=1.0;
    private double panX;
    private double panY;
    public VectorCanvas() {
        setOpaque(true);
        setBackground(DesktopTheme.CANVAS);
        setPreferredSize(new Dimension(800, 600));
        setMinimumSize(new Dimension(320, 240));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        setToolTipText("Scroll to zoom. Drag to pan.");
        getAccessibleContext().setAccessibleName("Vector preview canvas");
        getAccessibleContext().setAccessibleDescription("Displays the current raster or vector document.");
        mouseHandler=new MouseHandler(this);
        addMouseWheelListener(mouseHandler);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    public void setDocument(VectorDocument document) {
        setDocument(document, document!=null&&document.getOrigin().isRaster()&&document.getPaths().isEmpty());
    }
    public void setVectorResult(VectorDocument document) {
        setVectorResult(document, true);
    }
    public void setVectorResult(VectorDocument document, boolean rasterFallback) {
        setDocument(document, false, rasterFallback);
    }
    private void setDocument(VectorDocument document, boolean showRaster) {
        setDocument(document, showRaster, true);
    }
    private void setDocument(VectorDocument document, boolean showRaster, boolean rasterFallback) {
        this.document=document;
        rasterImage=null;
        vectorPreview=null;
        if (document!=null&&document.getWidth()>0&&document.getHeight()>0) {
            if (showRaster) {
                rasterImage=createRasterImage(document);
            } else if (document.getOrigin().isRaster()&&document.getPaths().size()>0) {
                vectorPreview=renderVectorPreview(document);
            }
        }
        if (getWidth()>0&&getHeight()>0) {
            fitToViewport();
        }
        repaint();
    }
    private BufferedImage createRasterImage(VectorDocument document) {
        BufferedImage image=new BufferedImage(document.getWidth(), document.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, document.getWidth(), document.getHeight(), document.getOwnedPixels(), 0, document.getWidth());
        return image;
    }
    private BufferedImage renderVectorPreview(VectorDocument document) {
        BufferedImage image=new BufferedImage(document.getWidth(), document.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hasCubicData(document)?RenderingHints.VALUE_ANTIALIAS_ON:RenderingHints.VALUE_ANTIALIAS_OFF);
            for (int index=0;index<document.getPaths().size();index++) {
                fillPath(graphics, document.getPaths().get(index));
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }
    public VectorDocument getDocument() {
        return document;
    }
    public void fitToViewport() {
        if (document==null||document.getWidth()<=0||document.getHeight()<=0) {
            zoom=1.0;
            panX=0.0;
            panY=0.0;
            repaint();
            return;
        }
        int availableWidth=Math.max(1, getWidth()-MARGIN*2);
        int availableHeight=Math.max(1, getHeight()-MARGIN*2);
        double horizontal=availableWidth/(double)document.getWidth();
        double vertical=availableHeight/(double)document.getHeight();
        double fitted=Math.min(horizontal, vertical);
        zoom=clamp(fitted);
        panX=(getWidth()-document.getWidth()*zoom)/2.0;
        panY=(getHeight()-document.getHeight()*zoom)/2.0;
        repaint();
    }
    public void setZoom(double value) {
        if (!Double.isFinite(value)||value<=0.0) {
            throw new IllegalArgumentException("zoom must be finite and positive");
        }
        double centerX=panX+getWidth()/(2.0*zoom);
        double centerY=panY+getHeight()/(2.0*zoom);
        zoom=clamp(value);
        panX=centerX-getWidth()/(2.0*zoom);
        panY=centerY-getHeight()/(2.0*zoom);
        repaint();
    }
    public double getZoom() {
        return zoom;
    }
    public double getPanX() {
        return panX;
    }
    public double getPanY() {
        return panY;
    }
    public void zoomAt(int anchorX, int anchorY, double factor) {
        if (!Double.isFinite(factor)||factor<=0.0) {
            throw new IllegalArgumentException("zoom factor must be finite and positive");
        }
        double worldX=(anchorX-panX)/zoom;
        double worldY=(anchorY-panY)/zoom;
        zoom=clamp(zoom*factor);
        panX=anchorX-worldX*zoom;
        panY=anchorY-worldY*zoom;
        repaint();
    }
    protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas=(Graphics2D)graphics.create();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (document==null) {
                drawEmptyState(canvas);
            } else {
                drawDocument(canvas);
            }
            if (hasFocus()) {
                canvas.setColor(DesktopTheme.ACCENT);
                canvas.drawRect(1, 1, getWidth()-3, getHeight()-3);
            }
        } finally {
            canvas.dispose();
        }
    }
    private void drawEmptyState(Graphics2D graphics) {
        graphics.setColor(DesktopTheme.TEXT);
        graphics.setFont(DesktopTheme.DISPLAY_FONT);
        FontMetrics metrics=graphics.getFontMetrics();
        String title="Open an image to begin";
        int titleX=(getWidth()-metrics.stringWidth(title))/2;
        int titleY=getHeight()/2-8;
        graphics.drawString(title, titleX, titleY);
        graphics.setColor(DesktopTheme.MUTED_TEXT);
        graphics.setFont(DesktopTheme.BODY_FONT);
        metrics=graphics.getFontMetrics();
        String detail="PNG, JPEG, and SVG";
        graphics.drawString(detail, (getWidth()-metrics.stringWidth(detail))/2, titleY+24);
    }
    private void drawDocument(Graphics2D graphics) {
        Rectangle2D page=new Rectangle2D.Double(panX, panY, document.getWidth()*zoom, document.getHeight()*zoom);
        if (document.getOrigin().isRaster()) {
            drawCheckerboard(graphics, page);
        }
        Graphics2D transformed=(Graphics2D)graphics.create();
        try {
            transformed.translate(panX, panY);
            transformed.scale(zoom, zoom);
            if (document.getOrigin().isRaster()) {
                transformed.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            }
            if (rasterImage!=null) {
                Object interpolation=zoom>=2.0?RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                transformed.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
                transformed.drawImage(rasterImage, 0, 0, null);
            } else if (vectorPreview!=null) {
                Object interpolation=zoom>=2.0?RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR:RenderingHints.VALUE_INTERPOLATION_BILINEAR;
                transformed.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
                transformed.drawImage(vectorPreview, 0, 0, null);
            } else {
                drawVectorPaths(transformed);
            }
        } finally {
            transformed.dispose();
        }
        graphics.setColor(DesktopTheme.BORDER);
        int left=(int)Math.round(page.getX());
        int top=(int)Math.round(page.getY());
        int right=(int)Math.round(page.getMaxX());
        int bottom=(int)Math.round(page.getMaxY());
        graphics.drawRect(left, top, Math.max(0, right-left), Math.max(0, bottom-top));
    }
    private void drawCheckerboard(Graphics2D graphics, Rectangle2D page) {
        int left=(int)Math.round(page.getX());
        int top=(int)Math.round(page.getY());
        int width=(int)Math.round(page.getWidth());
        int height=(int)Math.round(page.getHeight());
        graphics.setColor(DesktopTheme.SURFACE);
        graphics.fillRect(left, top, width, height);
        graphics.setColor(DesktopTheme.CANVAS);
        int startX=left-(left%CHECKER_SIZE);
        int startY=top-(top%CHECKER_SIZE);
        for (int y=startY;y<top+height;y+=CHECKER_SIZE) {
            for (int x=startX;x<left+width;x+=CHECKER_SIZE) {
                if (((x/CHECKER_SIZE+y/CHECKER_SIZE)&1)==0) {
                    graphics.fillRect(x, y, CHECKER_SIZE, CHECKER_SIZE);
                }
            }
        }
    }
    private void drawVectorPaths(Graphics2D graphics) {
        for (int index=0;index<document.getPaths().size();index++) {
            VectorPath path=document.getPaths().get(index);
            Path2D.Double shape=shape(path);
            setPathStyle(graphics, path);
            graphics.fill(shape);
            if (!document.getOrigin().isRaster()) {
                graphics.setColor(new Color(DesktopTheme.TEXT.getRed(), DesktopTheme.TEXT.getGreen(), DesktopTheme.TEXT.getBlue(), 48));
                graphics.draw(shape);
            }
        }
        graphics.setComposite(AlphaComposite.SrcOver);
    }
    private void fillPath(Graphics2D graphics, VectorPath path) {
        setPathStyle(graphics, path);
        graphics.fill(shape(path));
    }
    private void setPathStyle(Graphics2D graphics, VectorPath path) {
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)path.getOpacity()));
        graphics.setColor(new Color(path.getFill().getRed(), path.getFill().getGreen(), path.getFill().getBlue(), path.getFill().getAlpha()));
    }
    private static Path2D.Double shape(VectorPath path) {
        List<double[]> rings=path.getRings();
        double[][] cubicRings=path.getCubicRingCoordinates();
        Path2D.Double shape=new Path2D.Double();
        shape.setWindingRule(path.getFillRule().isEvenOdd()?Path2D.WIND_EVEN_ODD:Path2D.WIND_NON_ZERO);
        for (int ringIndex=0;ringIndex<rings.size();ringIndex++) {
            double[] cubic=cubicRings.length>ringIndex?cubicRings[ringIndex]:null;
            if (cubic!=null) {
                shape.moveTo(cubic[0], cubic[1]);
                for (int offset=0;offset<cubic.length;offset+=8) {
                    shape.curveTo(cubic[offset+2], cubic[offset+3], cubic[offset+4], cubic[offset+5], cubic[offset+6], cubic[offset+7]);
                }
            } else {
                double[] coordinates=rings.get(ringIndex);
                shape.moveTo(coordinates[0], coordinates[1]);
                for (int coordinate=2;coordinate<coordinates.length;coordinate+=2) {
                    shape.lineTo(coordinates[coordinate], coordinates[coordinate+1]);
                }
            }
            if (path.isClosed()) {
                shape.closePath();
            }
        }
        return shape;
    }
    private static boolean hasCubicData(VectorDocument document) {
        for (int index=0;index<document.getPaths().size();index++) {
            if (document.getPaths().get(index).hasCubicData()) {
                return true;
            }
        }
        return false;
    }
    private static double clamp(double value) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }
    private static final class MouseHandler implements MouseWheelListener, MouseListener, MouseMotionListener {
        private final VectorCanvas canvas;
        private int lastX;
        private int lastY;
        private boolean dragging;
        private MouseHandler(VectorCanvas canvas) {
            this.canvas=canvas;
        }
        public void mouseWheelMoved(MouseWheelEvent event) {
            double factor=event.getWheelRotation()<0?1.1:0.9;
            canvas.zoomAt(event.getX(), event.getY(), factor);
        }
        public void mousePressed(MouseEvent event) {
            if (event.getButton()==MouseEvent.BUTTON1) {
                dragging=true;
                lastX=event.getX();
                lastY=event.getY();
                canvas.requestFocusInWindow();
            }
        }
        public void mouseDragged(MouseEvent event) {
            if (dragging) {
                canvas.panX+=event.getX()-lastX;
                canvas.panY+=event.getY()-lastY;
                lastX=event.getX();
                lastY=event.getY();
                canvas.repaint();
            }
        }
        public void mouseReleased(MouseEvent event) {
            dragging=false;
        }
        public void mouseClicked(MouseEvent event) {
        }
        public void mouseEntered(MouseEvent event) {
        }
        public void mouseExited(MouseEvent event) {
        }
        public void mouseMoved(MouseEvent event) {
        }
    }
}
