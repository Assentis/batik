/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt.text;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.batik.gvt.font.AWTGVTFont;
import org.apache.batik.gvt.font.AltGlyphHandler;
import org.apache.batik.gvt.font.GVTFont;
import org.apache.batik.gvt.font.GVTGlyphMetrics;
import org.apache.batik.gvt.font.GVTGlyphVector;
import org.apache.batik.gvt.font.GVTLineMetrics;
import org.apache.batik.gvt.font.MultiGlyphVector;

/**
 * Implementation of TextSpanLayout which uses java.awt.font.GlyphVector.
 * @see org.apache.batik.gvt.text.TextSpanLayout
 *
 * @author <a href="bill.haneman@ireland.sun.com>Bill Haneman</a>
 * @version $Id$
 */
public class GlyphLayout implements TextSpanLayout {

    private GVTGlyphVector gv;
    private GVTFont font;
    private GVTLineMetrics metrics;
    private AttributedCharacterIterator aci;
    private FontRenderContext frc;
    private Point2D advance;
    private Point2D offset;
    private float   xScale=1;
    private float   yScale=1;
    private Point2D prevCharPosition;
    private TextPath textPath;
    private Point2D textPathAdvance;
    private int []  charMap;
    private boolean vertical, adjSpacing=true;
    private float [] glyphAdvances;
    private boolean isAltGlyph; //false

    // When layoutApplied is false it means that the glyph positions
    // are different from where they would be if you did
    // doExplicitGlyphLayout().
    private boolean layoutApplied = false;
    // When spacingApplied is false it means that xScale, yScale and
    // kerning/wordspacing stuff haven't been applied. This can
    // be rectified by calling adjustTextSpacing().  Note that when
    // spacing is actually used layoutApplied will be cleared it
    // is not garunteed that applying text spacing will cause it to
    // be cleared (it will only be cleared if the glyphs move).
    private boolean spacingApplied = false;
    // When pathApplied is false it means that the text has not been
    // layed out on the associated text path (if any).  If there is an
    // associated text path then this will clear both layoutApplied
    // and spacing applied but neither will be touched if no text path
    // is present.
    private boolean pathApplied    = false;


    public static final AttributedCharacterIterator.Attribute FLOW_LINE_BREAK
        = GVTAttributedCharacterIterator.TextAttribute.FLOW_LINE_BREAK;

    public static final AttributedCharacterIterator.Attribute FLOW_PARAGRAPH 
        = GVTAttributedCharacterIterator.TextAttribute.FLOW_PARAGRAPH;

    public static final AttributedCharacterIterator.Attribute 
        FLOW_EMPTY_PARAGRAPH 
        = GVTAttributedCharacterIterator.TextAttribute.FLOW_EMPTY_PARAGRAPH;

    public static final AttributedCharacterIterator.Attribute 
        TEXT_COMPOUND_DELIMITER 
        = GVTAttributedCharacterIterator.TextAttribute.TEXT_COMPOUND_DELIMITER;

    public static final AttributedCharacterIterator.Attribute 
        VERTICAL_ORIENTATION 
        = GVTAttributedCharacterIterator.TextAttribute.VERTICAL_ORIENTATION;

    public static final 
        AttributedCharacterIterator.Attribute VERTICAL_ORIENTATION_ANGLE =
       GVTAttributedCharacterIterator.TextAttribute.VERTICAL_ORIENTATION_ANGLE;

    public static final 
        AttributedCharacterIterator.Attribute HORIZONTAL_ORIENTATION_ANGLE =
     GVTAttributedCharacterIterator.TextAttribute.HORIZONTAL_ORIENTATION_ANGLE;

    private static final AttributedCharacterIterator.Attribute X
        = GVTAttributedCharacterIterator.TextAttribute.X;

    private static final AttributedCharacterIterator.Attribute Y
        = GVTAttributedCharacterIterator.TextAttribute.Y;

    private static final AttributedCharacterIterator.Attribute DX
        = GVTAttributedCharacterIterator.TextAttribute.DX;

    private static final AttributedCharacterIterator.Attribute DY
        = GVTAttributedCharacterIterator.TextAttribute.DY;

    private static final AttributedCharacterIterator.Attribute ROTATION
        = GVTAttributedCharacterIterator.TextAttribute.ROTATION;

    private static final AttributedCharacterIterator.Attribute BASELINE_SHIFT
        = GVTAttributedCharacterIterator.TextAttribute.BASELINE_SHIFT;

    private static final AttributedCharacterIterator.Attribute WRITING_MODE
        = GVTAttributedCharacterIterator.TextAttribute.WRITING_MODE;

    private static final Integer WRITING_MODE_TTB
        = GVTAttributedCharacterIterator.TextAttribute.WRITING_MODE_TTB;

    private static final Integer ORIENTATION_AUTO
        = GVTAttributedCharacterIterator.TextAttribute.ORIENTATION_AUTO;

    static Set runAtts = new HashSet();

    static {
        runAtts.add(X);
        runAtts.add(Y);
        runAtts.add(DX);
        runAtts.add(DY);
        runAtts.add(ROTATION);
        runAtts.add(BASELINE_SHIFT);
    }

    static Set szAtts = new HashSet();

    static {
        szAtts.add(TextAttribute.SIZE);
    }


    /**
     * Creates the specified text layout using the
     * specified AttributedCharacterIterator and rendering context.
     *
     * @param aci the AttributedCharacterIterator whose text is to
     *  be laid out
     * @param charMap Indicates how chars in aci map to original
     *                text char array.
     * @param offset The offset position of this text layout
     * @param frc the FontRenderContext to use for generating glyphs.
     */
    public GlyphLayout(AttributedCharacterIterator aci,
                       int [] charMap,
                       Point2D offset,
                       FontRenderContext frc) {

        this.aci = aci;
        this.frc = frc;
        this.offset = offset;
        this.font = getFont();
        this.charMap = charMap;

        this.metrics = font.getLineMetrics
            (aci, aci.getBeginIndex(), aci.getEndIndex(), frc);

        // create the glyph vector
        this.gv = null;
        this.aci.first();
        this.vertical = (aci.getAttribute(WRITING_MODE) == WRITING_MODE_TTB);
        this.textPath =  (TextPath) aci.getAttribute
            (GVTAttributedCharacterIterator.TextAttribute.TEXTPATH);

        AltGlyphHandler altGlyphHandler 
            = (AltGlyphHandler)this.aci.getAttribute
            (GVTAttributedCharacterIterator.TextAttribute.ALT_GLYPH_HANDLER);
        if (altGlyphHandler != null) {
            // this must be an altGlyph text element, try and create
            // the alternate glyphs
            this.gv = altGlyphHandler.createGlyphVector
                (frc, this.font.getSize(), this.aci);
            if ( this.gv != null ){
                this.isAltGlyph = true;
            }
        }
        if (this.gv == null) {
            // either not an altGlyph or the altGlyphHandler failed to
            // create a glyph vector
            this.gv = font.createGlyphVector(frc, this.aci);
        }
    }


    public GVTGlyphVector getGlyphVector() {
        return this.gv;
    }


    /**
     * Returns the current text position at the beginning
     * of glyph layout, before the application of explicit
     * glyph positioning attributes.
     */
    public Point2D getOffset() {
        return offset;
    }

    /**
     * Sets the scaling factor to use for string.  if ajdSpacing is
     * true then only the spacing between glyphs will be adjusted
     * otherwise the glyphs and the spaces between them will be
     * adjusted.  Only the scale factor in the progression direction
     * is used (x for horizontal text, y for vertical text
     * ).
     * @param xScale Scale factor to apply in X direction.
     * @param yScale Scale factor to apply in Y direction.
     * @param adjSpacing True if only spaces should be adjusted.  
     */
    public void setScale(float xScale, float yScale, boolean adjSpacing) {
        // Fix the off axis scale factor.
        if (vertical) xScale = 1;
        else          yScale = 1;

        if ((xScale != this.xScale) ||
            (yScale != this.yScale) ||
            (adjSpacing != this.adjSpacing)) {
            this.xScale = xScale;
            this.yScale = yScale;
            this.adjSpacing = adjSpacing;

            // We don't affect layoutApplied directly...
            // System.out.println("layoutApplied: " + layoutApplied);

            // However if we did path layout or spacing it's all junk now...
            spacingApplied = false;
            glyphAdvances  = null;
            pathApplied    = false;
        }
    }

    /**
     * Sets the text position used for the implicit origin
     * of glyph layout. Ignored if multiple explicit glyph
     * positioning attributes are present in ACI
     * (e.g. if the aci has multiple X or Y values).
     */
    public void setOffset(Point2D offset) {
        // System.out.println("SetOffset: " + offset + " - " + this.offset);
        if ((offset.getX() != this.offset.getX()) ||
            (offset.getY() != this.offset.getY())) {
            if ((layoutApplied)||(spacingApplied)) {
                // Already layed out need to shift glyph positions to
                // account for new offset.
                float dx = (float)(offset.getX()-this.offset.getX());
                float dy = (float)(offset.getY()-this.offset.getY());
                int numGlyphs = gv.getNumGlyphs();

                // System.out.println("DXY: [" + dx +","+dy+"]");
                float [] gp = gv.getGlyphPositions(0, numGlyphs+1, null);
                for (int i=0; i<=numGlyphs; i++) {
                    gv.setGlyphPosition(i, new Point2D.Float(gp[2*i]+dx,
                                                             gp[2*i+1]+dy));
                }
            }

            // When not layed out (or after updating) just set the new
            // offset this will be factored in for any future layout
            // operations.
            this.offset = offset;

            // We don't affect layoutApplied or spacingApplied since
            // they both work off the offset value.

            // However if we did path layout it's all junk now...
            pathApplied = false;
        }
    }

    public GVTGlyphMetrics getGlyphMetrics(int glyphIndex) {
        return gv.getGlyphMetrics(glyphIndex);
    }

    /**
     * Returns true if the advance direction of this text is vertical.
     */
    public boolean isVertical() {
        return vertical;
    }

    /**
     * Returns true if this layout in on a text path.
     */
    public boolean isOnATextPath() {
        return (textPath != null);
    }


    /**
     * Returns the number of glyphs in this layout.
     */
    public int getGlyphCount() {
        return gv.getNumGlyphs();
    }


    /**
     * Returns the number of chars represented by the glyphs within the
     * specified range.
     *
     * @param startGlyphIndex The index of the first glyph in the range.
     * @param endGlyphIndex The index of the last glyph in the range.
     *
     * @return The number of chars.
     */
    public int getCharacterCount(int startGlyphIndex, int endGlyphIndex) {
        return gv.getCharacterCount(startGlyphIndex, endGlyphIndex);
    }

