/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.extension.svg;


public interface BatikExtConstants {

    /** Namespace for batik extentions. */
    public static final String BATIK_EXT_NAMESPACE_URI = 
        "http://xml.apache.org/batik/ext";

    /** Tag name for Batik's regular poly extension. */
    public static final String BATIK_EXT_REGULAR_POLYGON_TAG = 
        "regularPolygon";

    /** Tag name for Batik's star extension. */
    public static final String BATIK_EXT_STAR_TAG = 
        "star";

    /** Tag name for Batik's flowText extension (SVG 1.2). */
    public static final String BATIK_EXT_FLOW_TEXT_TAG = 
        "flowText";

    /** Tag name for Batik's flowText extension Region element (SVG 1.2). */
    public static final String BATIK_EXT_FLOW_REGION_TAG = 
        "flowRegion";

    /** Tag name for Batik's flowText extension Region element (SVG 1.2). */
    public static final String BATIK_EXT_FLOW_REGION_EXCLUDE_TAG = 
        "flowRegionExclude";

    /** Tag name for Batik's flowText extension div element SVG 1.2). */
    public static final String BATIK_EXT_FLOW_DIV_TAG = 
        "flowDiv";

    /** Tag name for Batik's flowText extension p element SVG 1.2). */
    public static final String BATIK_EXT_FLOW_PARA_TAG = 
        "flowPara";

    /** Tag name for Batik's flowText extension flow Region break 
     *  element SVG 1.2). */
    public static final String BATIK_EXT_FLOW_REGION_BREAK_TAG = 
        "flowRegionBreak";

    /** Tag name for Batik's flowText extension line element SVG 1.2). */
    public static final String BATIK_EXT_FLOW_LINE_TAG = 
        "flowLine";

    /** Tag name for Batik's flowText extension span element SVG 1.2). */
    public static final String BATIK_EXT_FLOW_SPAN_TAG = 
        "flowSpan";

    /** Tag name for Batik's solid color extension (SVG 1.2). */
    public static final String BATIK_EXT_SOLID_COLOR_TAG = 
        "solidColor";

    /** Tag name for Batik's color switch extension. */
    public static final String BATIK_EXT_COLOR_SWITCH_TAG = 
        "colorSwitch";

    /** Tag name for Batik's histogram normalization extension. */
    public static final String BATIK_EXT_HISTOGRAM_NORMALIZATION_TAG =
        "histogramNormalization";

    /** Tag name for Batik's multiImage extension. */
    public static final String BATIK_EXT_MULTI_IMAGE_TAG =
        "multiImage";

    /** Tag name for Batik's subImage multiImage extension. */
    public static final String BATIK_EXT_SUB_IMAGE_TAG =
        "subImage";
    /** Tag name for Batik's subImageRef multiImage extension. */
    public static final String BATIK_EXT_SUB_IMAGE_REF_TAG =
        "subImageRef";

    /** Attribute name for dx attribute */
    public static final String BATIK_EXT_DX_ATRIBUTE =
        "dx";
    
    /** Attribute name for dy attribute */
    public static final String BATIK_EXT_DY_ATRIBUTE =
        "dy";
    
    /** Attribute name for dw attribute */
    public static final String BATIK_EXT_DW_ATRIBUTE =
        "dw";
    
    /** Attribute name for dh attribute */
    public static final String BATIK_EXT_DH_ATRIBUTE =
        "dh";

    /** Attribute name for filterPrimitiveMarginsUnits */
    public static final String BATIK_EXT_FILTER_PRIMITIVE_MARGINS_UNITS_ATTRIBUTE
        = "filterPrimitiveMarginsUnits";

    /** Attribute name for filterMarginsUnits */
    public static final String BATIK_EXT_FILTER_MARGINS_UNITS_ATTRIBUTE
        = "filterMarginsUnits";

    /** Attribute name for x attribute */
    public static final String BATIK_EXT_X_ATTRIBUTE = 
        "x";
    /** Attribute name for y attribute */
    public static final String BATIK_EXT_Y_ATTRIBUTE = 
        "y";
    /** Attribute name for width attribute */
    public static final String BATIK_EXT_WIDTH_ATTRIBUTE = 
        "width";
    /** Attribute name for height attribute */
    public static final String BATIK_EXT_HEIGHT_ATTRIBUTE = 
        "height";

