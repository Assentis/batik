/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.awt.Color;
import java.awt.Composite;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.io.StringReader;

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.StringTokenizer;

import org.apache.batik.css.engine.CSSEngineEvent;
import org.apache.batik.css.engine.CSSStylableElement;
import org.apache.batik.css.engine.StyleMap;
import org.apache.batik.css.engine.SVGCSSEngine;
import org.apache.batik.css.engine.value.ComputedValue;
import org.apache.batik.css.engine.value.ListValue;
import org.apache.batik.css.engine.value.Value;

import org.apache.batik.dom.svg.SVGContext;
import org.apache.batik.dom.svg.SVGTextContent;
import org.apache.batik.dom.svg.SVGOMDocument;
import org.apache.batik.dom.svg.SVGOMElement;
import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.dom.util.XMLSupport;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.text.Mark;
import org.apache.batik.gvt.text.TextHit;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import org.apache.batik.gvt.text.TextPath;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.gvt.font.UnresolvedFontFamily;
import org.apache.batik.gvt.renderer.StrokingTextPainter;
import org.apache.batik.gvt.text.TextSpanLayout;
import org.apache.batik.gvt.font.GVTGlyphVector;
import org.apache.batik.gvt.font.GVTGlyphMetrics;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;

/**
 * Bridge class for the &lt;text> element.
 *
 * @author <a href="stephane@hillion.org">Stephane Hillion</a>
 * @author <a href="bill.haneman@ireland.sun.com">Bill Haneman</a>
 * @version $Id$
 */