    /**
     * Returns true if the text direction in this layout is from left to right.
     */
    public boolean isLeftToRight() {
        aci.first();
        int bidiLevel = 
            ((Integer)aci.getAttribute
             (GVTAttributedCharacterIterator.TextAttribute.BIDI_LEVEL))
            .intValue();

        // Check if low bit is set if not then we are left to right
        // (even bidi level).
        return ((bidiLevel&0x01) == 0);
    }


    /**
     * This method makes certain that the layout has been 
     * completed at this point (much of the layout is done lazily).
     */
    private final void syncLayout() {
        if (!pathApplied) {
            // System.out.println("Doing Path Layout: " + this);
            doPathLayout();
        }
    }

    /**
     * Paints the text layout using the
     * specified Graphics2D and rendering context.
     * @param g2d the Graphics2D to use
     * @param context The current render context
     */
    public void draw(Graphics2D g2d) {
        syncLayout();
        gv.draw(g2d, aci);
    }

    /**
     * Returns the current text position at the completion
     * of glyph layout.
     */
    public Point2D getAdvance2D() {
        adjustTextSpacing();
        return advance;
    }


    /**
     * Returns the outline of the completed glyph layout.
     */
    public Shape getOutline() {
        syncLayout();

        return gv.getOutline();
    }

    public float [] getGlyphAdvances() {
        if (glyphAdvances != null)
            return glyphAdvances;

        if (!spacingApplied)
            // This will layout the text if needed.
            adjustTextSpacing();

        int numGlyphs = gv.getNumGlyphs();
        float [] glyphPos = gv.getGlyphPositions(0, numGlyphs+1, null);
        glyphAdvances = new float[numGlyphs+1];
        int off = 0;
        if (isVertical())
            off = 1;

        float start = glyphPos[off];
        for (int i=0; i<numGlyphs+1; i++) {
            glyphAdvances[i] = glyphPos[i+i+off]-start;
        }
        return glyphAdvances;
    }

    /**
     * Returns the outline of the specified decorations on the glyphs,
     * @param decorationType an integer indicating the type(s) of decorations
     *     included in this shape.  May be the result of "OR-ing" several
     *     values together:
     * e.g. <tt>DECORATION_UNDERLINE | DECORATION_STRIKETHROUGH</tt>
     */
    public Shape getDecorationOutline(int decorationType) {
        syncLayout();

        Shape g = new GeneralPath();
        if ((decorationType & DECORATION_UNDERLINE) != 0) {
             ((GeneralPath) g).append(getUnderlineShape(), false);
        }
        if ((decorationType & DECORATION_STRIKETHROUGH) != 0) {
             ((GeneralPath) g).append(getStrikethroughShape(), false);
        }
        if ((decorationType & DECORATION_OVERLINE) != 0) {
             ((GeneralPath) g).append(getOverlineShape(), false);
        }
        return g;
    }

    /**
     * Returns the rectangular bounds of the completed glyph layout.
     */
    public Rectangle2D getBounds2D() {
        syncLayout();
        return gv.getBounds2D(aci);
    }

    /**
     * Returns the rectangular bounds of the completed glyph layout,
     * inclusive of "decoration" (underline, overline, etc.)
     */
    public Rectangle2D getGeometricBounds() {
        syncLayout();
        Rectangle2D gvB, decB;
        gvB = gv.getGeometricBounds();
        decB = getDecorationOutline(DECORATION_ALL).getBounds2D();
        return gvB.createUnion(decB);
    }

    /**
     * Returns the position to used when drawing a text run after this one.
     * It takes into account the text path layout if there is one.
     */
    public Point2D getTextPathAdvance() {
        syncLayout();
        if (textPath != null) {
            return textPathAdvance;
        } else {
            return getAdvance2D();
        }
    }


    /**
     * Returns the index of the first glyph that has the specified char index.
     *
     * @param charIndex The original index of the character in the text node's
     * text string.
     * @return The index of the matching glyph in this layout's glyph vector,
     *         or -1 if a matching glyph could not be found.
     */
    public int getGlyphIndex(int charIndex) {
        int numGlyphs = getGlyphCount();
        int j=0;
        for (int i = 0; i < numGlyphs; i++) {
            int count = getCharacterCount(i, i);
            for (int n=0; n<count; n++) {
                int glyphCharIndex = charMap[j++];
                if (charIndex == glyphCharIndex)
                    return i;
                if (j >= charMap.length)
                    return -1;
            }
        }
        return -1;
    }

    /**
     * Returns the index of the last glyph that has the specified char index.
     *
     * @param charIndex The original index of the character in the text node's
     * text string.
     * @return The index of the matching glyph in this layout's glyph vector,
     *         or -1 if a matching glyph could not be found.
     */
    public int getLastGlyphIndex(int charIndex) {
        int numGlyphs = getGlyphCount();
        int j=charMap.length-1;
        for (int i = numGlyphs-1; i >= 0; --i) {
            int count = getCharacterCount(i, i);
            for (int n=0; n<count; n++) {
                int glyphCharIndex = charMap[j--];
                if (charIndex == glyphCharIndex) return i;
                if (j < 0)                       return -1;
            }
        }
        return -1;
    }


    /**
     * Return the angle value according to the orientation
     * of the character.
     */
    public double getComputedOrientationAngle(int index){

        if ( isGlyphOrientationAuto() ){
            if (isVertical()) {
                char ch = aci.setIndex(index);
                if (isLatinChar(ch))
                    return 90.0;
                else
                    return 0.0;
            }
            return 0.0;
        }
        else{
            return getGlyphOrientationAngle();
        }
    }

   /**
     * Returns a Shape which encloses the currently selected glyphs
     * as specified by the character indices.
     *
     * @param beginCharIndex the index of the first char in the
     * contiguous selection.
     * @param endCharIndex the index of the last char in the
     * contiguous selection.
     * @return The highlight shape or null if the spacified char range
     * does not overlap with the chars in this layout.  */
    public Shape getHighlightShape(int beginCharIndex, int endCharIndex) {
        syncLayout();

        if (beginCharIndex > endCharIndex) {
            int temp = beginCharIndex;
            beginCharIndex = endCharIndex;
            endCharIndex = temp;
        }
        GeneralPath shape = null;
        int start       = aci.getBeginIndex();
        int numGlyphs = getGlyphCount();

        Point2D.Float [] topPts = new Point2D.Float[2*numGlyphs];
        Point2D.Float [] botPts = new Point2D.Float[2*numGlyphs];

        int ptIdx = 0;

        int currentChar = 0;
        for (int i = 0; i < numGlyphs; i++) {
            int glyphCharIndex = charMap[currentChar];
            if ((glyphCharIndex >= beginCharIndex) &&
                (glyphCharIndex <= endCharIndex) &&
                gv.isGlyphVisible(i)) {
                    
                Shape gbounds = gv.getGlyphLogicalBounds(i);
                if (gbounds != null) {
                    // We got something...
                    if (shape == null)
                        shape = new GeneralPath();

                    // We are pretty dumb here we assume that we always
                    // get back polygons with four sides to them if
                    // isn't met we are SOL.
                    float [] pts = new float[6];
                    int count = 0;
                    int type = -1;

                    PathIterator pi = gbounds.getPathIterator(null);
                    Point2D.Float firstPt = null;

                    while (!pi.isDone()) {
                        type = pi.currentSegment(pts);
                        if ((type == PathIterator.SEG_MOVETO) ||
                            (type == PathIterator.SEG_LINETO)) {
                            // LINETO or MOVETO
                            if (count > 4) break; // too many lines...
                            if (count == 4) {
                                // make sure we are just closing it..
                                if ((firstPt == null)     ||
                                    (firstPt.x != pts[0]) ||
                                    (firstPt.y != pts[1]))
                                    break;
                            } else {
                                Point2D.Float pt;
                                pt = new Point2D.Float(pts[0], pts[1]);
                                if (count == 0) firstPt = pt;
                                // Use sides of  rectangle...
                                switch (count) {
                                case 0: botPts[ptIdx]   = pt; break;
                                case 1: topPts[ptIdx]   = pt; break;
                                case 2: topPts[ptIdx+1] = pt; break;
                                case 3: botPts[ptIdx+1] = pt; break;
                                }
                            }
                        } else if (type == PathIterator.SEG_CLOSE) {
                                // Close in the wrong spot?
                            if ((count < 4) || (count > 5)) break;
                        } else {
                            // QUADTO or CUBETO
                            break;
                        }

                        count++;
                        pi.next();
                    }
                    if (pi.isDone()) {
                        // Sucessfully Expressed as a quadralateral...
                        if ((botPts[ptIdx]!=null) &&
                            ((topPts[ptIdx].x != topPts[ptIdx+1].x) ||
                             (topPts[ptIdx].y != topPts[ptIdx+1].y)))
                            // box isn't empty so use it's points...
                            ptIdx += 2;
                    } else {
                        // System.out.println("Type: " + type +
                        //                    " count: " + count);
                        // Wasn't a quadralateral so just add it don't try
                        // and merge it...
                        addPtsToPath(shape, topPts, botPts, ptIdx);
                        ptIdx = 0;
                        shape.append(gbounds, false);
                    }
                }
            }
            currentChar += getCharacterCount(i, i);
            if (currentChar >= charMap.length)
                currentChar = charMap.length-1;
        }
        addPtsToPath(shape, topPts, botPts, ptIdx);

        return shape;
    }

    public static final float eps = 0.00001f;