    /** Attribute name for margin psudo-attribute */
    public static final String BATIK_EXT_MARGIN_ATTRIBUTE = 
        "margin";
    /** Attribute name for top-margin attribute */
    public static final String BATIK_EXT_TOP_MARGIN_ATTRIBUTE = 
        "top-margin";
    /** Attribute name for right-margin attribute */
    public static final String BATIK_EXT_RIGHT_MARGIN_ATTRIBUTE = 
        "right-margin";
    /** Attribute name for bottom-margin attribute */
    public static final String BATIK_EXT_BOTTOM_MARGIN_ATTRIBUTE = 
        "bottom-margin";
    /** Attribute name for left-margin attribute */
    public static final String BATIK_EXT_LEFT_MARGIN_ATTRIBUTE = 
        "left-margin";
    /** Attribute name for indent attribute/property */
    public static final String BATIK_EXT_INDENT_ATTRIBUTE = 
        "indent";
    /** Attribute name for justification */
    public static final String BATIK_EXT_JUSTIFICATION_ATTRIBUTE = 
        "justification";
    /** Value for justification to start of region */
    public static final String BATIK_EXT_JUSTIFICATION_START_VALUE  = "start";
    /** Value for justification to middle of region */
    public static final String BATIK_EXT_JUSTIFICATION_MIDDLE_VALUE = "middle";
    /** Value for justification to end of region */
    public static final String BATIK_EXT_JUSTIFICATION_END_VALUE    = "end";
    /** Value for justification to both edges of region */
    public static final String BATIK_EXT_JUSTIFICATION_FULL_VALUE = "full";


    /** Attribute name for preformated data */
    public static final String BATIK_EXT_PREFORMATTED_ATTRIBUTE = 
        "preformatted";

   /** Attribute name for preformated data */
    public static final String BATIK_EXT_VERTICAL_ALIGN_ATTRIBUTE =
        "vertical-align";

    /** Value for vertical-align to top of region */
    public static final String BATIK_EXT_ALIGN_TOP_VALUE    = "top";
    /** Value for vertical-align to middle of region */
    public static final String BATIK_EXT_ALIGN_MIDDLE_VALUE = "middle";
    /** Value for vertical-align to bottom of region */
    public static final String BATIK_EXT_ALIGN_BOTTOM_VALUE = "bottom";

    /** Attribute name for sides attribute */
    public static final String BATIK_EXT_SIDES_ATTRIBUTE = 
        "sides";

    /** Attribute name for inner radius attribute */
    public static final String BATIK_EXT_IR_ATTRIBUTE = 
        "ir";

    /** Attribute name for trim percent attribute */
    public static final String BATIK_EXT_TRIM_ATTRIBUTE = 
        "trim";

    /** Attribute name for pixel-width attribute */
    public static final String BATIK_EXT_MIN_PIXEL_SIZE_ATTRIBUTE = 
        "min-pixel-size";

    /** Attribute name for pixel-height attribute */
    public static final String BATIK_EXT_MAX_PIXEL_SIZE_ATTRIBUTE = 
        "max-pixel-size";

    /** Attribute name for color attribute */
    public static final String BATIK_EXT_SOLID_COLOR_PROPERTY = 
        "solid-color";

    /** Attribute name for opacity attribute */
    public static final String BATIK_EXT_SOLID_OPACITY_PROPERTY = 
        "solid-opacity";

    /** Default value for filter dx */
    public static final String SVG_FILTER_DX_DEFAULT_VALUE = "0";

    /** Default value for filter dy */
    public static final String SVG_FILTER_DY_DEFAULT_VALUE = "0";

    /** Default value for filter dw */
    public static final String SVG_FILTER_DW_DEFAULT_VALUE = "0";

    /** Default value for filter dh */
    public static final String SVG_FILTER_DH_DEFAULT_VALUE = "0";
}
