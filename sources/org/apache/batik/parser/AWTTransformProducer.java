/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.parser;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Reader;

/**
 * This class provides an implementation of the PathHandler that initializes
 * an AffineTransform from the value of a 'transform' attribute.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class AWTTransformProducer implements TransformListHandler {
    /**
     * The value of the current affine transform.
     */
    protected AffineTransform affineTransform;

    /**
     * Utility method for creating an AffineTransform.
     * @param r The reader used to read the transform specification.
     */
    public static AffineTransform createAffineTransform(Reader r)
        throws ParseException {
        TransformListParser p = new TransformListParser();
        AWTTransformProducer th = new AWTTransformProducer();

        p.setTransformListHandler(th);
        p.parse(r);

        return th.getAffineTransform();
    }

    /**
     * Utility method for creating an AffineTransform.
     * @param r The reader used to read the transform specification.
     */
    public static AffineTransform createAffineTransform(String s)
        throws ParseException {
        TransformListParser p = new TransformListParser();
        AWTTransformProducer th = new AWTTransformProducer();

        p.setTransformListHandler(th);
        p.parse(s);

        return th.getAffineTransform();
    }

    /**
     * Returns the AffineTransform object initialized during the last parsing.
     * @return the transform or null if this handler has not been used by
     *         a parser.
     */
    public AffineTransform getAffineTransform() {
        return affineTransform;
    }

    /**
     * Implements {@link TransformListHandler#startTransformList()}.
     */
    public void startTransformList() throws ParseException {
        affineTransform = new AffineTransform();
    }

    /**
     * Implements {@link
     * TransformListHandler#matrix(float,float,float,float,float,float)}.
     */
    public void matrix(float a, float b, float c, float d, float e, float f)
        throws ParseException {
        affineTransform.concatenate(new AffineTransform(a, b, c, d, e, f));
    }

    /**
     * Implements {@link TransformListHandler#rotate(float)}.
     */
    public void rotate(float theta) throws ParseException {
        affineTransform.concatenate
            (AffineTransform.getRotateInstance(Math.PI * theta / 180));
    }

    /**
     * Implements {@link TransformListHandler#rotate(float,float,float)}.
     */
    public void rotate(float theta, float cx, float cy) throws ParseException {
        AffineTransform at
            = AffineTransform.getRotateInstance(Math.PI * theta / 180, cx, cy);
        affineTransform.concatenate(at);
    }

    /**
     * Implements {@link TransformListHandler#translate(float)}.
     */
    public void translate(float tx) throws ParseException {
        AffineTransform at = AffineTransform.getTranslateInstance(tx, 0);
        affineTransform.concatenate(at);
    }

    /**
     * Implements {@link TransformListHandler#translate(float,float)}.
     */
    public void translate(float tx, float ty) throws ParseException {
        AffineTransform at = AffineTransform.getTranslateInstance(tx, ty);
        affineTransform.concatenate(at);
    }

    /**
     * Implements {@link TransformListHandler#scale(float)}.
     */
    public void scale(float sx) throws ParseException {
        affineTransform.concatenate(AffineTransform.getScaleInstance(sx, sx));
    }

    /**
     * Implements {@link TransformListHandler#scale(float,float)}.
     */
    public void scale(float sx, float sy) throws ParseException {
        affineTransform.concatenate(AffineTransform.getScaleInstance(sx, sy));
    }

    /**
     * Implements {@link TransformListHandler#skewX(float)}.
     */
    public void skewX(float skx) throws ParseException {
        affineTransform.concatenate
            (AffineTransform.getShearInstance(Math.tan(Math.PI * skx / 180),
                                              0));
    }

    /**
     * Implements {@link TransformListHandler#skewY(float)}.
     */
    public void skewY(float sky) throws ParseException {
        affineTransform.concatenate
            (AffineTransform.getShearInstance(0,
                                              Math.tan(Math.PI * sky / 180)));
    }

    /**
     * Implements {@link TransformListHandler#endTransformList()}.
     */
    public void endTransformList() throws ParseException {
    }
}