    public static int makeConvexHull(Point2D.Float [] pts, int numPts) {
        // Sort the Pts in X...
        Point2D.Float tmp;
        // System.out.print("Sorting...");
        for (int i=1; i<numPts; i++) {
            // Simple bubble sort (numPts should be small so shouldn't
            // be too bad.).
            if ((pts[i].x < pts[i-1].x) ||
                ((pts[i].x == pts[i-1].x) && (pts[i].y < pts[i-1].y))) {
                tmp = pts[i];
                pts[i] = pts[i-1];
                pts[i-1] = tmp;
                i=0;
                continue;
            }
        }

        // System.out.println("Sorted");

        Point2D.Float pt0 = pts[0];
        Point2D.Float pt1 = pts[numPts-1];
        Point2D.Float dxdy = new Point2D.Float(pt1.x-pt0.x, pt1.y-pt0.y);
        float soln, c = dxdy.y*pt0.x-dxdy.x*pt0.y;

        Point2D.Float [] topList = new Point2D.Float[numPts];
        Point2D.Float [] botList = new Point2D.Float[numPts];
        botList[0] = topList[0] = pts[0];
        int nTopPts=1;
        int nBotPts=1;
        for (int i=1; i<numPts-1; i++) {
            Point2D.Float pt = pts[i];
            soln = dxdy.x*pt.y-dxdy.y*pt.x+c;
            if (soln < 0) {
                // Below line goes into bot pt list...
                while (nBotPts >= 2) {
                    pt0 = botList[nBotPts-2];
                    pt1 = botList[nBotPts-1];
                    float dx = pt1.x-pt0.x;
                    float dy = pt1.y-pt0.y;
                    float c0 = dy*pt0.x-dx*pt0.y;
                    soln = dx*pt.y-dy*pt.x+c0;
                    if (soln > eps) // Left turn add and we are done..
                        break;
                    if (soln > -eps) {
                        // On line take lowest Y of two and keep going
                        if (pt1.y < pt.y) pt = pt1;
                        nBotPts--;
                        break;
                    }
                    // right turn drop prev pt;
                    nBotPts--;
                }
                botList[nBotPts++] = pt;
            } else {
                // Above line goes into top pt list...
                while (nTopPts >= 2) {
                    pt0 = topList[nTopPts-2];
                    pt1 = topList[nTopPts-1];
                    float dx = pt1.x-pt0.x;
                    float dy = pt1.y-pt0.y;
                    float c0 = dy*pt0.x-dx*pt0.y;
                    soln = dx*pt.y-dy*pt.x+c0;
                    if (soln < -eps) // Right turn add and check next point.
                        break;
                    if (soln < eps) {
                        // On line take greatest Y of two and keep going
                        if (pt1.y > pt.y) pt = pt1;
                        nTopPts--;
                        break;
                    }
                    // left turn drop prev pt;
                    nTopPts--;
                }
                topList[nTopPts++] = pt;
            }
        }

        // Check last point in both sets...
        Point2D.Float pt = pts[numPts-1];
        while (nBotPts >= 2) {
            pt0 = botList[nBotPts-2];
            pt1 = botList[nBotPts-1];
            float dx = pt1.x-pt0.x;
            float dy = pt1.y-pt0.y;
            float c0 = dy*pt0.x-dx*pt0.y;
            soln = dx*pt.y-dy*pt.x+c0;
            if (soln > eps)
                // Left turn add and we are done..
                break;
            if (soln > -eps) {
                // On line take lowest Y of two and keep going
                if (pt1.y >= pt.y) nBotPts--;
                break;
            }
            // right turn drop prev pt;
            nBotPts--;
        }

        while (nTopPts >= 2) {
            pt0 = topList[nTopPts-2];
            pt1 = topList[nTopPts-1];
            float dx = pt1.x-pt0.x;
            float dy = pt1.y-pt0.y;
            float c0 = dy*pt0.x-dx*pt0.y;
            soln = dx*pt.y-dy*pt.x+c0;
            if (soln < -eps)
                // Right turn done...
                break;
            if (soln < eps) {
                // On line take lowest Y of two and keep going
                if (pt1.y <= pt.y) nTopPts--;
                break;
            }
            // left turn drop prev pt;
            nTopPts--;
        }

        int i=0;
        for (; i<nTopPts; i++)
            pts[i] = topList[i];

        // We always include the 'last' point as it is always on convex hull.
        pts[i++] = pts[numPts-1];

        // don't include botList[0] since it is the same as topList[0].
        for (int n=nBotPts-1; n>0; n--, i++)
            pts[i] = botList[n];

        // System.out.println("CHull has " + i + " pts");
        return i;
    }

    public static void addPtsToPath(GeneralPath shape,
                                     Point2D.Float [] topPts,
                                     Point2D.Float [] botPts,
                                     int numPts) {
        if (numPts < 2) return;
        if (numPts == 2) {
            shape.moveTo(topPts[0].x, topPts[0].y);
            shape.lineTo(topPts[1].x, topPts[1].y);
            shape.lineTo(botPts[1].x, botPts[1].y);
            shape.lineTo(botPts[0].x, botPts[0].y);
            shape.lineTo(topPts[0].x, topPts[0].y);
            return;
        }

        // Here we 'connect the dots' the best way we know how...
        // What I do is construct a convex hull between adjacent
        // character boxes, then I union that into the shape.  this
        // does a good job of bridging between adjacent characters,
        // but still closely tracking to text boxes.  The use of the
        // Area class is fairly heavy weight but it seems to keep up
        // in this instanace (probably because all the shapes are very
        // simple polygons).
        Point2D.Float [] boxes = new Point2D.Float[8];
        Point2D.Float [] chull = new Point2D.Float[8];
        boxes[4] = topPts[0];
        boxes[5] = topPts[1];
        boxes[6] = botPts[1];
        boxes[7] = botPts[0];
        Area []areas = new Area[numPts/2];
        int nAreas =0;
        for (int i=2; i<numPts; i+=2) {
            boxes[0] = boxes[4];
            boxes[1] = boxes[5];
            boxes[2] = boxes[6];
            boxes[3] = boxes[7];
            boxes[4] = topPts[i];
            boxes[5] = topPts[i+1];
            boxes[6] = botPts[i+1];
            boxes[7] = botPts[i];

            float delta,sz,dist;
            delta  = boxes[2].x-boxes[0].x;
            dist   = delta*delta;
            delta  = boxes[2].y-boxes[0].y;
            dist  += delta*delta;
            sz     = (float)Math.sqrt(dist);

            delta  = boxes[6].x-boxes[4].x;
            dist   = delta*delta;
            delta  = boxes[6].y-boxes[4].y;
            dist  += delta*delta;
            sz    += (float)Math.sqrt(dist);

            delta = ((boxes[0].x+boxes[1].x+boxes[2].x+boxes[3].x)-
                     (boxes[4].x+boxes[5].x+boxes[6].x+boxes[7].x))/4;
            dist = delta*delta;
            delta = ((boxes[0].y+boxes[1].y+boxes[2].y+boxes[3].y)-
                     (boxes[4].y+boxes[5].y+boxes[6].y+boxes[7].y))/4;
            dist += delta*delta;
            dist  = (float)Math.sqrt(dist);
            // Note here that dist is the distance between center
            // points, and sz is the sum of the length of the
            // diagonals of the letter boxes.  In normal cases one
            // would expect dist to be approximately equal to sz/2.
            // So here we merge if the two characters are within four
            // character widths of each other. If they are farther
            // apart than that chances are it's a 'line break' or
            // something similar where we will get better results
            // merging seperately, and anyways with this much space
            // between them the extra outline shouldn't hurt..
            GeneralPath gp = new GeneralPath();
            if (dist < sz) {
                // Close enough to merge with previous char...
                System.arraycopy(boxes, 0, chull, 0, 8);
                int npts = makeConvexHull(chull, 8);
                gp.moveTo(chull[0].x, chull[0].y);
                for(int n=1; n<npts; n++)
                    gp.lineTo(chull[n].x, chull[n].y);
                gp.closePath();
            } else {
                // Merge all previous areas
                mergeAreas(shape, areas, nAreas);
                nAreas = 0; // Start fresh...

                // Then just add box (add the previous char box if first pts)
                if (i==2) {
                    gp.moveTo(boxes[0].x, boxes[0].y);
                    gp.lineTo(boxes[1].x, boxes[1].y);
                    gp.lineTo(boxes[2].x, boxes[2].y);
                    gp.lineTo(boxes[3].x, boxes[3].y);
                    gp.closePath();
                    shape.append(gp, false);
                    gp.reset();
                }
                gp.moveTo(boxes[4].x, boxes[4].y);
                gp.lineTo(boxes[5].x, boxes[5].y);
                gp.lineTo(boxes[6].x, boxes[6].y);
                gp.lineTo(boxes[7].x, boxes[7].y);
                gp.closePath();
            }
            areas[nAreas++] = new Area(gp);
        }

        mergeAreas(shape, areas, nAreas);
    }

    public static void mergeAreas(GeneralPath shape,
                                  Area []shapes, int nShapes) {
        // Merge areas hierarchically, this means that while there are
        // the same number of Area.add calls (n-1) the great majority
        // of them are very simple combinations.  This helps to speed
        // things up a tad...
        while (nShapes > 1) {
            int n=0;
            for (int i=1; i<nShapes;i+=2) {
                shapes[i-1].add(shapes[i]);
                shapes[n++] = shapes[i-1];
                shapes[i] = null;
            }

            // make sure we include the last one if odd.
            if ((nShapes&0x1) == 1)
                shapes[n-1].add(shapes[nShapes-1]);
            nShapes = nShapes/2;
        }
        if (nShapes == 1)
            shape.append(shapes[0], false);
    }

    /**
     * Perform hit testing for coordinate at x, y.
     *
     * @param x the x coordinate of the point to be tested.
     * @param y the y coordinate of the point to be tested.
     *
     * @return a TextHit object encapsulating the character index for
     *     successful hits and whether the hit is on the character
     *     leading edge.
     */
    public TextHit hitTestChar(float x, float y) {
        syncLayout();

        TextHit textHit = null;

        int currentChar = 0;
        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            Shape gbounds = gv.getGlyphLogicalBounds(i);
            if (gbounds != null) {
                Rectangle2D gbounds2d = gbounds.getBounds2D();
                // System.out.println("Hit Test: [" + x + ", " + y + "] - " +
                //                    gbounds2d);
                if (gbounds.contains(x, y)) {
                    boolean isRightHalf =
                        (x > (gbounds2d.getX()+(gbounds2d.getWidth()/2d)));
                    boolean isLeadingEdge = !isRightHalf;
                    int charIndex = charMap[currentChar];
                    textHit = new TextHit(charIndex, isLeadingEdge);
                    return textHit;
                }
            }
            currentChar += getCharacterCount(i, i);
            if (currentChar >= charMap.length)
                currentChar = charMap.length-1;
        }
        return textHit;
    }

