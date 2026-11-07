package com.avrix.provider.zomboid.approval.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws the approval window glyphs with Java2D.
 * <p>
 * Vector rendering keeps the icons crisp at every size and avoids bundled raster assets.
 */
public final class ZomboidApprovalIcons {

    private ZomboidApprovalIcons() {
        throw new UnsupportedOperationException("Icon utility class cannot be instantiated");
    }

    /**
     * Creates a scalable icon for a named glyph.
     *
     * @param name  glyph name
     * @param size  square size in pixels
     * @param color glyph color
     * @return scalable icon
     */
    public static Icon icon(String name, int size, Color color) {
        return new VectorIcon(name, Math.max(1, size), color);
    }

    /**
     * Paints a named glyph at the requested position and size.
     *
     * @param graphics target graphics
     * @param name     glyph name
     * @param x        left edge
     * @param y        top edge
     * @param size     square size in pixels
     * @param color    glyph color
     */
    public static void paint(Graphics2D graphics, String name, int x, int y, int size, Color color) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(color);
            draw(g2, name, Math.max(1, size));
        } finally {
            g2.dispose();
        }
    }

    private static void draw(Graphics2D graphics, String name, float size) {
        float stroke = Math.max(1.3f, size * 0.105f);
        graphics.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case "tick" -> drawTick(graphics, size);
            case "cross" -> drawCross(graphics, size);
            case "minus" -> drawLine(graphics, size, 0.20f, 0.50f, 0.80f, 0.50f);
            case "circle" -> graphics.fill(new Ellipse2D.Float(
                    size * 0.18f,
                    size * 0.18f,
                    size * 0.64f,
                    size * 0.64f
            ));
            case "arrow-down" -> drawArrowDown(graphics, size);
            case "arrow-right" -> drawArrowRight(graphics, size);
            case "document" -> drawDocument(graphics, size);
            case "info" -> drawInfo(graphics, size);
            case "tag" -> drawTag(graphics, size);
            case "grid-add" -> drawGridAdd(graphics, size);
            case "target" -> drawTarget(graphics, size);
            case "time" -> drawTime(graphics, size);
            case "exclamation" -> drawWarning(graphics, size);
            default -> graphics.fill(new Ellipse2D.Float(
                    size * 0.42f,
                    size * 0.42f,
                    size * 0.16f,
                    size * 0.16f
            ));
        }
    }

    private static void drawTick(Graphics2D graphics, float size) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(size * 0.18f, size * 0.52f);
        path.lineTo(size * 0.40f, size * 0.74f);
        path.lineTo(size * 0.82f, size * 0.26f);
        graphics.draw(path);
    }

    private static void drawCross(Graphics2D graphics, float size) {
        drawLine(graphics, size, 0.25f, 0.25f, 0.75f, 0.75f);
        drawLine(graphics, size, 0.75f, 0.25f, 0.25f, 0.75f);
    }

    private static void drawArrowDown(Graphics2D graphics, float size) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(size * 0.25f, size * 0.40f);
        path.lineTo(size * 0.50f, size * 0.65f);
        path.lineTo(size * 0.75f, size * 0.40f);
        graphics.draw(path);
    }

    private static void drawArrowRight(Graphics2D graphics, float size) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(size * 0.40f, size * 0.25f);
        path.lineTo(size * 0.65f, size * 0.50f);
        path.lineTo(size * 0.40f, size * 0.75f);
        graphics.draw(path);
    }

    private static void drawDocument(Graphics2D graphics, float size) {
        Path2D.Float outline = new Path2D.Float();
        outline.moveTo(size * 0.24f, size * 0.14f);
        outline.lineTo(size * 0.62f, size * 0.14f);
        outline.lineTo(size * 0.78f, size * 0.30f);
        outline.lineTo(size * 0.78f, size * 0.86f);
        outline.lineTo(size * 0.24f, size * 0.86f);
        outline.closePath();
        graphics.draw(outline);
        drawLine(graphics, size, 0.62f, 0.14f, 0.62f, 0.30f);
        drawLine(graphics, size, 0.62f, 0.30f, 0.78f, 0.30f);
    }

    private static void drawInfo(Graphics2D graphics, float size) {
        graphics.draw(new Ellipse2D.Float(size * 0.16f, size * 0.16f, size * 0.68f, size * 0.68f));
        graphics.fill(new Ellipse2D.Float(size * 0.46f, size * 0.30f, size * 0.08f, size * 0.08f));
        drawLine(graphics, size, 0.50f, 0.47f, 0.50f, 0.68f);
    }

    private static void drawTag(Graphics2D graphics, float size) {
        Path2D.Float tag = new Path2D.Float();
        tag.moveTo(size * 0.18f, size * 0.32f);
        tag.lineTo(size * 0.52f, size * 0.32f);
        tag.lineTo(size * 0.78f, size * 0.50f);
        tag.lineTo(size * 0.52f, size * 0.68f);
        tag.lineTo(size * 0.18f, size * 0.68f);
        tag.closePath();
        graphics.draw(tag);
        graphics.draw(new Ellipse2D.Float(size * 0.30f, size * 0.44f, size * 0.12f, size * 0.12f));
    }

    private static void drawGridAdd(Graphics2D graphics, float size) {
        float square = size * 0.27f;
        float gap = size * 0.08f;
        float left = size * 0.16f;
        float top = size * 0.16f;
        float arc = size * 0.06f;

        graphics.fill(new RoundRectangle2D.Float(left, top, square, square, arc, arc));
        graphics.fill(new RoundRectangle2D.Float(left + square + gap, top, square, square, arc, arc));
        graphics.fill(new RoundRectangle2D.Float(left, top + square + gap, square, square, arc, arc));

        float centerX = left + square + gap + square / 2;
        float centerY = top + square + gap + square / 2;
        float arm = size * 0.10f;
        drawLine(
                graphics,
                size,
                (centerX - arm) / size,
                centerY / size,
                (centerX + arm) / size,
                centerY / size
        );
        drawLine(
                graphics,
                size,
                centerX / size,
                (centerY - arm) / size,
                centerX / size,
                (centerY + arm) / size
        );
    }

    private static void drawTarget(Graphics2D graphics, float size) {
        graphics.draw(new Ellipse2D.Float(size * 0.16f, size * 0.16f, size * 0.68f, size * 0.68f));
        graphics.draw(new Ellipse2D.Float(size * 0.34f, size * 0.34f, size * 0.32f, size * 0.32f));
        drawLine(graphics, size, 0.50f, 0.04f, 0.50f, 0.18f);
        drawLine(graphics, size, 0.50f, 0.82f, 0.50f, 0.96f);
        drawLine(graphics, size, 0.04f, 0.50f, 0.18f, 0.50f);
        drawLine(graphics, size, 0.82f, 0.50f, 0.96f, 0.50f);
    }

    private static void drawTime(Graphics2D graphics, float size) {
        graphics.draw(new Ellipse2D.Float(size * 0.14f, size * 0.14f, size * 0.72f, size * 0.72f));
        drawLine(graphics, size, 0.50f, 0.28f, 0.50f, 0.52f);
        drawLine(graphics, size, 0.50f, 0.52f, 0.68f, 0.62f);
    }

    private static void drawWarning(Graphics2D graphics, float size) {
        Path2D.Float triangle = new Path2D.Float();
        triangle.moveTo(size * 0.50f, size * 0.12f);
        triangle.lineTo(size * 0.90f, size * 0.84f);
        triangle.lineTo(size * 0.10f, size * 0.84f);
        triangle.closePath();
        graphics.draw(triangle);
        drawLine(graphics, size, 0.50f, 0.36f, 0.50f, 0.60f);
        graphics.fill(new Ellipse2D.Float(size * 0.46f, size * 0.68f, size * 0.08f, size * 0.08f));
    }

    private static void drawLine(
            Graphics2D graphics,
            float size,
            float x1,
            float y1,
            float x2,
            float y2
    ) {
        graphics.drawLine(
                Math.round(size * x1),
                Math.round(size * y1),
                Math.round(size * x2),
                Math.round(size * y2)
        );
    }

    /**
     * Scalable icon backed by the vector glyph renderer.
     */
    private static final class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        private VectorIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        /**
         * Paints the icon at the requested position.
         *
         * @param component component that owns the icon
         * @param graphics  target graphics
         * @param x         left edge
         * @param y         top edge
         */
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (graphics instanceof Graphics2D g2) {
                ZomboidApprovalIcons.paint(g2, name, x, y, size, color);
            }
        }

        /**
         * Returns the icon width.
         *
         * @return icon width in pixels
         */
        @Override
        public int getIconWidth() {
            return size;
        }

        /**
         * Returns the icon height.
         *
         * @return icon height in pixels
         */
        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
