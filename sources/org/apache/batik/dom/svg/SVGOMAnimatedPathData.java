/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.dom.svg;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;

import org.w3c.dom.svg.SVGAnimatedPathData;
import org.w3c.dom.svg.SVGException;
import org.w3c.dom.svg.SVGPathSegList;

/**
 * This class is the implementation of
 * the SVGAnimatedPathData interface.
 *
 * @author <a href="mailto:nicolas.socheleau@bitflash.com">Nicolas Socheleau</a>
 * @version $Id$
 */
public class SVGOMAnimatedPathData 
    implements SVGAnimatedPathData,
               LiveAttributeValue {

    /**
     * The associated element.
     */
    protected AbstractElement element;

    /**
     * The attribute's namespace URI.
     */
    protected String namespaceURI;

    /**
     * The attribute's local name.
     */
    protected String localName;

    /**
     * Whether the list is changing.
     */
    protected boolean changing;

    /**
     * SVGPathSegList mapping the static 'd' attribute.
     */
    protected AbstractSVGPathSegList pathSegs;

    /**
     * Default value for the 'd' attribute.
     */
    protected String defaultValue;

    /**
     */
    public SVGOMAnimatedPathData(AbstractElement elt,
                                 String ns,
                                 String ln,
                                 String defaultValue){

        element = elt;
        namespaceURI = ns;
        localName = ln;
        this.defaultValue = defaultValue;
    }

    /**
     */
    public SVGPathSegList getAnimatedNormalizedPathSegList(){
        throw new RuntimeException("TODO :  getAnimatedNormalizedPathSegList() !!");
    }
          
    
    /**
     */
    public SVGPathSegList getAnimatedPathSegList(){
        throw new RuntimeException("TODO :  getAnimatedPathSegList() !!");
    }

    /**
     */
    public SVGPathSegList getNormalizedPathSegList(){
        throw new RuntimeException("TODO :  getNormalizedPathSegList() !!");
    }
          

    /**
     * return the SVGPathSegList mapping
     * the static 'd' attribute
     * of the element
     *
     * @return a path seg list.
     */
    public SVGPathSegList getPathSegList(){
        if ( pathSegs == null ){
            pathSegs = new SVGOMPathSegList();
        }
         return pathSegs;
    }

    /**
     * Called when an Attr node has been added.
     */
    public void attrAdded(Attr node, String newv) {
        if (!changing && pathSegs != null) {
            pathSegs.invalidate();
        }
    }

    /**
     * Called when an Attr node has been modified.
     */
    public void attrModified(Attr node, String oldv, String newv) {
        if (!changing && pathSegs != null) {
            pathSegs.invalidate();
        }
    }

    /**
     * Called when an Attr node has been removed.
     */
    public void attrRemoved(Attr node, String oldv) {
        if (!changing && pathSegs != null) {
            pathSegs.invalidate();
        }
    }
    
    /**
     * SVGPointList implementation for the
     * static 'points' attribute of the element.
     */
    public class SVGOMPathSegList extends AbstractSVGPathSegList {

        /**
         * Create a DOMException.
         */
        protected DOMException createDOMException(short    type,
                                                  String   key,
                                                  Object[] args){
            return element.createDOMException(type,key,args);
        }

        /**
         * Create a SVGException.
         */
        protected SVGException createSVGException(short    type,
                                                  String   key,
                                                  Object[] args){

            return ((SVGOMElement)element).createSVGException(type,key,args);
        }

        /**
         * Retrieve the value of the attribute 'points'.
         */
        protected String getValueAsString(){
            Attr attr = element.getAttributeNodeNS(namespaceURI, localName);
            if (attr == null) {
                return defaultValue;
            }
            return attr.getValue();
        }

        /**
         * Set the value of the attribute 'points'
         */
        protected void setAttributeValue(String value){
            try{
                changing = true;
                element.setAttributeNS(namespaceURI, localName, value);
            }
            finally{
                changing = false;
            }
        }
    }
}