//protected

    /**
     * Returns the GVTFont to use when rendering the specified
     * character iterator.  This should already be set as an attribute
     * on the aci.
     *
     * @param aci The character iterator to get the font attribute from.
     *
     * @return The GVTFont to use.  */
    protected GVTFont getFont() {
        aci.first();
        GVTFont gvtFont = (GVTFont)aci.getAttributes().get
            (GVTAttributedCharacterIterator.TextAttribute.GVT_FONT);

        if (gvtFont != null) 
            return gvtFont;

        // shouldn't get here
        return new AWTGVTFont(aci.getAttributes());
    }

    /**
     * Returns a shape describing the overline decoration for a given ACI.
     */
    protected Shape getOverlineShape() {
        double y = metrics.getOverlineOffset();
        float overlineThickness = metrics.getOverlineThickness();

        // need to move the overline a bit lower,
        // not sure if this is correct behaviour or not
        y += overlineThickness;

        // Not certain what should be done here...
        // aci.first();
        // Float dy = (Float) aci.getAttribute(DY);
        // if (dy != null)
        //     y += dy.floatValue();

        Stroke overlineStroke =
            new BasicStroke(overlineThickness);
        Rectangle2D logicalBounds = gv.getLogicalBounds();

        return overlineStroke.createStrokedShape(
                           new java.awt.geom.Line2D.Double(
                           logicalBounds.getMinX() + overlineThickness/2.0, offset.getY()+y,
                           logicalBounds.getMaxX() - overlineThickness/2.0, offset.getY()+y));
    }

    /**
     * Returns a shape describing the strikethrough line for a given ACI.
     */
    protected Shape getUnderlineShape() {

        double y = metrics.getUnderlineOffset();
        float underlineThickness = metrics.getUnderlineThickness();

        // need to move the underline a bit lower,
        // not sure if this is correct behaviour or not
        y += underlineThickness*1.5;

        BasicStroke underlineStroke =
            new BasicStroke(underlineThickness);

        // Not certain what should be done here...
        // aci.first();
        // Float dy = (Float) aci.getAttribute(DY);
        // if (dy != null)
        //     y += dy.floatValue();

        Rectangle2D logicalBounds = gv.getLogicalBounds();

        return underlineStroke.createStrokedShape(
                           new java.awt.geom.Line2D.Double(
                           logicalBounds.getMinX() + underlineThickness/2.0, offset.getY()+y,
                           logicalBounds.getMaxX() - underlineThickness/2.0, offset.getY()+y));
    }

    /**
     * Returns a shape describing the strikethrough line for a given ACI.
     */
    protected Shape getStrikethroughShape() {
        double y = metrics.getStrikethroughOffset();
        float strikethroughThickness = metrics.getStrikethroughThickness();

        Stroke strikethroughStroke =
            new BasicStroke(strikethroughThickness);

        // Not certain what should be done here...
        // aci.first();
        // Float dy = (Float) aci.getAttribute(DY);
        // if (dy != null)
        //     y += dy.floatValue();

        Rectangle2D logicalBounds = gv.getLogicalBounds();
        return strikethroughStroke.createStrokedShape(
                           new java.awt.geom.Line2D.Double(
                           logicalBounds.getMinX() + strikethroughThickness/2.0, offset.getY()+y,
                           logicalBounds.getMaxX() - strikethroughThickness/2.0, offset.getY()+y));
    }


    /**
     * Explicitly lays out each of the glyphs in the glyph
     * vector. This will handle any glyph position adjustments such as
     * dx, dy and baseline offsets.  It will also handle vertical
     * layouts.
     *
     * @param applyOffset Specifies whether or not to add the offset position
     * to each of the glyph positions.  */
    protected void doExplicitGlyphLayout() {

        this.gv.performDefaultLayout();

        float baselineAscent 
            = vertical ?
            (float) gv.getLogicalBounds().getWidth() :
            (metrics.getAscent() + Math.abs(metrics.getDescent()));

        int numGlyphs = gv.getNumGlyphs();
        // System.out.println("NumGlyphs: " + numGlyphs);

        float[] gp = gv.getGlyphPositions(0, numGlyphs+1, null);
        float verticalFirstOffset = 0f;
        float horizontalFirstOffset = 0f;

        boolean glyphOrientationAuto = isGlyphOrientationAuto();
        int glyphOrientationAngle = 0;
        if (!glyphOrientationAuto) {
            glyphOrientationAngle = getGlyphOrientationAngle();
        }
        int i=0;
        int aciStart = aci.getBeginIndex();
        int aciIndex = 0;
        char ch = aci.first();
        int runLimit = aciIndex+aciStart;

        Float x=null, y=null, dx=null, dy=null, rotation=null;
        Object baseline=null;

        float shift_x_pos = 0;
        float shift_y_pos = 0;
        float curr_x_pos = (float)offset.getX();
        float curr_y_pos = (float)offset.getY();

        while (i < numGlyphs) {
            //System.out.println("limit: " + runLimit + ", " + aciIndex);
            if (aciIndex+aciStart >= runLimit) {
                runLimit = aci.getRunLimit(runAtts);
                x        = (Float) aci.getAttribute(X);
                y        = (Float) aci.getAttribute(Y);
                dx       = (Float) aci.getAttribute(DX);
                dy       = (Float) aci.getAttribute(DY);
                rotation = (Float) aci.getAttribute(ROTATION);
                baseline = aci.getAttribute(BASELINE_SHIFT);
            }

            GVTGlyphMetrics gm = gv.getGlyphMetrics(i);

            if (i==0) {
                if (isVertical()) {
                    if (glyphOrientationAuto) {
                        if (isLatinChar(ch)) {
                            // it will be rotated 90
                            verticalFirstOffset = 0f;
                        } else {
                            // it won't be rotated
                            verticalFirstOffset = 
                                (float)gm.getBounds2D().getHeight();
                        }
                    } else {
                        if (glyphOrientationAngle == 0) {
                            verticalFirstOffset = 
                                (float)gm.getBounds2D().getHeight();
                        } else {
                            // 90, 180, 270
                            verticalFirstOffset = 0f;
                        }
                    }
                } else {
                    if ((glyphOrientationAngle == 270)) {
                        horizontalFirstOffset = 
                            (float)gm.getBounds2D().getHeight();
                    } else {
                        // 0, 90, 180
                        horizontalFirstOffset = 0;
                    }
                }
            } else {
                if (glyphOrientationAuto && 
                    (verticalFirstOffset == 0f)
                    && !isLatinChar(ch)) {

                    verticalFirstOffset = (float)gm.getBounds2D().getHeight();
                }
            }

            // ox and oy are origin adjustments for each glyph,
            // computed on the basis of baseline-shifts, etc.
            float ox = 0f;
            float oy = 0f;
            float glyphOrientationRotation = 0f;
            float glyphRotation = 0f;


            if (ch != CharacterIterator.DONE) {
                if (vertical) {
                    if (glyphOrientationAuto) {
                        if (isLatinChar(ch)) {
                            // If character is Latin, then rotate by
                            // 90 degrees
                            glyphOrientationRotation = (float) (Math.PI / 2f);
                        } else {
                            glyphOrientationRotation = 0f;
                        }
                    } else {
                        glyphOrientationRotation = (float)Math.toRadians(glyphOrientationAngle);
                    }
                    if (textPath != null) {
                        // if vertical and on a path, any x's are ignored
                        x = null;
                    }
                } else {
                    glyphOrientationRotation = (float)Math.toRadians(glyphOrientationAngle);
                    if (textPath != null) {
                        // if horizontal and on a path, any y's are ignored
                        y = null;
                    }
                }

                // calculate the total rotation for this glyph
                if (rotation == null || rotation.isNaN()) {
                    glyphRotation = glyphOrientationRotation;
                } else {
                    glyphRotation = (rotation.floatValue() +
                                     glyphOrientationRotation);
                }

                if ((x != null) && !x.isNaN()) {
                    if (i == 0)  
                        shift_x_pos = (float)(x.floatValue()-offset.getX());
                    curr_x_pos = x.floatValue()-shift_x_pos;
                } 
                if (dx != null && !dx.isNaN()) {
                    curr_x_pos += dx.floatValue();
                }

                if ((y != null) && !y.isNaN()) {
                    if (i == 0)  
                        shift_y_pos = (float)(y.floatValue()-offset.getY());
                    curr_y_pos = y.floatValue()-shift_y_pos;
                } 
                if (dy != null && !dy.isNaN()) {
                    curr_y_pos += dy.floatValue();
                } else if (i > 0) {
                    curr_y_pos += gp[i*2 + 1]-gp[i*2 - 1];
                }

                float baselineAdjust = 0f;
                if (baseline != null) {
                    if (baseline instanceof Integer) {
                        if (baseline==TextAttribute.SUPERSCRIPT_SUPER) {
                            baselineAdjust = baselineAscent*0.5f;
                        } else if (baseline==TextAttribute.SUPERSCRIPT_SUB) {
                            baselineAdjust = -baselineAscent*0.5f;
                        }
                    } else if (baseline instanceof Float) {
                        baselineAdjust = ((Float) baseline).floatValue();
                    }
                    if (vertical) {
                        ox = baselineAdjust;
                    } else {
                        oy = -baselineAdjust;
                    }
                }

                if (vertical) {
                    // offset due to rotation of first character
                    oy += verticalFirstOffset;

                    if (glyphOrientationAuto) {
                        if (isLatinChar(ch)) {
                            ox += metrics.getStrikethroughOffset();
                        } else {
                            Rectangle2D glyphBounds 
                                = gv.getGlyphVisualBounds(i).getBounds2D();
                            ox -= (float)((glyphBounds.getMaxX() - gp[2*i]) - 
                                          glyphBounds.getWidth()/2);
                        }
                    } else {
                        // center the character if it's not auto orient
                        Rectangle2D glyphBounds 
                            = gv.getGlyphVisualBounds(i).getBounds2D();
                        if (glyphOrientationAngle == 0) {
                            ox -= (float)((glyphBounds.getMaxX() - gp[2*i]) - 
                                          glyphBounds.getWidth()/2);
                        } else if (glyphOrientationAngle == 180) {
                            ox += (float)((glyphBounds.getMaxX() - gp[2*i]) - 
                                          glyphBounds.getWidth()/2);
                        } else if (glyphOrientationAngle == 90) {
                            ox += metrics.getStrikethroughOffset();
                        } else { // 270
                            ox -= metrics.getStrikethroughOffset();
                        }
                    }
                } else {
                    ox += horizontalFirstOffset;
                    if (glyphOrientationAngle == 90) {
                        oy -= gm.getHorizontalAdvance();
                    } else if (glyphOrientationAngle == 180) {
                        oy -= metrics.getAscent();
                    }
                }
            }

            // set the new glyph position
            gv.setGlyphPosition(i, new Point2D.Float
                                (curr_x_pos+ox,curr_y_pos+oy));

            // calculte the position of the next glyph
            if (!ArabicTextHandler.arabicCharTransparent(ch)) {
                // only apply the advance if the current char is not transparen
                if (vertical) {
                    float advanceY = 0;
                    if (glyphOrientationAuto) {
                        if (isLatinChar(ch)) {
                            advanceY = gm.getHorizontalAdvance();
                        } else {
                            advanceY = gm.getVerticalAdvance();
                        }
                    } else {
                        if ((glyphOrientationAngle ==   0) ||
                            (glyphOrientationAngle == 180)) {
                            advanceY = gm.getVerticalAdvance();
                        } else if (glyphOrientationAngle == 90) {
                            advanceY = gm.getHorizontalAdvance();
                        } else { // 270
                            advanceY = gm.getHorizontalAdvance();
                            // need to translate so that the spacing
                            // between chars is correct
                            gv.setGlyphTransform
                                (i, AffineTransform.getTranslateInstance
                                 (0, advanceY));
                        }
                    }
                    curr_y_pos += advanceY;
                } else {
                    float advanceX = 0;
                    if (glyphOrientationAngle ==   0) {
                        advanceX = gm.getHorizontalAdvance();
                    } else if (glyphOrientationAngle == 180) {
                        advanceX = gm.getHorizontalAdvance();
                        // need to translate so that the spacing
                        // between chars is correct
                        gv.setGlyphTransform
                            (i, AffineTransform.getTranslateInstance
                             (advanceX, 0));
                    } else {
                        // 90, 270
                        advanceX = gm.getVerticalAdvance();
                    }
                    curr_x_pos += advanceX;
                }
            }

            // rotate the glyph
            if (glyphRotation != 0f) {
                AffineTransform glyphTransform = gv.getGlyphTransform(i);
                if (glyphTransform == null) {
                    glyphTransform = new AffineTransform();
                }
                glyphTransform.rotate(glyphRotation);
                gv.setGlyphTransform(i, glyphTransform);
            }

            aciIndex += gv.getCharacterCount(i,i);
            if (aciIndex >= charMap.length)
                aciIndex = charMap.length-1;
            ch = aci.setIndex(aciIndex+aciStart);
            i++;
        }
        // Update last glyph pos
        gv.setGlyphPosition(i, new Point2D.Float(curr_x_pos,curr_y_pos));

        advance = new Point2D.Float((float)(curr_x_pos - offset.getX()), 
                                    (float)(curr_y_pos - offset.getY()));

        layoutApplied  = true;
        spacingApplied = false;
        glyphAdvances  = null;
        pathApplied    = false;
    }

    /**
     * Does any spacing adjustments that may have been specified.
     */
    protected void adjustTextSpacing() {

        if (spacingApplied) 
            // Nothing to do...
            return;

        if (!layoutApplied)
            // Must have clean layout to do spacing...
            doExplicitGlyphLayout();

        aci.first();
        Boolean customSpacing =  (Boolean) aci.getAttribute(
               GVTAttributedCharacterIterator.TextAttribute.CUSTOM_SPACING);
        if ((customSpacing != null) && customSpacing.booleanValue()) {
            advance = doSpacing
                ((Float) aci.getAttribute
                 (GVTAttributedCharacterIterator.TextAttribute.KERNING),
                 (Float) aci.getAttribute
                 (GVTAttributedCharacterIterator.TextAttribute.LETTER_SPACING),
                 (Float) aci.getAttribute
                 (GVTAttributedCharacterIterator.TextAttribute.WORD_SPACING));
            // Basic layout is now messed up...
            layoutApplied  = false;
        }

        // This will clear layoutApplied if it mucks with the current
        // character positions.
        applyStretchTransform(!adjSpacing);

        spacingApplied = true;
        pathApplied    = false;
    }

    /**
     * Performs any spacing adjustments required and returns the new advance
     * value.
     *
     * @param kern The kerning adjustment to apply to the space
     * between each char.
     * @param letterSpacing The amount of spacing required between each char.
     * @param wordSpacing The amount of spacing required between each word.  */
    protected Point2D doSpacing(Float kern,
                                Float letterSpacing,
                                Float wordSpacing) {
        boolean autoKern = true;
        boolean doWordSpacing = false;
        boolean doLetterSpacing = false;
        float kernVal = 0f;
        float letterSpacingVal = 0f;
        float wordSpacingVal = 0f;

        if ((kern instanceof Float) && (!kern.isNaN())) {
            kernVal = kern.floatValue();
            autoKern = false;
            //System.out.println("KERNING: "+kernVal);
        }
        if ((letterSpacing instanceof Float) && (!letterSpacing.isNaN())) {
            letterSpacingVal = letterSpacing.floatValue();
            doLetterSpacing = true;
            //System.out.println("LETTER-SPACING: "+letterSpacingVal);
        }
        if ((wordSpacing instanceof Float) && (!wordSpacing.isNaN())) {
            wordSpacingVal = wordSpacing.floatValue();
            doWordSpacing = true;
            //System.out.println("WORD_SPACING: "+wordSpacingVal);
        }

        int numGlyphs = gv.getNumGlyphs();

        float dx = 0f;
        float dy = 0f;
        Point2D newPositions[] = new Point2D[numGlyphs+1];
        Point2D prevPos = gv.getGlyphPosition(0);
        float x = (float) prevPos.getX();
        float y = (float) prevPos.getY();

        Point2D lastCharAdvance
            = new Point2D.Double(advance.getX() - (gv.getGlyphPosition(numGlyphs-1).getX() - x),
                                 advance.getY() - (gv.getGlyphPosition(numGlyphs-1).getY() - y));

        try {
            // do letter spacing first
            if ((numGlyphs > 1) && (doLetterSpacing || !autoKern)) {
                for (int i=1; i<=numGlyphs; ++i) {
                    Point2D gpos = gv.getGlyphPosition(i);
                    dx = (float)gpos.getX()-(float)prevPos.getX();
                    dy = (float)gpos.getY()-(float)prevPos.getY();
                    if (autoKern) {
                        if (vertical) dy += letterSpacingVal;
                        else dx += letterSpacingVal;
                    } else {
                        // apply explicit kerning adjustments,
                        // discarding any auto-kern dx values
                        if (vertical) {
                            dy = (float)
                            gv.getGlyphMetrics(i-1).getBounds2D().getHeight()+
                                kernVal + letterSpacingVal;
                        } else {
                            dx = (float)
                            gv.getGlyphMetrics(i-1).getBounds2D().getWidth()+
                                kernVal + letterSpacingVal;
                        }
                    }
                    x += dx;
                    y += dy;
                    newPositions[i] = new Point2D.Float(x, y);
                    prevPos = gpos;
                }

                for (int i=1; i<=numGlyphs; ++i) { // assign the new positions
                    if (newPositions[i] != null) {
                        gv.setGlyphPosition(i, newPositions[i]);
                    }
                }
            }

             // adjust the advance of the last character
            if (autoKern) {
                if (vertical) {
                    lastCharAdvance.setLocation(lastCharAdvance.getX(),
                            lastCharAdvance.getY() + letterSpacingVal);
                } else {
                    lastCharAdvance.setLocation(lastCharAdvance.getX()
                            + letterSpacingVal, lastCharAdvance.getY());
                }
            } else {
                if (vertical) {
                    lastCharAdvance.setLocation(lastCharAdvance.getX(),
                        gv.getGlyphMetrics(numGlyphs-2).getBounds2D().getHeight()+
                                kernVal + letterSpacingVal);
                } else {
                    lastCharAdvance.setLocation(
                        gv.getGlyphMetrics(numGlyphs-2).getBounds2D().getWidth()+
                                kernVal + letterSpacingVal, lastCharAdvance.getY());
                }
            }


            // now do word spacing
            dx = 0f;
            dy = 0f;
            prevPos = gv.getGlyphPosition(0);
            x = (float) prevPos.getX();
            y = (float) prevPos.getY();

            if ((numGlyphs > 1) && (doWordSpacing)) {
                for (int i = 1; i < numGlyphs; i++) {
                    Point2D gpos = gv.getGlyphPosition(i);
                    dx = (float)gpos.getX()-(float)prevPos.getX();
                    dy = (float)gpos.getY()-(float)prevPos.getY();
                    boolean inWS = false;
                    // while this is whitespace, increment
                    int beginWS = i;
                    int endWS = i;
                    GVTGlyphMetrics gm = gv.getGlyphMetrics(i);

                    // BUG: gm.isWhitespace() fails for latin SPACE glyph!
                    while ((gm.getBounds2D().getWidth()<0.01d) || gm.isWhitespace()) {
                        if (!inWS) inWS = true;
                        if (i == numGlyphs-1) {
                            // white space at the end
                            break;
                        }
                        ++i;
                        ++endWS;
                        gpos = gv.getGlyphPosition(i);
                        gm = gv.getGlyphMetrics(i);
                    }

                    if ( inWS ) {  // apply wordSpacing
                        int nWS = endWS-beginWS;
                        float px = (float) prevPos.getX();
                        float py = (float) prevPos.getY();
                        dx = (float) (gpos.getX() - px)/(nWS+1);
                        dy = (float) (gpos.getY() - py)/(nWS+1);
                        if (vertical) {
                            dy += (float) wordSpacing.floatValue()/(nWS+1);
                        } else {
                            dx += (float) wordSpacing.floatValue()/(nWS+1);
                        }
                        for (int j=beginWS; j<=endWS; ++j) {
                            x += dx;
                            y += dy;
                            newPositions[j] = new Point2D.Float(x, y);
                        }
                    } else {
                        dx = (float) (gpos.getX()-prevPos.getX());
                        dy = (float) (gpos.getY()-prevPos.getY());
                        x += dx;
                        y += dy;
                        newPositions[i] = new Point2D.Float(x, y);
                    }
                    prevPos = gpos;
                }
                Point2D gPos = gv.getGlyphPosition(numGlyphs);
                x += (float) (gPos.getX()-prevPos.getX());
                y += (float) (gPos.getY()-prevPos.getY());
                newPositions[numGlyphs] = new Point2D.Float(x, y);

                for (int i=1; i<=numGlyphs; ++i) { // assign the new positions
                    if (newPositions[i] != null) {
                        gv.setGlyphPosition(i, newPositions[i]);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // calculate the new advance
        double advX = gv.getGlyphPosition(numGlyphs-1).getX()
                     - gv.getGlyphPosition(0).getX();
        double advY = gv.getGlyphPosition(numGlyphs-1).getY()
                     - gv.getGlyphPosition(0).getY();
        Point2D newAdvance = new Point2D.Double(advX + lastCharAdvance.getX(),
                                                advY + lastCharAdvance.getY());
        return newAdvance;
    }

    /**
     * Stretches the text so that it becomes the specified length.
     *
     * @param stretchGlyphs if true xScale, yScale will be applied to
     *                      each glyphs transform.
     */
    protected void applyStretchTransform(boolean stretchGlyphs) {
        if ((xScale == 1) && (yScale==1)) 
            return;

        AffineTransform scaleAT = 
            AffineTransform.getScaleInstance(xScale, yScale);

        int numGlyphs = gv.getNumGlyphs();
        float [] gp   = gv.getGlyphPositions(0, numGlyphs+1, null);

        float initX   = (float) gp[0];
        float initY   = (float) gp[1];
        float dx = 0f;
        float dy = 0f;
        for (int i = 0; i <= numGlyphs; i++) {
            dx = gp[2*i]  -initX;
            dy = gp[2*i+1]-initY;
            gv.setGlyphPosition(i, new Point2D.Float(initX+dx*xScale,
                                                     initY+dy*yScale));

            if ((stretchGlyphs) && (i != numGlyphs)) {
                // stretch the glyph
                AffineTransform glyphTransform = gv.getGlyphTransform(i);
                if (glyphTransform != null) {
                    glyphTransform.preConcatenate(scaleAT);
                    gv.setGlyphTransform(i, glyphTransform);
                } else {
                    gv.setGlyphTransform (i, scaleAT);
                }
            }
        }

        advance = new Point2D.Float((float)(advance.getX()*xScale),
                                    (float)(advance.getY()*yScale));
        // Basic layout is now messed up...
        layoutApplied  = false;
    }

    /**
     * If this layout is on a text path, positions the characters
     * along the path.  
     */
    protected void doPathLayout() {
        if (pathApplied) 
            return;

        if (!spacingApplied)
            // This will layout the text if needed.
            adjustTextSpacing();

        getGlyphAdvances();

        // if doesn't have an attached text path, just return
        if (textPath == null) {
            // We applied the empty path (i.e. do nothing).
            pathApplied = true;
            return;
        }


        boolean horizontal = !isVertical();

        boolean glyphOrientationAuto = isGlyphOrientationAuto();
        int glyphOrientationAngle = 0;
        if (!glyphOrientationAuto) {
            glyphOrientationAngle = getGlyphOrientationAngle();
        }

        float pathLength  = textPath.lengthOfPath();
        float startOffset = textPath.getStartOffset();
        int   numGlyphs   = gv.getNumGlyphs();

        // make sure all glyphs visible again, this maybe just a change in
        // offset so they may have been made invisible in a previous
        // pathLayout call
        for (int i = 0; i < numGlyphs; i++) {
            gv.setGlyphVisible(i, true);
        }

        // calculate the total length of the glyphs, this will become be
        // the length along the path that is used by the text
        float glyphsLength;
        if (horizontal) {
            glyphsLength = (float) gv.getLogicalBounds().getWidth();
        } else {
            glyphsLength = (float) gv.getLogicalBounds().getHeight();
        }

        // check that pathLength and glyphsLength are not 0
        if (pathLength == 0f || glyphsLength == 0f) {
            // We applied the empty path.
            pathApplied = true;
            textPathAdvance = advance;
            return;
        }

        // the current start point of the character on the path
        float currentPosition;
        if (horizontal) {
            currentPosition = (float)offset.getX() + startOffset;
        } else {
            currentPosition = (float)offset.getY() + startOffset;
        }

        // calculate the offset of the first glyph the offset will be
        // 0 if the glyph is on the path (ie. not adjusted by a dy or
        // dx)
        Point2D firstGlyphPosition = gv.getGlyphPosition(0);
        float glyphOffset = 0;   // offset perpendicular to path
        if (horizontal) {
            glyphOffset = (float)(firstGlyphPosition.getY());
        } else {
            glyphOffset = (float)(firstGlyphPosition.getX());
        }

        char ch = aci.first();
        int start       = aci.getBeginIndex();
        int currentChar = 0;
        int lastGlyphDrawn = -1;
        float lastGlyphAdvance = 0;
        // iterate through the GlyphVector placing each glyph
        for (int i = 0; i < numGlyphs; i++) {

            Point2D currentGlyphPosition = gv.getGlyphPosition(i);

            // calculate the advance and offset for the next glyph, do it
            // now before we modify the current glyph position

            float glyphAdvance = 0;  // along path
            float nextGlyphOffset = 0;  // perpendicular to path eg dy or dx
            if (i < gv.getNumGlyphs()-1) {

                Point2D nextGlyphPosition = gv.getGlyphPosition(i+1);
                if (horizontal) {
                    glyphAdvance    = (float)(nextGlyphPosition.getX() -
                                              currentGlyphPosition.getX());
                    nextGlyphOffset = (float)(nextGlyphPosition.getY() -
                                              currentGlyphPosition.getY());
                } else {
                    glyphAdvance    = (float)(nextGlyphPosition.getY() -
                                              currentGlyphPosition.getY());
                    nextGlyphOffset = (float)(nextGlyphPosition.getX() -
                                              currentGlyphPosition.getX());
                }
            } else {
                // last glyph, use the glyph metrics
                GVTGlyphMetrics gm = gv.getGlyphMetrics(i);
                if (horizontal) {
                    if ((glyphOrientationAngle == 0) ||
                        (glyphOrientationAngle == 180)) {
                        glyphAdvance = gm.getHorizontalAdvance();
                    } else { // 90 || 270
                        glyphAdvance = gm.getVerticalAdvance();
                    }
                } else {
                    if (glyphOrientationAuto) {
                        if (isLatinChar(ch)) {
                            glyphAdvance = gm.getHorizontalAdvance();
                        } else {
                            glyphAdvance = gm.getVerticalAdvance();
                        }
                    } else {
                        if ((glyphOrientationAngle == 0) ||
                            (glyphOrientationAngle == 180)) {
                            glyphAdvance = gm.getVerticalAdvance();
                        } else { // 90 || 270
                            glyphAdvance = gm.getHorizontalAdvance();
                        }
                    }
                }
            }

            // calculate the center line position for the glyph
            Rectangle2D glyphBounds = gv.getGlyphOutline(i).getBounds2D();
            float glyphWidth = (float) glyphBounds.getWidth();
            float glyphHeight = (float) glyphBounds.getHeight();

            float charMidPos;
            if (horizontal) {
                charMidPos = currentPosition + glyphWidth / 2f;
            } else {
                charMidPos = currentPosition + glyphHeight / 2f;
            }

            // Calculate the actual point to place the glyph around
            Point2D charMidPoint = textPath.pointAtLength(charMidPos);

            // Check if the glyph is actually on the path
            if (charMidPoint != null) {

                // Calculate the normal to the path (midline of glyph)
                float angle = textPath.angleAtLength(charMidPos);

                // Define the transform of the glyph
                AffineTransform glyphPathTransform = new AffineTransform();

                // rotate midline of glyph to be normal to path
                if (horizontal) {
                    glyphPathTransform.rotate(angle);
                } else {
                    glyphPathTransform.rotate(angle-(Math.PI/2));
                }

                // re-apply any offset eg from tspan, or spacing adjust
                if (horizontal) {
                    glyphPathTransform.translate(0, glyphOffset);
                } else {
                    glyphPathTransform.translate(glyphOffset, 0);
                }

                // translate glyph backwards so we rotate about the
                // center of the glyph
                if (horizontal) {
                    if (glyphOrientationAngle ==  270) {
                        glyphPathTransform.translate(glyphWidth / 2f, 0f);
                    } else {
                        // 0 || 90 || 180
                        glyphPathTransform.translate(-glyphWidth / 2f, 0f);
                    } 
                } else {
                    if (glyphOrientationAuto) {
                        if (isLatinChar(ch)) {
                           glyphPathTransform.translate(0f, -glyphHeight/2f);
                        } else {
                            glyphPathTransform.translate(0f, glyphHeight/2f);
                        }
                    } else {
                        if (glyphOrientationAngle ==   0) {
                            glyphPathTransform.translate(0, glyphHeight / 2f);
                        } else {
                            // 90 || 180 || 270
                            glyphPathTransform.translate(0, -glyphHeight / 2f);
                        }
                    }
                }

                // set the new glyph position and transform
                AffineTransform glyphTransform = gv.getGlyphTransform(i);
                if (glyphTransform != null) {
                    glyphPathTransform.concatenate(glyphTransform);
                }

                gv.setGlyphTransform(i, glyphPathTransform);
                gv.setGlyphPosition(i, new Point2D.Double(charMidPoint.getX(),
                                                          charMidPoint.getY()));
                // keep track of the last glyph drawn to make calculating the
                // textPathAdvance value easier later
                lastGlyphDrawn = i;
                lastGlyphAdvance = glyphAdvance;

            } else {
                // not on path so don't render
                gv.setGlyphVisible(i, false);
            }
            currentPosition += glyphAdvance;
            glyphOffset += nextGlyphOffset;
            currentChar += gv.getCharacterCount(i,i);
            if (currentChar >= charMap.length)
                currentChar = charMap.length-1;
            ch = aci.setIndex(currentChar+start);
        }

        // store the position where a following glyph should be drawn,
        // note: this will only be used if the following text layout is not
        //       on a text path
        if (lastGlyphDrawn > -1) {
            Point2D lastGlyphPos = gv.getGlyphPosition(lastGlyphDrawn);
            if (horizontal) {
                textPathAdvance = new Point2D.Double
                    (lastGlyphPos.getX()+lastGlyphAdvance,
                     lastGlyphPos.getY());
            } else {
                textPathAdvance = new Point2D.Double
                    (lastGlyphPos.getX(),
                     lastGlyphPos.getY()+lastGlyphAdvance);
            }
        } else {
            textPathAdvance = new Point2D.Double(0,0);
        }

        // The default layout is junk now...
        layoutApplied  = false;
        // The spacing stuff is junk now.
        spacingApplied = false;
        pathApplied    = true;   
    }

    /**
     * Returns true if the specified character is within one of the Latin
     * unicode character blocks.
     *
     * @param c The char to test.
     *
     * @return True if c is latin.
     */
    protected boolean isLatinChar(char c) {

        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);

        if (block == Character.UnicodeBlock.BASIC_LATIN ||
            block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT ||
            block == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL ||
            block == Character.UnicodeBlock.LATIN_EXTENDED_A ||
            block == Character.UnicodeBlock.LATIN_EXTENDED_B) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Returns whether or not the vertical glyph orientation value is "auto".
     */
    protected boolean isGlyphOrientationAuto() {
        if (!isVertical()) return false;
        aci.first();
        Integer vOrient = (Integer)aci.getAttribute(VERTICAL_ORIENTATION);
        if (vOrient != null) {
            return (vOrient == ORIENTATION_AUTO);
        }
        return true;
    }

    /**
     * Returns the value of the vertical glyph orientation angle. This will be
     * one of 0, 90, 180 or 270.
     */
    protected int getGlyphOrientationAngle() {

        int glyphOrientationAngle = 0;

        aci.first();
        Float angle;

        if (isVertical()) {
            angle = (Float)aci.getAttribute(VERTICAL_ORIENTATION_ANGLE);
        } else {
            angle = (Float)aci.getAttribute(HORIZONTAL_ORIENTATION_ANGLE);
        }

        if (angle != null) {
            glyphOrientationAngle = (int)angle.floatValue();
        }

        // if not one of 0, 90, 180 or 270, round to nearest value
        if ((glyphOrientationAngle !=   0) || (glyphOrientationAngle !=  90) ||
            (glyphOrientationAngle != 180) || (glyphOrientationAngle != 270)) {

            while (glyphOrientationAngle < 0) {
                glyphOrientationAngle += 360;
            }

            while (glyphOrientationAngle >= 360) {
                glyphOrientationAngle -= 360;
            }

            if ((glyphOrientationAngle <= 45) || 
                (glyphOrientationAngle > 315)) {
                glyphOrientationAngle = 0;
            } else if ((glyphOrientationAngle > 45) && 
                       (glyphOrientationAngle <= 135)) {
                glyphOrientationAngle = 90;
            } else if ((glyphOrientationAngle > 135) && 
                       (glyphOrientationAngle <= 225)) {
                glyphOrientationAngle = 180;
            } else {
                glyphOrientationAngle = 270;
            }
        }
        return glyphOrientationAngle;
    }


    // Issues: 
    //   Should the font size of non-printing chars affect line spacing?
    //   Does line breaking get done before/after ligatures?
    //   What should be done if the next glyph does not fit in the
    //   flow rect (very narrow flow rect).
    //      Print the one char anyway.
    //      Go to the next flow rect.
    //   Should dy be considered for line offsets? (super scripts)
    //   Should p's & br's carry over from flow rect to flow rect if
    //   so how much????

    // In cases where 1/2 leading is negative (lineBox is smaller than
    // lineAscent+lineDescent) do we use the lineBox (some glyphs will
    // go outside flowRegion) or the visual box.  My feeling is that
    // we should use the larger of the two.

    // We stated that for empty para elements it moves to the new flow
    // region if the zero-height line is outside the flow region.  In
    // this case the para elements top-margin is used in the new flow
    // region (and it's bottom-margin is collapsed with the next
    // flowPara element if any).  What happens when the first line of
    // a non-empty flowPara doesn't fit (so the top margin does fit
    // but the first line of text doesn't).  I think the para should
    // move to the next flow region and the top margin should apply in
    // the new flow region.  The top margin does not apply if
    // subsequint lines move to a new flow region.

    // Note that line wrapping is done on visual bounds of glyph
    // not the glyph advance (which often includes some whitespace
    // after the right edge of the glyph char).

    //
    //   How are Margins done?  Can't figure out Box size until
    //   after we know the margins.
    //   Should 'A' element be allowed in 'flowPara'.
    //   
    //   For Full justification:
    //       Streach glyphs to fill line? (attribute?)
    //       What to do with partial line (last line in 'p', 'line'
    //       element, or 'div' element), still full justify, just left 
    //       justify, attribute?
    //       What to do when only one glyph on line? left or center or stretch?
    //       For full to look good I think the line must be able to squeeze a
    //         bit as well as grow (pretty easy to add).
    //
    // This Only does horizontal languages.
    // Supports Zero Width Spaces (0x200B) Zero Width Joiner( 0x200D), 
    // and soft hyphens (0x00AD).
    // 
    // Does not properly handle Bi-DI languages (does text wrapping on
    // display order not logical order).

    /**
     * This will wrap the text associated with <tt>aci</tt> and
     * <tt>layouts</tt>.
     * @param acis An array of Attributed Charater Iterators containing the 
     *             text to wrap.  There is one aci per text chunk
     *             (which maps to flowPara elements. Used to access
     *             font, paragraph, and line break info.
     * @param chunkLayouts A List of List of GlyphLayout objects.  There
     *                     is a List of GlyphLayout objects for each
     *                     flowPara element.  There is a GlyphLayout
     *                     for approximately each sub element in the 
     *                     flowPara element.
     * @param flowRects A List of Rectangle2D representing the regions
     *                  to flow text into.
     */
    public static void textWrapTextChunk(AttributedCharacterIterator [] acis,
                                         List chunkLayouts,
                                         List flowRects) {
        int numChunks = acis.length;
        // System.out.println("Len: " + acis.length + " Size: " + 
        //                    chunkLayouts.size());

        // Make a list of the GlyphVectors so we can construct a
        // multiGlyphVector that makes them all look like one big
        // glyphVector
        GVTGlyphVector [] gvs            = new GVTGlyphVector[acis.length];
        List           [] chunkLineInfos = new List          [acis.length];
        GlyphIterator  [] gis            = new GlyphIterator [acis.length];
        Iterator clIter = chunkLayouts.iterator();

        // Get an iterator for the flow rects.
        Iterator flowRectsIter = flowRects.iterator();
        // Get info for new flow rect.
        RegionInfo currentRegion = null;
        float y0, x0, width, height=0;
        if (flowRectsIter.hasNext()) {
            currentRegion = (RegionInfo) flowRectsIter.next();
            height = (float) currentRegion.getHeight();
        }

        boolean lineHeightRelative = true;
        float lineHeight           = 1.0f;
        float nextLineMult         = 0.0f;
        float dy                   = 0.0f;

        //
        Point2D.Float verticalAlignOffset = new Point2D.Float(0,0);

        //System.out.println("Chunks: " + numChunks);
        
        float prevBotMargin = 0;
        for (int chunk=0; clIter.hasNext(); chunk++) {
            // System.out.println("Chunk: " + chunk);
            AttributedCharacterIterator aci = acis[chunk];
            if (currentRegion != null)
            {
                List extraP = (List)aci.getAttribute(FLOW_EMPTY_PARAGRAPH);
                if (extraP != null) {
                    Iterator epi = extraP.iterator();
                    while (epi.hasNext()) {
                        MarginInfo emi = (MarginInfo)epi.next();
                        float inc = ((prevBotMargin > emi.getTopMargin()) 
                                     ? prevBotMargin 
                                     : emi.getTopMargin());
                        if ((dy + inc <= height) &&
                            !emi.isFlowRegionBreak()) {
                            dy += inc;
                        } else {
                            // Move to next flow region..
                            if (!flowRectsIter.hasNext()) {
                                currentRegion = null;
                                break; // No flow rect stop layout here...
                            }

                            // NEXT FLOW REGION
                            currentRegion = (RegionInfo) flowRectsIter.next();
                            height = (float) currentRegion.getHeight();
                            // start a new alignment offset for this flow rect.
                            verticalAlignOffset = new Point2D.Float(0,0);

                            // New rect so no previous row to consider...
                            dy        = emi.getTopMargin();
                        }
                        prevBotMargin = emi.getBottomMargin();
                    }

                    if (currentRegion == null) break;
                }
            }

            List gvl = new LinkedList();
            List layouts = (List)clIter.next();
            Iterator iter = layouts.iterator();
            while (iter.hasNext()) {
                GlyphLayout gl = (GlyphLayout)iter.next();
                gvl.add(gl.getGlyphVector());
            }
            GVTGlyphVector gv = new MultiGlyphVector(gvl);
            gvs[chunk] = gv;
            int numGlyphs = gv.getNumGlyphs();

            // System.out.println("Glyphs: " + numGlyphs);

            aci.first();
            MarginInfo mi = (MarginInfo)aci.getAttribute(FLOW_PARAGRAPH);
            if (mi == null) {
              continue;
            }
            int justification = mi.getJustification();

            if (currentRegion == null) {
                for(int idx=0; idx <numGlyphs; idx++) 
                    gv.setGlyphVisible(idx, false);
                continue;
            }

            float inc = ((prevBotMargin > mi.getTopMargin()) 
                         ? prevBotMargin : mi.getTopMargin());
            if (dy + inc <= height) {
                dy += inc;
            } else {
                // Move to next flow region..
                // NEXT FLOW REGION
                if (!flowRectsIter.hasNext()) {
                    currentRegion = null;
                    break; // No flow rect stop layout here...
                }

                // NEXT FLOW REGION
                currentRegion = (RegionInfo) flowRectsIter.next();
                height = (float) currentRegion.getHeight();
                // start a new alignment offset for this flow rect..
                verticalAlignOffset = new Point2D.Float(0,0);

                            // New rect so no previous row to consider...
                dy        = mi.getTopMargin();
            }
            prevBotMargin = mi.getBottomMargin();

            float leftMargin = mi.getLeftMargin();
            float rightMargin = mi.getRightMargin();
            if (((GlyphLayout)layouts.get(0)).isLeftToRight()) {
                leftMargin += mi.getIndent();
            } else {
                rightMargin += mi.getIndent();
            }

            x0 = (float) currentRegion.getX() + leftMargin;
            y0 = (float) currentRegion.getY();
            width = (float) (currentRegion.getWidth() - 
                             (leftMargin + rightMargin));
            height = (float) currentRegion.getHeight();
            
            List lineInfos = new LinkedList();
            chunkLineInfos[chunk] = lineInfos;

            float prevDesc = 0.0f;
            GlyphIterator gi = new GlyphIterator(aci, gv);
            gis[chunk] = gi;

            GlyphIterator breakGI  = null, newBreakGI = null;

            if (!gi.done() && !gi.isPrinting()) {
                // This will place any preceeding whitespace on an
                // imaginary line that preceeds the real first line of
                // the paragraph, also calculate the vertical
                // alignment offset, this will be repeated until the
                // last line in the flow rect.
               updateVerticalAlignOffset(verticalAlignOffset, 
                                         currentRegion, dy);
               lineInfos.add(gi.newLine
                             (new Point2D.Float(x0, y0+dy), 
                              width, true, verticalAlignOffset));
            }


            GlyphIterator lineGI   =  gi.copy();
            boolean firstLine = true;
            while (!gi.done()) {
                boolean doBreak = false;
                boolean partial = false;

                if (gi.isPrinting() && (gi.getAdv() > width)) {
                    if (breakGI == null) {
                        // first char on line didn't fit.
                        // move to next flow rect.
                        if (!flowRectsIter.hasNext()) {
                            currentRegion = null;
                            gi = lineGI.copy(gi);
                            break; // No flow rect stop layout here...
                        }

                        // NEXT FLOW REGION
                        currentRegion = (RegionInfo) flowRectsIter.next();
                        x0 = (float) currentRegion.getX() + leftMargin;
                        y0 = (float) currentRegion.getY();
                        width = (float) (currentRegion.getWidth() -
                                        (leftMargin+rightMargin));
                        height = (float) currentRegion.getHeight();
                        // start a new alignment offset for this flow rect..
                        verticalAlignOffset = new Point2D.Float(0,0);

                        // New rect so no previous row to consider...
                        dy = firstLine ? mi.getTopMargin() : 0;
                        ;
                        prevDesc  = 0;
                        gi = lineGI.copy(gi);
                        continue;
                    }

                    gi = breakGI.copy(gi);  // Back up to break loc...

                    nextLineMult = 1;
                    doBreak = true;
                    partial = false;
                } else if (gi.isLastChar()) {
                    nextLineMult = 1;
                    doBreak = true;
                    partial = true;
                } 
                int lnBreaks = gi.getLineBreaks();
                if (lnBreaks != 0) {
                    if (doBreak)
                        nextLineMult -= 1;
                    nextLineMult += lnBreaks;
                    doBreak = true;
                    partial = true;
                }

                if (!doBreak) {
                    // System.out.println("No Brk Adv: " + gi.getAdv());
                    // We don't need to break the line because of this glyph
                    // So we just check if we need to update our break loc.
                    if ((gi.isBreakChar()) ||
                        (breakGI == null)  ||
                        (!breakGI.isBreakChar())) {
                        // Make this the new break if curr char is a
                        // break char or we don't have any break chars
                        // yet, or our current break char is also not
                        // a break char.
                        newBreakGI = gi.copy(newBreakGI);
                        gi.nextChar();
                        if (gi.getChar() != GlyphIterator.ZERO_WIDTH_JOINER) {
                            GlyphIterator tmpGI = breakGI;
                            breakGI = newBreakGI;
                            newBreakGI = tmpGI;
                        }
                    } else {
                        gi.nextChar();
                    }
                    continue;
                }

                // System.out.println("   Brk Adv: " + gi.getAdv());

                // We will now attempt to break the line just
                // after 'gi'.

                // Note we are trying to figure out where the current
                // line is going to be placed (not the next line).  We
                // must wait until we have a potential line break so
                // we know how tall the line is.

                // Get the nomial line advance based on the
                // largest font we encountered on line...
                float lineSize = gi.getMaxAscent()+gi.getMaxDescent(); 
                float lineBoxHeight;
                if (lineHeightRelative) 
                    lineBoxHeight = gi.getMaxFontSize()*lineHeight;
                else
                    lineBoxHeight = lineHeight;
                float halfLeading = (lineBoxHeight-lineSize)/2;

                float ladv = prevDesc + halfLeading + gi.getMaxAscent();
                float newDesc = halfLeading + gi.getMaxDescent();

                dy += ladv;
                float bottomEdge = newDesc;
                if (newDesc < gi.getMaxDescent()) 
                    bottomEdge = gi.getMaxDescent();

                if ((dy + bottomEdge) > height)  {
                    // The current Line doesn't fit in the
                    // current flow rectangle so we need to
                    // move line to the next flow rect.

                    // System.out.println("Doesn't Fit: " + dy);

                    if (!flowRectsIter.hasNext()) {
                        currentRegion = null;
                        gi = lineGI.copy(gi);
                        break; // No flow rect stop layout here...
                    }

                        // Remember how wide this rectangle is...
                    float oldWidth = width;

                    // Get info for new flow rect.
                    currentRegion = (RegionInfo) flowRectsIter.next();
                    x0     = (float) currentRegion.getX() + leftMargin;
                    y0     = (float) currentRegion.getY();
                    width  = (float)(currentRegion.getWidth() -
                                     (leftMargin+rightMargin));
                    height = (float) currentRegion.getHeight();
                    // start a new alignment offset for this flow rect..
                    verticalAlignOffset = new Point2D.Float(0,0);

                    // New rect so no previous row to consider...
                    dy = firstLine ? mi.getTopMargin() : 0;
                    ;
                    prevDesc  = 0;
                    // previous flows?

                    if (gi.getAdv() > oldWidth) {
                        // need to back up to start of line...
                        gi = lineGI.copy(gi);
                    }
                    continue;
                }

                prevDesc = newDesc + (nextLineMult-1)*lineBoxHeight;
                nextLineMult = 0f;
                updateVerticalAlignOffset(verticalAlignOffset, 
                                          currentRegion, dy + bottomEdge);
                lineInfos.add(gi.newLine
                              (new Point2D.Float(x0, y0 + dy), width, partial, 
                               verticalAlignOffset));

                // System.out.println("Fit: " + dy);
                x0    -= leftMargin;
                width += leftMargin+rightMargin;

                leftMargin  = mi.getLeftMargin();
                rightMargin = mi.getRightMargin();
                x0    += leftMargin;
                width -= leftMargin+rightMargin;

                firstLine = false;
                // The line fits in the current flow rectangle.
                lineGI  = gi.copy(lineGI);
                breakGI = null;
            }
            dy += prevDesc;

            int idx = gi.getGlyphIndex();
            while(idx <numGlyphs) 
                gv.setGlyphVisible(idx++, false);

            if (mi.isFlowRegionBreak()) {
                // Move to next flow region..
                currentRegion = null;
                if (flowRectsIter.hasNext()) {
                    currentRegion = (RegionInfo) flowRectsIter.next();
                    height = (float) currentRegion.getHeight();
                    dy     = mi.getTopMargin();
                    verticalAlignOffset = new Point2D.Float(0,0);
                }
            }
        }

        for (int chunk=0; chunk < acis.length; chunk++) {
            List lineInfos = chunkLineInfos[chunk];
            if (lineInfos == null) continue;

            AttributedCharacterIterator aci = acis[chunk];
            aci.first();
            MarginInfo mi = (MarginInfo)aci.getAttribute(FLOW_PARAGRAPH);
            if (mi == null) {
              continue;
            }
            int justification = mi.getJustification();
            
            GVTGlyphVector gv = gvs[chunk];
            if (gv == null) break;

            GlyphIterator gi = gis[chunk];
            
            layoutChunk(gv, gi.getOrigin(), justification, lineInfos);
        }
    }


    /**
     * Updates the specified verticalAlignmentOffset using the current
     * alignment rule and the heights of the flow rect and the maximum
     * descent of the text.  This method gets for called every line,
     * but only the value that is calculated for the last line of the
     * flow rect is used by the glyph rendering.  This is achieved by
     * creating a new verticalAlignOffset object everytime a new flow
     * rect is encountered, thus a single verticalAlignmentOffset is
     * shared for all {@link LineInfo} objects created for a given
     * flow rect.  The value is calculated by determining the left
     * over space in the flow rect and scaling that value by 1.0 to
     * align to the bottom, 0.5 for middle and 0.0 for top.
     *
     * @param verticalAlignOffset the {@link java.awt.geom.Point2D.Float} object that 
     *                            is storing the alignment offset.
     * @param currentRegion the {@link RegionInfo} object that we 
     *                      are rendering into.
     * @param maxDescent the very lowest point this line reaches.
     */
    public static void updateVerticalAlignOffset
        (Point2D.Float verticalAlignOffset,
         RegionInfo region, float maxDescent)
        {
            float freeSpace = (float)region.getHeight() - maxDescent;
            verticalAlignOffset.setLocation
                (0, region.getVerticalAlignment() * freeSpace);
        }

    public static void layoutChunk(GVTGlyphVector gv, Point2D origin,
                                   int justification,
                                   List lineInfos) {
        Iterator lInfoIter = lineInfos.iterator();
        int numGlyphs      = gv.getNumGlyphs();
        float [] gp        = gv.getGlyphPositions(0, numGlyphs+1, null);
        Point2D.Float lineLoc  = null;
        float         lineAdv  = 0;
        float         lineVAdv = 0;

        float xOrig=(float)origin.getX();
        float yOrig=(float)origin.getY();

        float xScale=1;
        float xAdj=0;
        float charW=0;
        float lineWidth=0;
        boolean partial = false;
        float verticalAlignOffset = 0;

        // This loop goes through and puts glyphs where they belong
        // based on info collected in first trip through glyphVector...
        int lineEnd = 0;
        int i;
        for (i =0; i<numGlyphs; i++) {
            if (i == lineEnd) {
                // Always comes through here on first char...

                // Update offset for new line based on last line length
                xOrig += lineAdv;

                // Get new values for everything...
                if (!lInfoIter.hasNext())
                    break;
                LineInfo li = (LineInfo)lInfoIter.next();
                // System.out.println(li.toString());

                lineEnd   = li.getEndIdx();
                lineLoc   = li.getLocation();
                lineAdv   = li.getAdvance();
                lineVAdv  = li.getVisualAdvance();
                charW     = li.getLastCharWidth();
                lineWidth = li.getLineWidth();
                partial   = li.isPartialLine();
                verticalAlignOffset = li.getVerticalAlignOffset().y;

                xAdj = 0;
                xScale = 1;
                // Recalc justification info.
                switch (justification) {
                case 0: default: break;                  // Left
                case 1:                                  // Center
                    xAdj = (lineWidth - lineVAdv) / 2;
                    break;
                case 2:                                  // Right
                    xAdj = lineWidth - lineVAdv;
                    break;
                case 3:                                  // Full
                    if ((!partial) && (lineEnd != i+1)) {
                        // More than one char on line...
                        // Scale char spacing to fill line.
                        xScale = (lineWidth-charW)/(lineVAdv-charW);
                    }
                    break;
                }
            }
            float x = lineLoc.x + (gp[2*i]  -xOrig)*xScale+xAdj;
            float y = lineLoc.y + ((gp[2 * i + 1] - yOrig) + 
                                   verticalAlignOffset);
            gv.setGlyphPosition(i, new Point2D.Float(x, y));
        }

        float x = xOrig;
        float y = yOrig;
        if (lineLoc != null) {
          x = lineLoc.x + (gp[2*i]  -xOrig)*xScale+xAdj;
            y = lineLoc.y + (gp[2 * i + 1] - yOrig) + verticalAlignOffset;
        }
        gv.setGlyphPosition(i, new Point2D.Float(x, y));
    }

    /**
     * Return true is the character index is represented by glyphs 
     * in this layout.
     *
     * @param index index of the character in the ACI.
     * @return true if the layout represents that character.
     */
    public boolean hasCharacterIndex(int index){

        for (int n=0; n<charMap.length; n++) {
            if (index == charMap[n])
                return true;
        }
        return false;
    }

    /**
     * Return true if this text run represents
     * an alt glyph.
     */
    public boolean isAltGlyph(){
        return this.isAltGlyph;
    }
}