public class SVGTextElementBridge extends AbstractGraphicsNodeBridge 
    implements SVGTextContent {

    protected final static Integer ZERO = new Integer(0);

    protected AttributedString layoutedText;

    /**
     * Constructs a new bridge for the &lt;text> element.
     */
    public SVGTextElementBridge() {}

    /**
     * Returns 'text'.
     */
    public String getLocalName() {
        return SVG_TEXT_TAG;
    }

    /**
     * Returns a new instance of this bridge.
     */
    public Bridge getInstance() {
        return new SVGTextElementBridge();
    }

    /**
     * Creates a <tt>GraphicsNode</tt> according to the specified parameters.
     *
     * @param ctx the bridge context to use
     * @param e the element that describes the graphics node to build
     * @return a graphics node that represents the specified element
     */
    public GraphicsNode createGraphicsNode(BridgeContext ctx, Element e) {
        TextNode node = (TextNode)super.createGraphicsNode(ctx, e);
        if (node == null) {
            return null;
        }
        // specify the text painter to use
        if (ctx.getTextPainter() != null) {
            node.setTextPainter(ctx.getTextPainter());
        }
        // 'text-rendering' and 'color-rendering'
        RenderingHints hints = CSSUtilities.convertTextRendering(e, null);
        hints = CSSUtilities.convertColorRendering(e, hints);
        if (hints != null) {
            node.setRenderingHints(hints);
        }
        node.setLocation(getLocation(ctx, e));

        return node;
    }

    /**
     * Creates the GraphicsNode depending on the GraphicsNodeBridge
     * implementation.
     */
    protected GraphicsNode instantiateGraphicsNode() {
        return new TextNode();
    }

    /**
     * Returns the text node location according to the 'x' and 'y'
     * attributes of the specified text element.
     *
     * @param ctx the bridge context to use
     * @param e the text element
     */
    protected Point2D getLocation(BridgeContext ctx, Element e) {
        UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, e);

        // 'x' attribute - default is 0
        String s = e.getAttributeNS(null, SVG_X_ATTRIBUTE);
        float x = 0;
        if (s.length() != 0) {
            StringTokenizer st = new StringTokenizer(s, ", ", false);
            x = UnitProcessor.svgHorizontalCoordinateToUserSpace
                (st.nextToken(), SVG_X_ATTRIBUTE, uctx);
        }

        // 'y' attribute - default is 0
        s = e.getAttributeNS(null, SVG_Y_ATTRIBUTE);
        float y = 0;
        if (s.length() != 0) {
            StringTokenizer st = new StringTokenizer(s, ", ", false);
            y = UnitProcessor.svgVerticalCoordinateToUserSpace
                (st.nextToken(), SVG_Y_ATTRIBUTE, uctx);
        }

        return new Point2D.Float(x, y);
    }

    /**
     * Builds using the specified BridgeContext and element, the
     * specified graphics node.
     *
     * @param ctx the bridge context to use
     * @param e the element that describes the graphics node to build
     * @param node the graphics node to build
     */
    public void buildGraphicsNode(BridgeContext ctx,
                                  Element e,
                                  GraphicsNode node) {
        e.normalize();
        AttributedString as = buildAttributedString(ctx, e);
        addGlyphPositionAttributes(as, e, ctx);
        if (ctx.isDynamic()) {
            layoutedText = new AttributedString(as.getIterator());
        }
        TextNode tn = (TextNode)node;
        tn.setAttributedCharacterIterator(as.getIterator());

        // now add the painting attributes, cannot do it before this because
        // some of the Paint objects need to know the bounds of the text
        // and this isn't know until the text node aci is set
        TextDecoration textDecoration = 
            getTextDecoration(e, tn, new TextDecoration(), ctx);
        addPaintAttributes(as, e, tn, textDecoration, ctx);
        tn.setAttributedCharacterIterator(as.getIterator());

        //
        // DO NOT CALL super, 'opacity' is handle during addPaintAttributes()
        //

        // 'filter'
        node.setFilter(CSSUtilities.convertFilter(e, node, ctx));
        // 'mask'
        node.setMask(CSSUtilities.convertMask(e, node, ctx));
        // 'clip-path'
        node.setClip(CSSUtilities.convertClipPath(e, node, ctx));
        // 'pointer-events'
        node.setPointerEventType(CSSUtilities.convertPointerEvents(e));

        if (ctx.isDynamic()) {
            initializeDynamicSupport(ctx, e, node);
        }
        // Handle children elements such as <title>
        SVGUtilities.bridgeChildren(ctx, e);
    }

    /**
     * Returns false as text is not a container.
     */
    public boolean isComposite() {
        return false;
    }

    // Listener implementation ----------------------------------------------

    /**
     * The DOM EventListener to receive 'DOMNodeRemoved' event.
     */
    protected DOMChildNodeRemovedEventListener childNodeRemovedEventListener = 
        new DOMChildNodeRemovedEventListener();

    /**
     * The DOM EventListener invoked when a node is removed.
     */
    protected class DOMChildNodeRemovedEventListener implements EventListener {

        /**
         * Handles 'DOMNodeRemoved' event type.
         */
        public void handleEvent(Event evt) {
            handleDOMChildNodeRemovedEvent((MutationEvent)evt);            
        }
    }

    /**
     * The DOM EventListener to receive 'DOMSubtreeModified' event.
     */
    protected DOMSubtreeModifiedEventListener subtreeModifiedEventListener = 
        new DOMSubtreeModifiedEventListener();

    /**
     * The DOM EventListener invoked when the subtree is modified.
     */
    protected class DOMSubtreeModifiedEventListener implements EventListener {

        /**
         * Handles 'DOMSubtreeModified' event type.
         */
        public void handleEvent(Event evt) {
            handleDOMSubtreeModifiedEvent((MutationEvent)evt);
        }
    }

    // BridgeUpdateHandler implementation -----------------------------------

    /**
     * This method insures that any modification to a text
     * element and its children is going to be reflected 
     * into the GVT tree.
     */
    protected void initializeDynamicSupport(BridgeContext ctx,
                                            Element e,
                                            GraphicsNode node) {
        super.initializeDynamicSupport(ctx,e,node);

        EventTarget evtTarget = (EventTarget)e;

        //to be notified when a child is removed from the 
        //<text> element.
        evtTarget.addEventListener
            ("DOMNodeRemoved", childNodeRemovedEventListener, true);
        ctx.storeEventListener
            (evtTarget, "DOMNodeRemoved", childNodeRemovedEventListener, true);
        
        //to be notified when the modification of the subtree
        //of the <text> element is done
        evtTarget.addEventListener
            ("DOMSubtreeModified", subtreeModifiedEventListener, false);
        ctx.storeEventListener
            (evtTarget, "DOMSubtreeModified", subtreeModifiedEventListener, false);

        // traverse the children to add context on 
        // <tspan>, <tref> and <textPath>
        Node child  = e.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addContextToChild(ctx,(Element)child);
            }
            child = child.getNextSibling();
        }
    }

    /**
     * Add to the element children of the node, a
     * <code>SVGContext</code> to support dynamic updated . This is
     * recurssive, the children of the nodes are also traversed to add
     * to the support elements their context
     *
     * @param ctx a <code>BridgeContext</code> value
     * @param e an <code>Element</code> value
     *
     * @see org.apache.batik.dom.svg.SVGContext
     * @see org.apache.batik.bridge.BridgeUpdateHandler
     */
    protected void addContextToChild(BridgeContext ctx,Element e) {
        if (e.getNamespaceURI().equals(SVG_NAMESPACE_URI)) {
            if (e.getLocalName().equals(SVG_TSPAN_TAG)) {
                ((SVGOMElement)e).setSVGContext
                    (new TspanBridge(ctx, this, e));
            } else if (e.getLocalName().equals(SVG_TEXT_PATH_TAG)) {
                ((SVGOMElement)e).setSVGContext
                    (new TextPathBridge(ctx, this, e));
            } else if (e.getLocalName().equals(SVG_TREF_TAG)) {
                ((SVGOMElement)e).setSVGContext
                    (new TRefBridge(ctx, this, e));
            }
        }

        Node child  = e.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addContextToChild(ctx, (Element)child);
            }        
            child = child.getNextSibling();
        }
    }

    /**
     * Invoked when an MutationEvent of type 'DOMNodeInserted' is fired.
     */
    public void handleDOMNodeInsertedEvent(MutationEvent evt) {
        Node childNode = (Node)evt.getTarget();
        
        //check the type of the node inserted before discard the layout
        //in the case of <title> or <desc> or <metadata>, the layout
        //is unchanged
        switch( childNode.getNodeType() ){
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                layoutedText = null;
                break;
            case Node.ELEMENT_NODE:
                if (childNode.getNamespaceURI().equals(SVG_NAMESPACE_URI)) {
                    String nodeName = childNode.getLocalName();
                    if (nodeName.equals(SVG_TSPAN_TAG) ||
                        nodeName.equals(SVG_ALT_GLYPH_TAG) ||
                        nodeName.equals(SVG_A_TAG) ||
                        nodeName.equals(SVG_TEXT_PATH_TAG) ||
                        nodeName.equals(SVG_TREF_TAG)) {
                        addContextToChild( ctx, (Element)childNode );
                        layoutedText = null;
                    }
                }
                break;
            default:
        }
        if (layoutedText == null) {
            computeLayoutedText();
        }
    }

    /**
     * Invoked when an MutationEvent of type 'DOMNodeInserted' is fired.
     */
    public void handleDOMNodeRemovedEvent(MutationEvent evt) {
        EventTarget evtTarget = (EventTarget)evt.getTarget();
        evtTarget.removeEventListener("DOMNodeRemoved",
                                      childNodeRemovedEventListener,
                                      true);
        evtTarget.removeEventListener("DOMSubtreeModified",
                                      subtreeModifiedEventListener,
                                      false);
        super.handleDOMNodeRemovedEvent(evt);
    }

    /**
     * Invoked when an MutationEvent of type 'DOMNodeRemoved' is fired.
     */
    public void handleDOMChildNodeRemovedEvent(MutationEvent evt) {
        Node childNode = (Node)evt.getTarget();
        
        //check the type of the node inserted before discard the layout
        //in the case of <title> or <desc> or <metadata>, the layout
        //is unchanged
        switch (childNode.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                //the parent has to be a displayed node
                if (isParentDisplayed( childNode)) {
                    layoutedText = null;
                }
                break;
            case Node.ELEMENT_NODE:
                if (childNode.getNamespaceURI().equals(SVG_NAMESPACE_URI)) {
                    String nodeName = childNode.getLocalName();
                    if (nodeName.equals(SVG_TSPAN_TAG) ||
                        nodeName.equals(SVG_ALT_GLYPH_TAG) ||
                        nodeName.equals(SVG_A_TAG) ||
                        nodeName.equals(SVG_TEXT_PATH_TAG) ||
                        nodeName.equals(SVG_TREF_TAG)) {
                        
                        layoutedText = null;
                    }
                }
                break;
            default:
        }
        //if the layoutedText was set to null,
        //then wait for DOMSubtreeChange to recompute it.
    }

    /**
     * Invoked when an MutationEvent of type 'DOMSubtree' is fired.
     */
    public void handleDOMSubtreeModifiedEvent(MutationEvent evt){
        //an operation occured onto the children of the
        //text element, check if the layout was discarded
        if (layoutedText == null) {
            computeLayoutedText();
        }
    }

    /**
     * Invoked when an MutationEvent of type 'DOMCharacterDataModified' 
     * is fired.
     */
    public void handleDOMCharacterDataModified(MutationEvent evt){
        Node childNode = (Node)evt.getTarget();
        //if the parent is displayed, then discard the layout.
        if (isParentDisplayed(childNode)) {
            layoutedText = null;
        }
    }

    /**
     * Indicate of the parent of a node is
     * a displayed element.
     * &lt;title&gt;, &lt;desc&gt; and &lt;metadata&gt;
     * are non displayable elements.
     *
     * @return true if the parent of the node is &lt;text&gt;, 
     *   &lt;tspan&gt;, &lt;tref&gt;, &lt;textPath&gt;, &lt;a&gt;,
     *   &lt;altGlyph&gt;
     */
    protected boolean isParentDisplayed(Node childNode) {
        Node parentNode = childNode.getParentNode();
        if (parentNode.getNodeType() == Node.ELEMENT_NODE) {
            if (parentNode.getNamespaceURI().equals(SVG_NAMESPACE_URI)) {
                String nodeName = parentNode.getLocalName();
                if (nodeName.equals(SVG_TEXT_TAG) ||
                    nodeName.equals(SVG_TSPAN_TAG) ||
                    nodeName.equals(SVG_ALT_GLYPH_TAG) ||
                    nodeName.equals(SVG_A_TAG) ||
                    nodeName.equals(SVG_TEXT_PATH_TAG) ||
                    nodeName.equals(SVG_TREF_TAG)) {
                
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Recompute the layout of the &lt;text&gt; node.
     *
     * Assign onto the TextNode pending to the element
     * the new recomputed AtrributedString. Also
     * update <code>layoutedText</code> with the new
     * value.
     */
    protected void computeLayoutedText() {
        AttributedString as = buildAttributedString(ctx, e);
        addGlyphPositionAttributes(as, e, ctx);
        layoutedText = new AttributedString(as.getIterator());
        TextNode tn = (TextNode)node;
        tn.setAttributedCharacterIterator(as.getIterator());
        TextDecoration textDecoration = 
            getTextDecoration(e, tn, new TextDecoration(), ctx);
        addPaintAttributes(as, e, tn, textDecoration, ctx);
        tn.setAttributedCharacterIterator(as.getIterator());
    }

    /**
     * This flag bit indicates if a new ACI has been created in
     * response to a CSSEngineEvent.
     * Avoid creating one ShapePainter per CSS property change
     */
    private boolean hasNewACI;

    /**
     * This is the element a CSS property has changed.
     */
    private Element cssProceedElement;

    /**
     * Invoked when an MutationEvent of type 'DOMAttrModified' is fired.
     */
    public void handleDOMAttrModifiedEvent(MutationEvent evt) {
        String attrName = evt.getAttrName();
        if (attrName.equals(SVG_X_ATTRIBUTE) ||
            attrName.equals(SVG_Y_ATTRIBUTE) ||
            attrName.equals(SVG_DX_ATTRIBUTE) ||
            attrName.equals(SVG_DY_ATTRIBUTE) ||
            attrName.equals(SVG_ROTATE_ATTRIBUTE) ){

            if ( attrName.equals(SVG_X_ATTRIBUTE) ||
                 attrName.equals(SVG_Y_ATTRIBUTE)){
                ((TextNode)node).setLocation(getLocation(ctx, e));
            }

            computeLayoutedText();
        }
        else{
            super.handleDOMAttrModifiedEvent(evt);
        }
    }

    /**
     * Invoked when CSS properties have changed on an element.
     *
     * @param evt the CSSEngine event that describes the update
     */
    public void handleCSSEngineEvent(CSSEngineEvent evt) {
        hasNewACI = false;
        int [] properties = evt.getProperties();
        // first try to find CSS properties that change the layout
        for (int i=0; i < properties.length; ++i) {
            switch(properties[i]) {
            case SVGCSSEngine.TEXT_ANCHOR_INDEX:
            case SVGCSSEngine.FONT_SIZE_INDEX:
            case SVGCSSEngine.FONT_WEIGHT_INDEX:
            case SVGCSSEngine.FONT_STYLE_INDEX:
            case SVGCSSEngine.FONT_STRETCH_INDEX:
            case SVGCSSEngine.FONT_FAMILY_INDEX:
            case SVGCSSEngine.BASELINE_SHIFT_INDEX:
            case SVGCSSEngine.UNICODE_BIDI_INDEX:
            case SVGCSSEngine.DIRECTION_INDEX:
            case SVGCSSEngine.WRITING_MODE_INDEX:
            case SVGCSSEngine.GLYPH_ORIENTATION_VERTICAL_INDEX:
            case SVGCSSEngine.GLYPH_ORIENTATION_HORIZONTAL_INDEX:
            case SVGCSSEngine.LETTER_SPACING_INDEX:
            case SVGCSSEngine.WORD_SPACING_INDEX:
            case SVGCSSEngine.KERNING_INDEX: {
                if (!hasNewACI) {
                    hasNewACI = true;
                    AttributedString as = buildAttributedString(ctx, e);
                    addGlyphPositionAttributes(as, e, ctx);
                    layoutedText = new AttributedString(as.getIterator());
                    TextNode tn = (TextNode)node;
                    tn.setAttributedCharacterIterator(as.getIterator());
                    TextDecoration textDecoration = 
                        getTextDecoration(e, tn, new TextDecoration(), ctx);
                    addPaintAttributes(as, e, tn, textDecoration, ctx);
                    tn.setAttributedCharacterIterator(as.getIterator());
                }
                break;
            }
            }
        }
        //optimize the calculation of
        //the painting attributes and decoration
        //by only recomputing the section for the element
        cssProceedElement = evt.getElement();
        // go for the other CSS properties
        super.handleCSSEngineEvent(evt);
        cssProceedElement = null;
    }

    /**
     * Invoked for each CSS property that has changed.
     */
    protected void handleCSSPropertyChanged(int property) {
        switch(property) {
        case SVGCSSEngine.FILL_INDEX:
        case SVGCSSEngine.FILL_OPACITY_INDEX:
        case SVGCSSEngine.STROKE_INDEX:
        case SVGCSSEngine.STROKE_OPACITY_INDEX:
        case SVGCSSEngine.STROKE_WIDTH_INDEX:
        case SVGCSSEngine.STROKE_LINECAP_INDEX:
        case SVGCSSEngine.STROKE_LINEJOIN_INDEX:
        case SVGCSSEngine.STROKE_MITERLIMIT_INDEX:
        case SVGCSSEngine.STROKE_DASHARRAY_INDEX:
        case SVGCSSEngine.STROKE_DASHOFFSET_INDEX:
        case SVGCSSEngine.TEXT_DECORATION_INDEX: {
            if (!hasNewACI) {
                hasNewACI = true;
                TextNode tn = (TextNode)node;
                AttributedString as;

                TextDecoration parentDecoration;

                if ( cssProceedElement == e ){
                    parentDecoration = new TextDecoration();
                    as = new AttributedString(layoutedText.getIterator());
                }
                else{
                    //if a child CSS property has changed, then
                    //retrieve the parent text decoration
                    //and only update the section of the AtrtibutedString of
                    //the child
                    parentDecoration = getParentTextDecoration
                        (tn.getAttributedCharacterIterator(), 
                         cssProceedElement);
                    as = new AttributedString
                        (tn.getAttributedCharacterIterator());
                }
                tn.setAttributedCharacterIterator(as.getIterator());
                TextDecoration textDecoration = getTextDecoration
                    (cssProceedElement, tn, parentDecoration, ctx);
                addPaintAttributes
                    (as, cssProceedElement, tn, textDecoration, ctx);
                tn.setAttributedCharacterIterator(as.getIterator());
            }
            break;
        }
        case SVGCSSEngine.TEXT_RENDERING_INDEX: {
            RenderingHints hints = node.getRenderingHints();
            hints = CSSUtilities.convertTextRendering(e, hints);
            if (hints != null) {
                node.setRenderingHints(hints);
            }
            break;
        }
        case SVGCSSEngine.COLOR_RENDERING_INDEX: {
            RenderingHints hints = node.getRenderingHints();
            hints = CSSUtilities.convertColorRendering(e, hints);
            if (hints != null) {
                node.setRenderingHints(hints);
            }
            break;
        } 
        default:
            super.handleCSSPropertyChanged(property);
        }
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------

    /**
     * Creates the attributed string which represents the given text
     * element children.
     *
     * @param ctx the bridge context to use
     * @param element the text element
     */
    protected AttributedString buildAttributedString(BridgeContext ctx,
                                                     Element element) {
        
        AttributedStringBuffer asb = new AttributedStringBuffer();
        fillAttributedStringBuffer(ctx, element, true, null, asb);
        return asb.toAttributedString();
    }

    /**
     * Fills the given AttributedStringBuffer.
     */
    protected void fillAttributedStringBuffer(BridgeContext ctx,
                                              Element element,
                                              boolean top,
                                              TextPath textPath,
                                              AttributedStringBuffer asb) {
        // 'requiredFeatures', 'requiredExtensions' and 'systemLanguage'
        if (!SVGUtilities.matchUserAgent(element, ctx.getUserAgent())) {
            return;
        }
        
        String s = XMLSupport.getXMLSpace(element);
        boolean preserve = s.equals(SVG_PRESERVE_VALUE);
        boolean first = true;
        boolean last;
        boolean stripFirst = !preserve;
        boolean stripLast = !preserve;
        Element nodeElement = element;
        Map map = null;

        for (Node n = element.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            
            last = n.getNextSibling() == null;
            
            int lastChar = asb.getLastChar();
            stripFirst = !preserve && first &&
                (top || lastChar == ' ' || lastChar == -1);
            
            switch (n.getNodeType()) {
            case Node.ELEMENT_NODE:
                if (n.getNamespaceURI() != SVG_NAMESPACE_URI) {
                    break;
                }
                
                nodeElement = (Element)n;
                String ln = n.getLocalName();
                
                if (ln.equals(SVG_TSPAN_TAG) ||
                    ln.equals(SVG_ALT_GLYPH_TAG)) {
                    fillAttributedStringBuffer(ctx,
                                               nodeElement,
                                               false,
                                               textPath,
                                               asb);
                } else if (ln.equals(SVG_TEXT_PATH_TAG)) {
                    SVGTextPathElementBridge textPathBridge
                        = (SVGTextPathElementBridge)ctx.getBridge(nodeElement);
                    TextPath newTextPath
                        = textPathBridge.createTextPath(ctx, nodeElement);
                    if (newTextPath != null) {
                        fillAttributedStringBuffer(ctx,
                                                   nodeElement,
                                                   false,
                                                   newTextPath,
                                                   asb);
                    }
                } else if (ln.equals(SVG_TREF_TAG)) {
                    String uriStr = XLinkSupport.getXLinkHref((Element)n);
                    Element ref = ctx.getReferencedElement((Element)n, uriStr);
                    s = TextUtilities.getElementContent(ref);
                    s = normalizeString(s, preserve, stripFirst, last && top);
                    if (s != null) {
                        stripLast = !preserve && s.charAt(0) == ' ';
                        if (stripLast && !asb.isEmpty()) {
                            asb.stripLast();
                        }
                        Map m = getAttributeMap(ctx, nodeElement, textPath);
                        asb.append(s, m);
                    }
                } else if (ln.equals(SVG_A_TAG)) {
                    EventTarget target = (EventTarget)nodeElement;
                    UserAgent ua = ctx.getUserAgent();
                    EventListener l = new SVGAElementBridge.AnchorListener(ua);
                    target.addEventListener(SVG_EVENT_CLICK, l, false);
                    ctx.storeEventListener(target, SVG_EVENT_CLICK, l, false);
                    
                    fillAttributedStringBuffer(ctx,
                                               nodeElement,
                                               false,
                                               textPath,
                                               asb);
                }
                break;
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                s = n.getNodeValue();
                s = normalizeString(s, preserve, stripFirst, last && top);
                if (s != null) {
                    stripLast = !preserve && s.charAt(0) == ' ';
                    if (stripLast && !asb.isEmpty()) {
                        asb.stripLast();
                    }
                    if (map == null) {
                        map = getAttributeMap(ctx, element, textPath);
                    }
                    asb.append(s, map);
                }
            }
            first = false;
        }
    }

    /**
     * Normalizes the given string.
     */
    protected String normalizeString(String s,
                                     boolean preserve,
                                     boolean stripfirst,
                                     boolean striplast) {
        StringBuffer sb = new StringBuffer();
        if (preserve) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                case 10:
                case 13:
                case '\t':
                    sb.append(' ');
                    break;
                default:
                    sb.append(c);
                }
            }
        } else {
            boolean space = false;
            int idx = 0;
            if (stripfirst) {
                loop: while (idx < s.length()) {
                    switch (s.charAt(idx)) {
                    default:
                        break loop;
                    case 10:
                    case 13:
                    case ' ':
                    case '\t':
                        idx++;
                    }
                }
            }
            for (int i = idx; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                case 10:
                case 13:
                    break;
                case ' ':
                case '\t':
                    if (!space) {
                        sb.append(' ');
                        space = true;
                    }
                    break;
                default:
                    sb.append(c);
                    space = false;
                }
            }
            if (striplast) {
                int len;
                while ((len = sb.length()) > 0) {
                    if (sb.charAt(len - 1) == ' ') {
                        sb.deleteCharAt(len - 1);
                    } else {
                        break;
                    }
                }
            }
        }
        if (sb.length() > 0) {
            return sb.toString();
        } else if (stripfirst && striplast) {
            return " ";
        }
        return null;
    }

    /**
     * This class is used to build an AttributedString.
     */
    protected static class AttributedStringBuffer {

        /**
         * The strings.
         */
        protected List strings;

        /**
         * The attributes.
         */
        protected List attributes;
        
        /**
         * The number of items.
         */
        protected int count;

        /**
         * The length of the attributed string.
         */
        protected int length;
        
        /**
         * Creates a new empty AttributedStringBuffer.
         */
        public AttributedStringBuffer() {
            strings    = new ArrayList();
            attributes = new ArrayList();
            count      = 0;
            length     = 0;
        }

        /**
         * Tells whether this AttributedStringBuffer is empty.
         */
        public boolean isEmpty() {
            return count == 0;
        }
        
        /**
         * Returns the length in chars of the current Attributed String
         */
        public int length() {
            return length;
        }

        /**
         * Appends a String and its associated attributes.
         */
        public void append(String s, Map m) {
          if (s.length() == 0) return;
          strings.add(s);
          attributes.add(m);
          count++;
          length += s.length();
        }

        /**
         * Returns the value of the last char or -1.
         */
        public int getLastChar() {
            if (count == 0) {
                return -1;
            }
            String s = (String)strings.get(count - 1);
            return s.charAt(s.length() - 1);
        }

        /**
         * Strips the last string last character.
         */
        public void stripLast() {
            String s = (String)strings.remove(count - 1);
            if (s.charAt(s.length() - 1) == ' ') {
                if (s.length() == 1) {
                    attributes.remove(--count);
                    return;
                }
                strings.add(s.substring(0, s.length() - 1));
                length--;
            } else {
                strings.add(s);
            }
        }

        /**
         * Builds an attributed string from the content of this
         * buffer.
         */
        public AttributedString toAttributedString() {
            switch (count) {
            case 0:
                return new AttributedString(" ");
            case 1:
                return new AttributedString((String)strings.get(0),
                                            (Map)attributes.get(0));
            }

            StringBuffer sb = new StringBuffer();
            Iterator it = strings.iterator();
            while (it.hasNext()) {
                sb.append((String)it.next());
            }

            AttributedString result = new AttributedString(sb.toString());

            // Set the attributes

            Iterator sit = strings.iterator();
            Iterator ait = attributes.iterator();
            int idx = 0;
            while (sit.hasNext()) {
                String s = (String)sit.next();
                int nidx = idx + s.length();
                Map m = (Map)ait.next();
                Iterator kit = m.keySet().iterator();
                Iterator vit = m.values().iterator();
                while (kit.hasNext()) {
                    Attribute attr = (Attribute)kit.next();
                    Object val = vit.next();
                    result.addAttribute(attr, val, idx, nidx);
                }
                idx = nidx;
            }

            return result;
        }

        public String toString() {
            switch (count) {
            case 0:
                return "";
            case 1:
                return (String)strings.get(0);
            }

            StringBuffer sb = new StringBuffer();
            Iterator it = strings.iterator();
            while (it.hasNext()) {
                sb.append((String)it.next());
            }
            return sb.toString();
        }
    }

    /**
     * Returns true if node1 is an ancestor of node2
     */
    protected boolean nodeAncestorOf(Node node1, Node node2) {
        if (node2 == null || node1 == null) {
            return false;
        }
        Node parent = node2.getParentNode();
        while (parent != null && parent != node1) {
            parent = parent.getParentNode();
        }
        return (parent == node1);
    }


    /**
     * Adds glyph position attributes to an AttributedString.
     */
    protected void addGlyphPositionAttributes(AttributedString as,
                                              Element element,
                                              BridgeContext ctx) {

        // 'requiredFeatures', 'requiredExtensions' and 'systemLanguage'
        if (!SVGUtilities.matchUserAgent(element, ctx.getUserAgent())) {
            return;
        }
        AttributedCharacterIterator aci = as.getIterator();

        // calculate which chars in the string belong to this element
        int firstChar = -1;
        for (int i = 0; i < aci.getEndIndex(); i++) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
            GVTAttributedCharacterIterator.
            TextAttribute.TEXT_COMPOUND_DELIMITER);
            if (delimeter == element || nodeAncestorOf(element, delimeter)) {
                firstChar = i;
                break;
            }
        }
        // No match so no chars to annotate.
        if (firstChar == -1) return;

        int lastChar = aci.getEndIndex()-1;
        for (int i = aci.getEndIndex()-1; i >= 0; i--) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
                GVTAttributedCharacterIterator.
                TextAttribute.TEXT_COMPOUND_DELIMITER);
            if (delimeter == element || nodeAncestorOf(element, delimeter)) {
                lastChar = i;
                break;
            }
        }

        // get all of the glyph position attribute values
        String xAtt = element.getAttributeNS(null, SVG_X_ATTRIBUTE);
        String yAtt = element.getAttributeNS(null, SVG_Y_ATTRIBUTE);
        String dxAtt = element.getAttributeNS(null, SVG_DX_ATTRIBUTE);
        String dyAtt = element.getAttributeNS(null, SVG_DY_ATTRIBUTE);
        String rotateAtt = element.getAttributeNS(null, SVG_ROTATE_ATTRIBUTE);

        UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, element);

        ArrayList al;
        int len;

        // process the x attribute
        if (xAtt.length() != 0) {
            al = TextUtilities.svgHorizontalCoordinateArrayToUserSpace
                (element, SVG_X_ATTRIBUTE, xAtt, ctx);
            len = al.size();

            for (int i = 0; i < len; i++) {
                if (firstChar + i <= lastChar) {
                    as.addAttribute
                        (GVTAttributedCharacterIterator.TextAttribute.X,
                         al.get(i), firstChar+i, firstChar+i+1);
                }
            }
        }

       // process the y attribute
        if (yAtt.length() != 0) {
            al = TextUtilities.svgVerticalCoordinateArrayToUserSpace
                (element, SVG_Y_ATTRIBUTE, yAtt, ctx);
            len = al.size();

            for (int i = 0; i < len; i++) {
                if (firstChar+i <= lastChar) {
                    as.addAttribute
                        (GVTAttributedCharacterIterator.TextAttribute.Y,
                         al.get(i), firstChar+i, firstChar+i+1);
                }
            }
        }

        // process dx attribute
        if (dxAtt.length() != 0) {
            al = TextUtilities.svgHorizontalCoordinateArrayToUserSpace
                (element, SVG_DX_ATTRIBUTE, dxAtt, ctx);
            len = al.size();

            for (int i = 0; i < len; i++) {
                if (firstChar+i <= lastChar) {
                    as.addAttribute
                        (GVTAttributedCharacterIterator.TextAttribute.DX,
                         al.get(i), firstChar+i, firstChar+i+1);
                }
            }
        }

        // process dy attribute
        if (dyAtt.length() != 0) {
            al = TextUtilities.svgVerticalCoordinateArrayToUserSpace
                (element, SVG_DY_ATTRIBUTE, dyAtt, ctx);
            len = al.size();

            for (int i = 0; i < len; i++) {
                if (firstChar+i <= lastChar) {
                    as.addAttribute
                        (GVTAttributedCharacterIterator.TextAttribute.DY,
                         al.get(i), firstChar+i, firstChar+i+1);
                }
            }
        }

        // process rotate attribute
        if (rotateAtt.length() != 0) {
            al = TextUtilities.svgRotateArrayToFloats
                (element, SVG_ROTATE_ATTRIBUTE, rotateAtt, ctx);
            len = al.size();

            if (len == 1) {  // not a list
                // each char will have the same rotate value
                as.addAttribute
                    (GVTAttributedCharacterIterator.TextAttribute.ROTATION,
                     al.get(0), firstChar, lastChar + 1);

            } else {  // its a list
                // set each rotate value from the list
                for (int i = 0; i < len; i++) {
                    if (firstChar+i <= lastChar) {
                        as.addAttribute
                            (GVTAttributedCharacterIterator.
                             TextAttribute.ROTATION,
                             al.get(i), firstChar+i, firstChar+i+1);
                    }
                }
            }
        }

        addChildGlyphPositionAttributes(as, element, ctx);
    }

    protected void addChildGlyphPositionAttributes(AttributedString as,
                                                   Element element,
                                                   BridgeContext ctx) {
        // do the same for each child element
        for (Node child = element.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!SVG_NAMESPACE_URI.equals(child.getNamespaceURI())) {
                continue;
            }
            String ln = child.getLocalName();
            if (ln.equals(SVG_TSPAN_TAG) ||
                ln.equals(SVG_ALT_GLYPH_TAG) ||
                ln.equals(SVG_A_TAG) ||
                ln.equals(SVG_TEXT_PATH_TAG) ||
                ln.equals(SVG_TREF_TAG)) {
                addGlyphPositionAttributes(as, (Element)child, ctx);
            }
        }
    }

    /**
     * Adds painting attributes to an AttributedString.
     */
    protected void addPaintAttributes(AttributedString as,
                                      Element element,
                                      TextNode node,
                                      TextDecoration textDecoration,
                                      BridgeContext ctx) {


        // 'requiredFeatures', 'requiredExtensions' and 'systemLanguage'
        if (!SVGUtilities.matchUserAgent(element, ctx.getUserAgent())) {
            return;
        }
        AttributedCharacterIterator aci = as.getIterator();

        // calculate which chars in the string belong to this element
        int firstChar = -1;
        for (int i = 0; i < aci.getEndIndex(); i++) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
            GVTAttributedCharacterIterator.
            TextAttribute.TEXT_COMPOUND_DELIMITER);
            if (delimeter == element || nodeAncestorOf(element, delimeter)) {
                firstChar = i;
                break;
            }
        }
        if (firstChar == -1)
            return; // Element not part of aci (no chars in elem usually)

        int lastChar = aci.getEndIndex()-1;
        for (int i = aci.getEndIndex()-1; i >= 0; i--) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
                GVTAttributedCharacterIterator.
                TextAttribute.TEXT_COMPOUND_DELIMITER);
            if (delimeter == element || nodeAncestorOf(element, delimeter)) {
                lastChar = i;
                break;
            }
        }

        // Opacity
        Composite composite = CSSUtilities.convertOpacity(element);
        as.addAttribute(GVTAttributedCharacterIterator.TextAttribute.OPACITY,
                        composite, firstChar, lastChar+1);

        // Fill
        Paint p = PaintServer.convertFillPaint(element, node, ctx);
        // System.out.println("Fore: " + p + " [" + firstChar + "," +
        //                    (lastChar+1) + "]");
        as.addAttribute(TextAttribute.FOREGROUND, p,
                        firstChar, lastChar+1);

        // Stroke Paint
        Paint sp = PaintServer.convertStrokePaint(element, node, ctx);
        as.addAttribute
            (GVTAttributedCharacterIterator.TextAttribute.STROKE_PAINT,
             sp, firstChar, lastChar+1);

        // Stroke
        Stroke stroke = PaintServer.convertStroke(element);
        as.addAttribute
            (GVTAttributedCharacterIterator.TextAttribute.STROKE,
             stroke, firstChar, lastChar+1);

        // Text decoration
        if (textDecoration != null) {
            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.UNDERLINE_PAINT,
                            textDecoration.underlinePaint,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.UNDERLINE_STROKE_PAINT,
                            textDecoration.underlineStrokePaint,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.UNDERLINE_STROKE,
                            textDecoration.underlineStroke,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.OVERLINE_PAINT,
                            textDecoration.overlinePaint,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.OVERLINE_STROKE_PAINT,
                            textDecoration.overlineStrokePaint,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.OVERLINE_STROKE,
                            textDecoration.overlineStroke,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.STRIKETHROUGH_PAINT,
                            textDecoration.strikethroughPaint,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.STRIKETHROUGH_STROKE_PAINT,
                            textDecoration.strikethroughStrokePaint,
                            firstChar, lastChar+1);

            as.addAttribute(GVTAttributedCharacterIterator.
                            TextAttribute.STRIKETHROUGH_STROKE,
                            textDecoration.strikethroughStroke,
                            firstChar, lastChar+1);
        }

        addChildPaintAttributes(as, element, node, textDecoration, ctx);
    }

    protected void addChildPaintAttributes(AttributedString as,
                                           Element element,
                                           TextNode node,
                                           TextDecoration textDecoration,
                                           BridgeContext ctx) {
        // Add Paint attributres for children of text element
        for (Node child = element.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!SVG_NAMESPACE_URI.equals(child.getNamespaceURI())) {
                continue;
            }
            String ln = child.getLocalName();
            if (ln.equals(SVG_TSPAN_TAG) ||
                ln.equals(SVG_ALT_GLYPH_TAG) ||
                ln.equals(SVG_A_TAG) ||
                ln.equals(SVG_TEXT_PATH_TAG) ||
                ln.equals(SVG_TREF_TAG)) {
                Element childElement = (Element)child;
                TextDecoration td = getTextDecoration(childElement, node,
                                                      textDecoration, ctx);
                addPaintAttributes(as, childElement, node, td, ctx);
            }
        }
    }

    /**
     * Returns the map to pass to the current characters.
     */
    protected Map getAttributeMap(BridgeContext ctx,
                                  Element element,
                                  TextPath textPath) {
        UnitProcessor.Context uctx = UnitProcessor.createContext(ctx, element);

        Map result = new HashMap();
        String s;
        float f;
        short t;
        boolean verticalText = false;

        result.put
        (GVTAttributedCharacterIterator.TextAttribute.TEXT_COMPOUND_DELIMITER,
         element);

        if (element.getTagName().equals(SVG_ALT_GLYPH_TAG)) {
            result.put
              (GVTAttributedCharacterIterator.TextAttribute.ALT_GLYPH_HANDLER,
               new SVGAltGlyphHandler(ctx, element));
        }

        if (textPath != null) {
            result.put(GVTAttributedCharacterIterator.TextAttribute.TEXTPATH,
                       textPath);
        }

        // Text-anchor
        TextNode.Anchor a = TextUtilities.convertTextAnchor(element);
        result.put(GVTAttributedCharacterIterator.TextAttribute.ANCHOR_TYPE,
                   a);

        // Font size.
        Float fs = TextUtilities.convertFontSize(element);
        result.put(TextAttribute.SIZE, fs);

        // Font weight
        Float fw = TextUtilities.convertFontWeight(element);
        Value v = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.FONT_WEIGHT_INDEX);
        String fontWeightString = v.getCssText();
        result.put(TextAttribute.WEIGHT, fw);

        // Font style
        String fontStyleString = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.FONT_STYLE_INDEX).getStringValue();
        result.put(TextAttribute.POSTURE,
                   TextUtilities.convertFontStyle(element));

        // Font stretch
        String fontStretchString = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.FONT_STRETCH_INDEX).getStringValue();
        result.put(TextAttribute.WIDTH,
                   TextUtilities.convertFontStretch(element));

        // Font family
        Value val = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.FONT_FAMILY_INDEX);

        //  make a list of GVTFontFamily objects
        List fontFamilyList = new Vector();
        int len = val.getLength();
        for (int i = 0; i < len; i++) {
            Value it = val.item(i);
            String fontFamilyName = it.getStringValue();
            GVTFontFamily fontFamily
                = SVGFontUtilities.getFontFamily(element, ctx, fontFamilyName,
                   fontWeightString, fontStyleString);
            fontFamilyList.add(fontFamily);
        }

        result.put
            (GVTAttributedCharacterIterator.TextAttribute.GVT_FONT_FAMILIES,
             fontFamilyList);

        // Text baseline adjustment.
        Object bs = TextUtilities.convertBaselineShift(element);
        if (bs != null) {
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.BASELINE_SHIFT, bs);
        }

        // Unicode-bidi mode
        val =  CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.UNICODE_BIDI_INDEX);
        s = val.getStringValue();
        if (s.charAt(0) == 'n') {
            result.put(TextAttribute.BIDI_EMBEDDING, ZERO);
        } else {

            // Text direction
            // XXX: this needs to coordinate with the unicode-bidi
            // property, so that when an explicit reversal
            // occurs, the BIDI_EMBEDDING level is
            // appropriately incremented or decremented.
            // Note that direction is implicitly handled by unicode
            // BiDi algorithm in most cases, this property
            // is only needed when one wants to override the
            // normal writing direction for a string/substring.

            val = CSSUtilities.getComputedStyle
                (element, SVGCSSEngine.DIRECTION_INDEX);
            String rs = val.getStringValue();
            switch (rs.charAt(0)) {
            case 'l':
                result.put(TextAttribute.RUN_DIRECTION,
                           TextAttribute.RUN_DIRECTION_LTR);

                switch (s.charAt(0)) {
                case 'b': // bidi-override
                    result.put(TextAttribute.BIDI_EMBEDDING,
                               new Integer(-2));
                    break;
                case 'e': // embed
                    result.put(TextAttribute.BIDI_EMBEDDING,
                               new Integer(2));
                    break;
                }

                break;
            case 'r':
                result.put(TextAttribute.RUN_DIRECTION,
                           TextAttribute.RUN_DIRECTION_RTL);
                switch (s.charAt(0)) {
                case 'b': // bidi-override
                    result.put(TextAttribute.BIDI_EMBEDDING,
                               new Integer(-1));
                    break;
                case 'e': // embed
                    result.put(TextAttribute.BIDI_EMBEDDING,
                               new Integer(1));
                    break;
                }
                break;
            }
        }

        // Writing mode

        val = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.WRITING_MODE_INDEX);
        s = val.getStringValue();
        switch (s.charAt(0)) {
        case 'l':
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.WRITING_MODE,
                       GVTAttributedCharacterIterator.
                       TextAttribute.WRITING_MODE_LTR);
            break;
        case 'r':
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.WRITING_MODE,
                       GVTAttributedCharacterIterator.
                       TextAttribute.WRITING_MODE_RTL);
            break;
        case 't':
                result.put(GVTAttributedCharacterIterator.
                       TextAttribute.WRITING_MODE,
                       GVTAttributedCharacterIterator.
                       TextAttribute.WRITING_MODE_TTB);
            break;
        }

        // glyph-orientation-vertical

        val = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.GLYPH_ORIENTATION_VERTICAL_INDEX);
        switch (val.getPrimitiveType()) {
        case CSSPrimitiveValue.CSS_IDENT: // auto
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION,
                       GVTAttributedCharacterIterator.
                       TextAttribute.ORIENTATION_AUTO);
            break;
        case CSSPrimitiveValue.CSS_DEG:
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION,
                       GVTAttributedCharacterIterator.
                       TextAttribute.ORIENTATION_ANGLE);
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION_ANGLE,
                       new Float(val.getFloatValue()));
            break;
        case CSSPrimitiveValue.CSS_RAD:
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION,
                       GVTAttributedCharacterIterator.
                       TextAttribute.ORIENTATION_ANGLE);
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION_ANGLE,
                       new Float(val.getFloatValue() * 180 / Math.PI));
            break;
        case CSSPrimitiveValue.CSS_GRAD:
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION,
                       GVTAttributedCharacterIterator.
                       TextAttribute.ORIENTATION_ANGLE);
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.VERTICAL_ORIENTATION_ANGLE,
                       new Float(val.getFloatValue() * 9 / 5));
            break;
        default:
            // Cannot happen
            throw new InternalError();
        }

        // glyph-orientation-horizontal

        val = CSSUtilities.getComputedStyle
            (element, SVGCSSEngine.GLYPH_ORIENTATION_HORIZONTAL_INDEX);
        switch (val.getPrimitiveType()) {
        case CSSPrimitiveValue.CSS_DEG:
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.HORIZONTAL_ORIENTATION_ANGLE,
                       new Float(val.getFloatValue()));
            break;
        case CSSPrimitiveValue.CSS_RAD:
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.HORIZONTAL_ORIENTATION_ANGLE,
                       new Float(val.getFloatValue() * 180 / Math.PI));
            break;
        case CSSPrimitiveValue.CSS_GRAD:
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.HORIZONTAL_ORIENTATION_ANGLE,
                       new Float(val.getFloatValue() * 9 / 5));
            break;
        default:
            // Cannot happen
            throw new InternalError();
        }

        // text spacing properties...

        // Letter Spacing
        Float sp = TextUtilities.convertLetterSpacing(element);
        if (sp != null) {
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.LETTER_SPACING,
                       sp);
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.CUSTOM_SPACING,
                       Boolean.TRUE);
        }

        // Word spacing
        sp = TextUtilities.convertWordSpacing(element);
        if (sp != null) {
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.WORD_SPACING,
                       sp);
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.CUSTOM_SPACING,
                       Boolean.TRUE);
        }

        // Kerning
        sp = TextUtilities.convertKerning(element);
        if (sp != null) {
            result.put(GVTAttributedCharacterIterator.TextAttribute.KERNING,
                       sp);
            result.put(GVTAttributedCharacterIterator.
                       TextAttribute.CUSTOM_SPACING,
                       Boolean.TRUE);
        }

        // textLength
        s = element.getAttributeNS(null, SVG_TEXT_LENGTH_ATTRIBUTE);
        if (s.length() != 0) {
            f = UnitProcessor.svgOtherLengthToUserSpace
                (s, SVG_TEXT_LENGTH_ATTRIBUTE, uctx);
            result.put(GVTAttributedCharacterIterator.TextAttribute.BBOX_WIDTH,
                       new Float(f));

            // lengthAdjust
            s = element.getAttributeNS(null, SVG_LENGTH_ADJUST_ATTRIBUTE);

            if (s.length() < 10) {
                result.put(GVTAttributedCharacterIterator.
                           TextAttribute.LENGTH_ADJUST,
                           GVTAttributedCharacterIterator.
                           TextAttribute.ADJUST_SPACING);
                result.put(GVTAttributedCharacterIterator.
                           TextAttribute.CUSTOM_SPACING,
                           Boolean.TRUE);
            } else {
                result.put(GVTAttributedCharacterIterator.
                           TextAttribute.LENGTH_ADJUST,
                           GVTAttributedCharacterIterator.
                           TextAttribute.ADJUST_ALL);
            }
        }

        return result;
    }


    /**
     * Retrieve in the AttributeString the closest parent
     * of the node 'child' and extract the text decorations
     * of the parent.
     *
     * @param aci an <code>AttributedCharacterIterator</code> value
     * @param child an <code>Element</code> value
     * @return a <code>TextDecoration</code> value
     */
    protected TextDecoration getParentTextDecoration
        (AttributedCharacterIterator aci, 
         Element child){

        Element parent = null;

        // calculate which chars in the string belong to the parent
        int firstChar = -1;
        for (int i = 0; i < aci.getEndIndex(); i++) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
            GVTAttributedCharacterIterator.
            TextAttribute.TEXT_COMPOUND_DELIMITER);
            if ( nodeAncestorOf(delimeter,child) ){
                parent = delimeter;
                firstChar = i;
            }
            if (delimeter == child || nodeAncestorOf(child, delimeter)) {
                break;
            }
        }

        TextDecoration textDecoration = new TextDecoration();

        if ( parent == null){
            //no parent
            return textDecoration;
        }

        aci.setIndex(firstChar);

        textDecoration.underlinePaint = 
            (Paint)aci.getAttribute(GVTAttributedCharacterIterator.
                                    TextAttribute.UNDERLINE_PAINT);

        textDecoration.underlineStrokePaint = 
            (Paint)aci.getAttribute(GVTAttributedCharacterIterator.
                                    TextAttribute.UNDERLINE_STROKE_PAINT);

        textDecoration.underlineStroke = 
            (Stroke)aci.getAttribute(GVTAttributedCharacterIterator.
                                     TextAttribute.UNDERLINE_STROKE);

        textDecoration.overlinePaint = 
            (Paint)aci.getAttribute(GVTAttributedCharacterIterator.
                                    TextAttribute.OVERLINE_PAINT);

        textDecoration.overlineStrokePaint = 
            (Paint)aci.getAttribute(GVTAttributedCharacterIterator.
                                    TextAttribute.OVERLINE_STROKE_PAINT);

        textDecoration.overlineStroke = 
            (Stroke)aci.getAttribute(GVTAttributedCharacterIterator.
                                     TextAttribute.OVERLINE_STROKE);

        textDecoration.strikethroughPaint = 
            (Paint)aci.getAttribute(GVTAttributedCharacterIterator.
                                    TextAttribute.STRIKETHROUGH_PAINT);

        textDecoration.strikethroughStrokePaint = 
            (Paint)aci.getAttribute(GVTAttributedCharacterIterator.
                                    TextAttribute.STRIKETHROUGH_STROKE_PAINT);

        textDecoration.strikethroughStroke = 
            (Stroke)aci.getAttribute(GVTAttributedCharacterIterator.
                                     TextAttribute.STRIKETHROUGH_STROKE);

        return textDecoration;
    }

    /**
     * Constructs a TextDecoration object for the specified element. This will
     * contain all of the decoration properties to be used when drawing the
     * text.
     */
    protected TextDecoration getTextDecoration(Element element,
                                               GraphicsNode node,
                                               TextDecoration parent,
                                               BridgeContext ctx) {
        int pidx = SVGCSSEngine.TEXT_DECORATION_INDEX;
        Value val = CSSUtilities.getComputedStyle(element, pidx);
        
        // Was text-decoration explicity set on this element?
        StyleMap sm = ((CSSStylableElement)element).getComputedStyleMap(null);
        if (sm.isNullCascaded(pidx)) {
            // If not, keep the same decorations.
            return parent;
        }

        TextDecoration textDecoration = new TextDecoration(parent);

        short t = val.getCssValueType();

        switch (val.getCssValueType()) {
        case CSSValue.CSS_VALUE_LIST:
            ListValue lst = (ListValue)val;

            Paint paint = PaintServer.convertFillPaint(element, node, ctx);
            Paint strokePaint = PaintServer.convertStrokePaint(element,
                                                               node, ctx);
            Stroke stroke = PaintServer.convertStroke(element);

            int len = lst.getLength();
            for (int i = 0; i < len; i++) {
                Value v = lst.item(i);
                String s = v.getStringValue();
                switch (s.charAt(0)) {
                case 'u':
                    if (paint != null) {
                       textDecoration.underlinePaint = paint;
                    }
                    if (strokePaint != null) {
                        textDecoration.underlineStrokePaint = strokePaint;
                    }
                    if (stroke != null) {
                        textDecoration.underlineStroke = stroke;
                    }
                    break;
                case 'o':
                    if (paint != null) {
                       textDecoration.overlinePaint = paint;
                    }
                    if (strokePaint != null) {
                        textDecoration.overlineStrokePaint = strokePaint;
                    }
                    if (stroke != null) {
                        textDecoration.overlineStroke = stroke;
                    }
                    break;
                case 'l':
                    if (paint != null) {
                       textDecoration.strikethroughPaint = paint;
                    }
                    if (strokePaint != null) {
                        textDecoration.strikethroughStrokePaint = strokePaint;
                    }
                    if (stroke != null) {
                        textDecoration.strikethroughStroke = stroke;
                    }
                    break;
                }
            }
            return textDecoration;
        default: // None
            return TextDecoration.EMPTY_TEXT_DECORATION;
        }
    }

    /**
     * To store the text decorations of a text element.
     */
    protected static class TextDecoration {

        static final TextDecoration EMPTY_TEXT_DECORATION =
            new TextDecoration();

        Paint underlinePaint;
        Paint underlineStrokePaint;
        Stroke underlineStroke;
        Paint overlinePaint;
        Paint overlineStrokePaint;
        Stroke overlineStroke;
        Paint strikethroughPaint;
        Paint strikethroughStrokePaint;
        Stroke strikethroughStroke;

        TextDecoration() {
            underlinePaint = null;
            underlineStrokePaint = null;
            underlineStroke = null;
            overlinePaint = null;
            overlineStrokePaint = null;
            overlineStroke = null;
            strikethroughPaint = null;
            strikethroughStrokePaint = null;
            strikethroughStroke = null;
        }

        TextDecoration(TextDecoration td) {
            underlinePaint = td.underlinePaint;
            underlineStrokePaint = td.underlineStrokePaint;
            underlineStroke = td.underlineStroke;
            overlinePaint = td.overlinePaint;
            overlineStrokePaint = td.overlineStrokePaint;
            overlineStroke = td.overlineStroke;
            strikethroughPaint = td.strikethroughPaint;
            strikethroughStrokePaint = td.strikethroughStrokePaint;
            strikethroughStroke = td.strikethroughStroke;
        }
    }

    /**
     * Implementation of <ode>SVGContext</code> for
     * the children of &lt;text&gt;
     */
    protected abstract class AbstractTextChildSVGContext 
        implements SVGContext {

        /** Bridge Context */
        protected BridgeContext ctx;

        /** Text bridge parent */
        protected SVGTextElementBridge textBridge;

        /** Element */
        protected Element e;

        /**
         * Initialize the <code>SVGContext</code> implementation
         * with the bridgeContext, the parent bridge, and the
         * element supervised by this context
         */
        public AbstractTextChildSVGContext(BridgeContext ctx,
                                           SVGTextElementBridge parent,
                                           Element e) {
            this.ctx = ctx;
            this.textBridge = parent;
            this.e = e;
        }
        
        /**
         * Returns the size of a px CSS unit in millimeters.
         */
        public float getPixelUnitToMillimeter() {
            return ctx.getUserAgent().getPixelUnitToMillimeter();
        }

        /**
         * Returns the size of a px CSS unit in millimeters.
         * This will be removed after next release.
         * @see #getPixelUnitToMillimeter();
         */
        public float getPixelToMM() {
            return getPixelUnitToMillimeter();
            
        }
        /**
         * Returns the tight bounding box in current user space (i.e.,
         * after application of the transform attribute, if any) on the
         * geometry of all contained graphics elements, exclusive of
         * stroke-width and filter effects).
         */
        public Rectangle2D getBBox() {
            //text children does not support getBBox
            //return textBridge.getBBox();
            return null;
        }

        /**
         * Returns the transformation matrix from current user units
         * (i.e., after application of the transform attribute, if any) to
         * the viewport coordinate system for the nearestViewportElement.
         */
        public AffineTransform getCTM() {
            // text children does not support transform attribute
            //return textBridge.getCTM();
            return null;
        }

        /**
         * Returns the global transformation matrix from the current
         * element to the root.
         */
        public AffineTransform getGlobalTransform() {
            //return node.getGlobalTransform();
            return null;
        }

        /**
         * Returns the width of the viewport which directly contains the
         * given element.
         */
        public float getViewportWidth() {
            return ctx.getBlockWidth(e);
        }
        
        /**
         * Returns the height of the viewport which directly contains the
         * given element.
         */
        public float getViewportHeight() {
            return ctx.getBlockHeight(e);
        }
        
        /**
         * Returns the font-size on the associated element.
         */
        public float getFontSize() {
            return CSSUtilities.getComputedStyle
                (e, SVGCSSEngine.FONT_SIZE_INDEX).getFloatValue();
        }
    }

    /**
     * Implementation for the <code>BridgeUpdateHandler</code>
     * for the child elements of &lt;text&gt;.
     * This implementation relies on the parent bridge
     * which contains the <code>TextNode</code>
     * representing the node this context supervised.
     * All operations are done by the parent bridge
     * <code>SVGTextElementBridge</code> which can determine
     * the impact of a change of one of its children for the others.
     */
    protected abstract class AbstractTextChildBridgeUpdateHandler 
        extends AbstractTextChildSVGContext implements BridgeUpdateHandler {

        /**
         * Initialize the BridgeUpdateHandler implementation.
         */
        public AbstractTextChildBridgeUpdateHandler
            (BridgeContext ctx,
             SVGTextElementBridge parent,
             Element e) {

            super(ctx,parent,e);
        }
        /**
         * Invoked when an MutationEvent of type 'DOMAttrModified' is fired.
         */
        public void handleDOMAttrModifiedEvent(MutationEvent evt) {
            //nothing to do
        }

        /**
         * Invoked when an MutationEvent of type 'DOMNodeInserted' is fired.
         */
        public void handleDOMNodeInsertedEvent(MutationEvent evt) {
            textBridge.handleDOMNodeInsertedEvent(evt);
        }

        /**
         * Invoked when an MutationEvent of type 'DOMNodeRemoved' is fired.
         */
        public void handleDOMNodeRemovedEvent(MutationEvent evt) {
            //nothing to do
            dispose();
        }

        /**
         * Invoked when an MutationEvent of type 'DOMCharacterDataModified' 
         * is fired.
         */
        public void handleDOMCharacterDataModified(MutationEvent evt) {
            textBridge.handleDOMCharacterDataModified(evt);
        }

        /**
         * Invoked when an CSSEngineEvent is fired.
         */
        public void handleCSSEngineEvent(CSSEngineEvent evt) {
            textBridge.handleCSSEngineEvent(evt);
        }

        /**
         * Disposes this BridgeUpdateHandler and releases all resources.
         */
        public void dispose(){
            ((SVGOMElement)e).setSVGContext(null);
        }
    }

    protected class AbstractTextChildTextContent 
        extends AbstractTextChildBridgeUpdateHandler
        implements SVGTextContent{

        /**
         * Initialize the AbstractTextChildBridgeUpdateHandler implementation.
         */
        public AbstractTextChildTextContent
            (BridgeContext ctx,
             SVGTextElementBridge parent,
             Element e) {

            super(ctx,parent,e);
        }

        //Implementation of TextContent

        public int getNumberOfChars(){
            return textBridge.getNumberOfChars(e);
        }
        
        public Rectangle2D getExtentOfChar(int charnum ){
            return textBridge.getExtentOfChar(e,charnum);
        }

        public Point2D getStartPositionOfChar(int charnum){
            return textBridge.getStartPositionOfChar(e,charnum);
        }

        public Point2D getEndPositionOfChar(int charnum){
            return textBridge.getEndPositionOfChar(e,charnum);
        }

        public void selectSubString(int charnum, int nchars){
            textBridge.selectSubString(e,charnum,nchars);
        }

        public float getRotationOfChar(int charnum){
            return textBridge.getRotationOfChar(e,charnum);
        }

        public float getComputedTextLength(){
            return textBridge.getComputedTextLength(e);
        }
        
        public float getSubStringLength(int charnum, int nchars){
            return textBridge.getSubStringLength(e,charnum,nchars);
        }

        public int getCharNumAtPosition(float x , float y){
            return textBridge.getCharNumAtPosition(e,x,y);
        }
    }

    /**
     * BridgeUpdateHandle for &lt;tref&gt; element.
     */
    protected class TRefBridge 
        extends AbstractTextChildTextContent {

        public TRefBridge(BridgeContext ctx,
                          SVGTextElementBridge parent,
                          Element e) {
            super(ctx,parent,e);
        }
    }

    /**
     * BridgeUpdateHandle for &lt;textPath&gt; element.
     */
    protected class TextPathBridge 
        extends AbstractTextChildTextContent{

        public TextPathBridge(BridgeContext ctx,
                              SVGTextElementBridge parent,
                              Element e){
            super(ctx,parent,e);
        }
    }

    /**
     * BridgeUpdateHandle for &lt;tspan&gt; element.
     */
    protected class TspanBridge 
        extends AbstractTextChildTextContent {

        public TspanBridge(BridgeContext ctx,
                           SVGTextElementBridge parent,
                           Element e){
            super(ctx,parent,e);
        }
        
        /**
         * Handle the dynamic update for the attributes of 
         * &lt;tspan&gt; : 'x', 'y', 'dx', 'dy' and 'rotate'.
         */
        public void handleDOMAttrModifiedEvent(MutationEvent evt){
            String attrName = evt.getAttrName();
            if (attrName.equals(SVG_X_ATTRIBUTE) ||
                attrName.equals(SVG_Y_ATTRIBUTE) ||
                attrName.equals(SVG_DX_ATTRIBUTE) ||
                attrName.equals(SVG_DY_ATTRIBUTE) ||
                attrName.equals(SVG_ROTATE_ATTRIBUTE)) {
                
                //recompute the layout of the text node
                textBridge.computeLayoutedText();
            }
        }        
    }

    //Implementation of TextContent
    public int getNumberOfChars(){
        return getNumberOfChars(e);
    }

    public Rectangle2D getExtentOfChar(int charnum ){
        return getExtentOfChar(e,charnum);
    }

    public Point2D getStartPositionOfChar(int charnum){
        return getStartPositionOfChar(e,charnum);
    }

    public Point2D getEndPositionOfChar(int charnum){
        return getEndPositionOfChar(e,charnum);
    }
    
    public void selectSubString(int charnum, int nchars){
        selectSubString(e,charnum,nchars);
    }

    public float getRotationOfChar(int charnum){
        return getRotationOfChar(e,charnum);
    }

    public float getComputedTextLength(){
        return getComputedTextLength(e);
    }

    public float getSubStringLength(int charnum, int nchars){
        return getSubStringLength(e,charnum,nchars);
    }

    public int getCharNumAtPosition(float x , float y){
        return getCharNumAtPosition(e,x,y);
    }

    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getNumberOfChars()}.
     */
    protected int getNumberOfChars(Element element){

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();

        //get the index in the aci for the first character
        //of the element
        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if (firstChar == -1)
            return 0; // Element not part of aci (no chars in elem usually)

        int lastChar = getLastCharacterIndexForElement(aci,element);

        StrokingTextPainter.TextRun lastRun = null;
        //retrieve the text run for the text node
        List list = getTextRuns((TextNode)node);

        int visible = 0;

        for(int k = firstChar ; k <= lastChar ; k++ ){

            for( int l = 0 ; l < list.size() ; l++ ){
                StrokingTextPainter.TextRun run = 
                    (StrokingTextPainter.TextRun)list.get(l);

                TextSpanLayout layout =  run.getLayout();

                if ( layout.hasCharacterIndex(k) ){
                    if ( layout.isOnATextPath() ){
                        
                        GVTGlyphVector vector = layout.getGlyphVector();

                        //alt glyph ?
                        if ( layout.isAltGlyph() ){
                                //get the number of glyph visible here
                            int glyphs = vector.getNumGlyphs();
                            int visibleGlyphs = 0;
                            for( int h=0 ; h < glyphs ; h++ ){
                                if ( vector.isGlyphVisible(h)){
                                    visibleGlyphs++;
                                }
                            }
                                //get the number of character associated 
                                //to this run
                            int charactersInRun = 1;
                            while ( layout.hasCharacterIndex( k+1 )){
                                charactersInRun++;
                                k++;
                            }
                            visible += (int)(charactersInRun*visibleGlyphs/glyphs);
                        }
                        else{
                            int lastGlyphIndexFound = -1;
                            do{
                                int glyphIndex = layout.getGlyphIndex(k);
                                if(  glyphIndex == -1 ){
                                    //probable missing glyph
                                    if ( layout.isLeftToRight() ){
                                        glyphIndex = 1 + lastGlyphIndexFound;
                                    }
                                    else{
                                        glyphIndex = ( lastGlyphIndexFound == -1) 
                                            ? vector.getNumGlyphs()-1
                                            : lastGlyphIndexFound -1;
                                    }
                                }
                                lastGlyphIndexFound = glyphIndex;
                                if ( vector.isGlyphVisible( glyphIndex ) ){
                                    visible++;
                                }
                            
                                k++;
                            }while (k <= lastChar && layout.hasCharacterIndex(k) );
                            //got one too far;
                            k--;
                        }
                             
                    }
                    else{
                        visible++;
                        while ( k < lastChar && layout.hasCharacterIndex(k+1) ){
                            k++;
                            visible++;
                        }
                    }
                }
            }
        }

        //return( lastChar - firstChar + 1 );
        return visible;

    }


    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getExtentOfChar(int charnum)}.
     */
    protected Rectangle2D getExtentOfChar(Element element,int charnum ){

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();

        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if ( firstChar == -1 )
            return null;

        //retrieve the text run for the text node
        List list = getTextRuns((TextNode)node);

        //find the character 'charnum' in the text run
        CharacterInformation info = getCharacterInformation(list, firstChar,charnum, aci);
            
        if ( info != null ){

            //retrieve the glyphvector containing the glyph
            //for 'charnum'
            GVTGlyphVector it = info.layout.getGlyphVector();

            Shape b;

            if ( info.glyphIndexStart == info.glyphIndexEnd ){
                b = it.getGlyphOutline(info.glyphIndexStart);
            }
            else{
                GeneralPath path = new GeneralPath();
                for( int k = info.glyphIndexStart ; 
                     k <= info.glyphIndexEnd;
                     k++){

                    path.append(it.getGlyphOutline(k),false);
                }
                b = path;
            }

            //get the transform for the node
            AffineTransform at = getCTM();

            b = at.createTransformedShape(b);
            
            //return the bounding box of the outline
            return b.getBounds2D();

        }
        else{
            return null;
        }
    }


    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getStartPositionOfChar(int charnum)}.
     */
    protected Point2D getStartPositionOfChar(Element element,int charnum){

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();

        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if ( firstChar == -1 )
            return null;

        //retrieve the text run for the text node
        List list = getTextRuns((TextNode)node);

        //find the character 'charnum' in the text run
        CharacterInformation info = getCharacterInformation(list, firstChar,charnum, aci);

        if ( info != null ){
            return getStartPoint( info );            
        }
        else{
            return null;
        }
    }

    protected Point2D getStartPoint(CharacterInformation info){

        GVTGlyphVector it = info.layout.getGlyphVector();

        Point2D b = it.getGlyphPosition(info.glyphIndexStart);

        Point2D result = new Point2D.Float();
            
        AffineTransform glyphTransform = it.getGlyphTransform(info.glyphIndexStart);

        double x = 0,y = 0;

        //glyph are defined starting at position (0,0)
        if ( glyphTransform != null ){
            
            //apply the glyph transformation to the start point
            glyphTransform.transform(new Point2D.Double(x,y),result);
            x = result.getX();
            y = result.getY();
            
        }
        
        //apply the glyph translation to the start point
        AffineTransform af = AffineTransform.getTranslateInstance(b.getX(), b.getY());

        af.transform(new Point2D.Double(x,y),result);                
        
        //apply the node transformation to the start point
        AffineTransform at = new AffineTransform(getCTM());
        Point2D startPoint = new Point2D.Float();
        at.transform(result,startPoint);
        
        return startPoint;
    }

    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getEndPositionOfChar(int charnum)}.
     */
    protected Point2D getEndPositionOfChar(Element element,int charnum ){

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();
        TextNode textNode = (TextNode)node;

        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if ( firstChar == -1 )
            return null;

        //retrieve the text run for the text node
        List list = getTextRuns((TextNode)node);

        //find the glyph information for the character 'charnum'
        CharacterInformation info = getCharacterInformation(list, firstChar,charnum, aci);
            
        if ( info != null ){
            return getEndPoint(info);
        }
        else{
            return null;
        }
    }

    protected Point2D getEndPoint(CharacterInformation info){

        GVTGlyphVector it = info.layout.getGlyphVector();
        
        Point2D b = it.getGlyphPosition(info.glyphIndexEnd);
        
        Point2D result = new Point2D.Float();
        
        AffineTransform glyphTransform = it.getGlyphTransform(info.glyphIndexEnd);
        
        GVTGlyphMetrics metrics = it.getGlyphMetrics(info.glyphIndexEnd);
        
        double x = 0,y = 0;
            
        x = metrics.getHorizontalAdvance();
            
        if ( glyphTransform != null ){
                
            glyphTransform.transform(new Point2D.Double(x,y),result);
            x = result.getX();
            y = result.getY();
            
        }
        
        AffineTransform af = AffineTransform.getTranslateInstance(b.getX(), b.getY());
        
        af.transform(new Point2D.Double(x,y),result);                
        
        AffineTransform at = new AffineTransform(getCTM());
        Point2D endPoint = new Point2D.Float();
        at.transform(result,endPoint);
        
        return endPoint;
        
    }
    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getRotationOfChar(int charnum)}.
     */
    protected float getRotationOfChar(Element element, int charnum){

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();
        TextNode textNode = (TextNode)node;

        //first the first character for the element
        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if ( firstChar == -1 )
            return 0;

        //retrieve the text run for the text node
        List list = getTextRuns((TextNode)node);

        //find the glyph information for the character 'charnum'
        CharacterInformation info = getCharacterInformation(list, firstChar,charnum, aci);
            

        double angle = 0.0;
        int nbGlyphs = 0;

        if ( info != null ){

            GVTGlyphVector it = info.layout.getGlyphVector();

            for( int k = info.glyphIndexStart ;
                 k <= info.glyphIndexEnd ;
                 k++ ){

                nbGlyphs++;
                
                double glyphAngle = 0.0;

                //the glyph transform contains only a scale and a rotate.
                AffineTransform glyphTransform = it.getGlyphTransform(k);

                if ( glyphTransform != null ){
                    double cosTheta = glyphTransform.getScaleX();
                    double sinTheta = glyphTransform.getShearX();
                
                    //extract the angle
                    if ( cosTheta == 0.0 ){
                        if ( sinTheta > 0 ){
                            glyphAngle = Math.PI;
                        }
                        else{
                            glyphAngle = -Math.PI;
                        }
                    }
                    else{
                        glyphAngle = Math.atan(sinTheta/cosTheta);
                        if ( cosTheta < 0 ){
                            glyphAngle += Math.PI;
                        }
                    }
                    //get a degrees value for the angle
                    //SVG angle are clock wise java anticlockwise 
                
                    glyphAngle = (Math.toDegrees( - glyphAngle ) ) % 360.0;

                //remove the orientation from the value
                angle += glyphAngle - info.getComputedOrientationAngle();
                }
            
            
            }
        }
        return (float)(angle / nbGlyphs );
    }

    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getComputedTextLength()}.
     */
    protected float getComputedTextLength(Element e) {
        return getSubStringLength(e,0,getNumberOfChars(e));
    }

    /**
     * Implementation of {@link
     * org.w3c.dom.svg.SVGTextContentElement#getSubStringLength(int charnum,int nchars)}.
     */
    protected float getSubStringLength(Element element,
                                       int charnum, 
                                       int nchars){
        float length = 0;

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();
        TextNode textNode = (TextNode)node;

        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if ( firstChar == -1 )
            return -1;

        List list = getTextRuns(textNode);

        CharacterInformation currentInfo = getCharacterInformation
            (list, firstChar,charnum,aci);
        CharacterInformation lastCharacterInRunInfo = null;
        int chIndex = currentInfo.characterIndex+1;

        for( int k = charnum +1; k < charnum +nchars ; k++){

            //reach the next run
            if ( currentInfo.layout.hasCharacterIndex(chIndex) ){
                chIndex++;
                continue;
            }

            lastCharacterInRunInfo = getCharacterInformation
                (list,firstChar,k-1,aci);

            //if the text run change compute the distance between the 
            //first character of the run and the last
            length += distanceFirstLastCharacterInRun
                (currentInfo,lastCharacterInRunInfo);

            currentInfo = getCharacterInformation(list,firstChar,k,aci);
            chIndex = currentInfo.characterIndex+1;
            lastCharacterInRunInfo = null;
        }

        if ( lastCharacterInRunInfo == null ){
            lastCharacterInRunInfo = getCharacterInformation
                (list,firstChar,charnum+nchars-1,aci);
        }
        //add the length between the end position of the last character
        //and the first character in the run
        length += distanceFirstLastCharacterInRun(currentInfo,lastCharacterInRunInfo);

        return length;
    }

    protected float distanceFirstLastCharacterInRun
        (CharacterInformation first, CharacterInformation last){
        
        float [] advs = first.layout.getGlyphAdvances();
        
        int firstStart = first.glyphIndexStart;
        int firstEnd   = first.glyphIndexEnd;
        int lastStart  = last.glyphIndexStart;
        int lastEnd    = last.glyphIndexEnd;

        int start = (firstStart<lastStart)?firstStart:lastStart;
        int end   = (firstEnd<lastEnd)?lastEnd:firstEnd;
        return advs[end+1] - advs[start];
    }

    protected float distanceBetweenRun
        (CharacterInformation last, CharacterInformation first){
        
        float distance;
        Point2D startPoint;
        Point2D endPoint;
        CharacterInformation info = new CharacterInformation();

        //determine where the last run stops

        info.layout = last.layout;
        info.glyphIndexEnd = last.layout.getGlyphCount()-1;

        startPoint = getEndPoint(info);

        //determine where the next run starts
        info.layout = first.layout;
        info.glyphIndexStart = 0;

        endPoint = getStartPoint(info);

        if( first.isVertical() ){
            distance = (float)(endPoint.getY() - startPoint.getY());
        }
        else{
            distance = (float)(endPoint.getX() - startPoint.getX());
        }
    
        return distance;
    }
                                                    

    /**
     * Select an ensemble of characters for that element.
     *
     * TODO : report the selection to the selection
     *  manager in JSVGComponent.
     */
    protected void selectSubString(Element element, int charnum, int nchars){

        AttributedCharacterIterator aci = 
            ((TextNode)node).getAttributedCharacterIterator();
        TextNode textNode = (TextNode)node;

        int firstChar = getFirstCharacterIndexForElement(aci,element);

        if ( firstChar == -1 )
            return;

        List list = getTextRuns(textNode);

        int lastChar = getLastCharacterIndexForElement(aci,element);

        CharacterInformation firstInfo = getCharacterInformation
            (list, firstChar,charnum,aci);
        CharacterInformation lastInfo = getCharacterInformation
            (list, firstChar,charnum+nchars-1,aci);
        
        Mark firstMark = textNode.getMarkerForChar(firstInfo.characterIndex,true);
        Mark lastMark;

        if ( lastInfo != null && lastInfo.characterIndex <= lastChar ){
            lastMark = textNode.getMarkerForChar(lastInfo.characterIndex,false);
        }
        else{
            lastMark = textNode.getMarkerForChar(lastChar,false);
        }

        textNode.setSelection(firstMark,lastMark);
        
    }

    protected int getCharNumAtPosition(Element e, float x, float y){
        
        TextNode textNode = (TextNode)node;

        //check if there is an hit
        List list = getTextRuns(textNode);

        //going backward in the list to catch the last character
        // displayed at that position
        TextHit hit = null;

        for( int i = list.size()-1 ; i>= 0 && hit == null; i-- ){

            hit = ((StrokingTextPainter.TextRun)list.get(i)).getLayout().hitTestChar(x,y);
        }

        if ( hit == null ){
            return -1;
        }

        AttributedCharacterIterator aci = ((TextNode)node).getAttributedCharacterIterator();

        //found an hit, check if it belong to the element
        int first = getFirstCharacterIndexForElement( aci, e );
        int last  = getLastCharacterIndexForElement( aci, e );

        int hitIndex = hit.getCharIndex();

        if ( hitIndex >= first && hitIndex <= last ){
            
            return hitIndex - first;
        }
        else{
            return -1;
        }
    }

    /**
     * Find the first index in the ACI for the first character
     * of the element.
     */
    protected int getFirstCharacterIndexForElement(AttributedCharacterIterator aci, Element element){

        // calculate which chars in the string belong to this element
        int firstChar = -1;
        for (int i = 0; i < aci.getEndIndex(); i++) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
            GVTAttributedCharacterIterator.
            TextAttribute.TEXT_COMPOUND_DELIMITER);
            if (delimeter == element || nodeAncestorOf(element, delimeter)) {
                firstChar = i;
                break;
            }
        }
        return firstChar;
    }

    /**
     * Find the last index in the ACI for the first character
     * of the element.
     */
    protected int getLastCharacterIndexForElement(AttributedCharacterIterator aci, Element element){

        //find the last char of the element in the aci
        int lastChar = aci.getEndIndex()-1;
        for (int i = aci.getEndIndex()-1; i >= 0; i--) {
            aci.setIndex(i);
            Element delimeter = (Element)aci.getAttribute(
                GVTAttributedCharacterIterator.
                TextAttribute.TEXT_COMPOUND_DELIMITER);
            if (delimeter == element || nodeAncestorOf(element, delimeter)) {
                lastChar = i;
                break;
            }
        }
        return lastChar;
    }

    /**
     * Retrieve the list of layout for the 
     * text node.
     */
    protected List getTextRuns(TextNode node){
        //System.out.println(node.getTextRuns());
        if ( node.getTextRuns() == null ){
            //TODO : need to work out a solution
            //to compute the text runs
            node.getPrimitiveBounds();
        }
        //System.out.println(node.getTextRuns());
        return node.getTextRuns();
    }

    /**
     * Retrieve the information about a character
     * of en element. The element first character in
     * the ACI is 'firstChar' and the character
     * look for is the charnum th character in the 
     * element
     *
     * @param list : list of the layouts
     * @param firstChar : index in the ACI of the first
     *   character for the element
     * @param charnum : index of the character ( among the 
     *   characters of the element ) looked for.
     *
     * @return information about the glyph representing the
     *  character
     */
    protected CharacterInformation getCharacterInformation
        (List list,int startIndex, int charnum, AttributedCharacterIterator aci)
    {
        int visible = 0;
        int k = 0;
        StrokingTextPainter.TextRun run = null;

        for( k = startIndex ; (visible < (charnum+1)) ; k++ ){

            for( int l = 0 ; l < list.size() && (visible < (charnum+1)) ; l++ ){
                run = (StrokingTextPainter.TextRun)list.get(l);

                TextSpanLayout layout = run.getLayout();

                if ( layout.hasCharacterIndex(k) ){
                    if ( layout.isOnATextPath() ){
                        
                        GVTGlyphVector vector = layout.getGlyphVector();

                        //alt glyph ?
                        if ( layout.isAltGlyph() ){
                                //get the number of glyph visible here
                            int glyphs = vector.getNumGlyphs();
                            int visibleGlyphs = 0;
                            for( int h=0 ; h < glyphs ; h++ ){
                                if ( vector.isGlyphVisible(h)){
                                    visibleGlyphs++;
                                }
                            }
                                //get the number of character associated 
                                //to this run
                            int charactersInRun = 1;
                            while ( layout.hasCharacterIndex( k+1 )){
                                charactersInRun++;
                                k++;
                            }
                            visible += (int)(charactersInRun*visibleGlyphs/glyphs);
                            
                            if ( visible > charnum +1 ){
                                visible = charnum +1;
                            }

                        }
                        else{
                            int lastGlyphIndexFound = -1;
                            do{
                                int glyphIndex = layout.getGlyphIndex(k);
                                if(  glyphIndex == -1 ){
                                    //probable missing glyph
                                    if ( layout.isLeftToRight() ){
                                        glyphIndex = 1 + lastGlyphIndexFound;
                                    }
                                    else{
                                        glyphIndex = ( lastGlyphIndexFound == -1) 
                                            ? vector.getNumGlyphs()-1
                                            : lastGlyphIndexFound -1;
                                    }
                                }
                                lastGlyphIndexFound = glyphIndex;
                                if ( vector.isGlyphVisible( glyphIndex ) ){
                                    visible++;
                                }
                                k++;
                            }while ((visible < (charnum+1)) && layout.hasCharacterIndex(k) );
                            //got one too far
                            k--;
                        }
                    }
                    else{
                        visible++;
                        while ( (visible < (charnum+1)) && layout.hasCharacterIndex(k+1) ){
                            k++;
                            visible++;
                        }
                    }
                }
            }
        }

        if ( visible != charnum+1 ){
            return null;
        }

        CharacterInformation info = new CharacterInformation();
        info.characterIndex = k-1;
        info.layout = run.getLayout();

        //check is it is a altGlyph
        if ( info.layout.isAltGlyph() ){
            
            //first visible glyph, last visible glyph
            info.glyphIndexStart = 0;
            info.glyphIndexEnd = info.layout.getGlyphCount()-1;
            boolean visibleGlyph = false;
            GVTGlyphVector vector = info.layout.getGlyphVector();  
            for( int j = 0 ; j < vector.getNumGlyphs() ; j++ ){
                if ( !visibleGlyph && vector.isGlyphVisible(j) ){
                    info.glyphIndexStart = j;
                    visibleGlyph = true;
                }
                if ( visibleGlyph && (!vector.isGlyphVisible(j)) ){
                    info.glyphIndexEnd = j-1;
                    break;
                }
            }

        }
        else{
            
            info.glyphIndexStart = info.layout.getGlyphIndex
                (info.characterIndex);
            
            //special case when the missing glyph does not have a unicode
            //associated to it, it will return -1
            if ( info.glyphIndexStart == -1 ){
                if ( info.layout.isLeftToRight() ){
                    info.glyphIndexStart = info.layout.getGlyphIndex( info.characterIndex-1 )+1;
                }
                else{
                    info.glyphIndexStart = info.layout.getGlyphIndex( info.characterIndex+1 ) -1;
                    if ( info.glyphIndexStart == -2 ){
                        info.glyphIndexStart = info.layout.getGlyphCount()-1;
                    }
                }
            }
            info.glyphIndexEnd = info.glyphIndexStart;
            
        }

        return info;
        /*
          CharacterInformation info = new CharacterInformation();

        info.characterIndex = characterIndex;
        boolean found = false;

        for (int i = 0 ; i < list.size() && !found ; i++) {
            StrokingTextPainter.TextRun run = 
                (StrokingTextPainter.TextRun)list.get(i);

            if ( run.getLayout().hasCharacterIndex(info.characterIndex) ){
                info.layout = run.getLayout();

                aci.setIndex(info.characterIndex);

                //check is it is a altGlyph
                if ( aci.getAttribute(GVTAttributedCharacterIterator.
                             TextAttribute.ALT_GLYPH_HANDLER) != null ){

                    info.glyphIndexStart = 0;
                    info.glyphIndexEnd = info.layout.getGlyphCount()-1;
                }
                else{

                    info.glyphIndexStart = info.layout.getGlyphIndex
                        (info.characterIndex);

                    //special case when the glyph does not have a unicode
                    //associated to it, it will return -1
                    if ( info.glyphIndexStart == -1 ){
                        info.glyphIndexStart = 0;
                        info.glyphIndexEnd = info.layout.getGlyphCount()-1;
                    }
                    else{
                        info.glyphIndexEnd = info.glyphIndexStart;
                    }
                }
                found = true;
            }
        }
        if ( !found ){
            return( null );
        }
        else{
            return( info );
        }
        */
    }

    /**
     * Helper class to collect information about one Glyph
     * in the GlyphVector
     */
    protected class CharacterInformation{
        ///layout associated to the Glyph
        TextSpanLayout layout;
        ///GlyphIndex in the vector
        int glyphIndexStart;

        int glyphIndexEnd;

        ///Character index in the ACI.
        int characterIndex;

        /// Indicates is the glyph is vertical
        public boolean isVertical(){
            return layout.isVertical();
        }
        /// Retrieve the orientation angle for the Glyph
        public double getComputedOrientationAngle(){
            return layout.getComputedOrientationAngle(characterIndex);
        }
    }
    /*
    protected int countCharacter(AttributedCharacterIterator aci, Element element){

        int firstChar = getFirstCharacterIndexForElement(aci,element);
            
        if (firstChar == -1)
            return 0; // Element not part of aci (no chars in elem usually)
        
        int lastChar = getLastCharacterIndexForElement(aci,element);
            
        if (!(( element instanceof SVGTextElement ) || 
              ( element instanceof SVGTextPathElement ))){
            //is tref,tspan,altGlyph,a
            return ( lastChar - firstChar + 1 );
        }
        else if ( element instanceof SVGTextPathElement ){

            return getNumberOfVisibleCharacter(firstChar,lastChar);
        }
        else{
            //text element
            int counted = 0;
            int currentChar = firstChar;

            while ( currentChar > lastChar ){
            
                aci.setIndex(currentChar);

                Element child = aci.get(GVTAttributedCharacterIterator.
                                    TextAttribute.TEXT_COMPOUND_DELIMITER);
            
                if(  child == element ){
                    //character associated to the <text> element 
                    //directly
                    int last = aci.runLimit(GVTAttributedCharacterIterator.
                                            TextAttribute.TEXT_COMPOUND_DELIMITER);
                    
                    counted = last - currentChar+1;
                    currentChar = last+1;
                }
                else{
                    //character associated to a child of text
                    int lastCharChild = getLastCharacterIndexForElement(aci,child);
                    
                    counted+= countCharacter(aci,child);
                    //go to the next child or back to 
                    //the <text> element characters
                    currentChar = lastCharChild+1;
                
                }
            }
        }
    }

    protected int getNumberOfVisibleCharacters(int first, int last){

        //get the layouts
        List list = getTextRuns(textNode);

        StrokingTextPainter.TextRun lastRunUsed = null;        
        for( int i = first ; i <= last ; i++ ){
            if ( lastRunUsed != null ){
                
            }
        }
    }
    */
}
