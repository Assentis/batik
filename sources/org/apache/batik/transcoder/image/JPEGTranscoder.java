/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.transcoder.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.resources.Messages;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * This class is an <tt>ImageTranscoder</tt> that produces a JPEG image.
 *
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id$
 */
public class JPEGTranscoder extends ImageTranscoder {

    /**
     * Constructs a new transcoder that produces jpeg images.
     */
    public JPEGTranscoder() {
        hints.put(ImageTranscoder.KEY_BACKGROUND_COLOR, Color.white);
    }

    /**
     * Creates a new ARGB image with the specified dimension.
     * @param width the image width in pixels
     * @param height the image height in pixels
     */
    public BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    /**
     * Writes the specified image to the specified output.
     * @param img the image to write
     * @param output the output where to store the image
     * @param TranscoderException if an error occured while storing the image
     */
    public void writeImage(BufferedImage img, TranscoderOutput output)
            throws TranscoderException {
        OutputStream ostream = output.getOutputStream();
        // The outputstream wrapper protects the JPEG encoder from
        // exceptions due to stream closings.  If it gets an exception
        // it nulls out the stream and just ignores any future calls.
        ostream = new OutputStreamWrapper(ostream);

        if (ostream == null) {
            throw new TranscoderException(
                Messages.formatMessage("jpeg.badoutput", null));
        }
        float quality;
        if (hints.containsKey(KEY_QUALITY)) {
            quality = ((Float)hints.get(KEY_QUALITY)).floatValue();
        } else {
            handler.error(new TranscoderException(
                Messages.formatMessage("jpeg.unspecifiedQuality", null)));
            quality = 1f;
        }
        try {
            JPEGImageEncoder jpegEncoder = JPEGCodec.createJPEGEncoder(ostream);
            JPEGEncodeParam params = JPEGCodec.getDefaultJPEGEncodeParam(img);
            params.setQuality(quality, true);
            float PixSzMM = userAgent.getPixelUnitToMillimeter();
            int PixSzInch = (int)(25.4/PixSzMM+0.5);
            params.setDensityUnit(JPEGEncodeParam.DENSITY_UNIT_DOTS_INCH);
            params.setXDensity(PixSzInch);
            params.setYDensity(PixSzInch);
            jpegEncoder.encode(img, params);
        } catch (IOException ex) {
            throw new TranscoderException(ex);
        }
    }

    // --------------------------------------------------------------------
    // Keys definition
    // --------------------------------------------------------------------

    /**
     * The encoder quality factor key.
     * <TABLE BORDER="0" CELLSPACING="0" CELLPADDING="1">
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Key: </TH>
     * <TD VALIGN="TOP">KEY_QUALITY</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Value: </TH>
     * <TD VALIGN="TOP">Float (between 0 and 1)</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Default: </TH>
     * <TD VALIGN="TOP">1 (no lossy)</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Required: </TH>
     * <TD VALIGN="TOP">Recommended</TD></TR>
     * <TR>
     * <TH VALIGN="TOP" ALIGN="RIGHT"><P ALIGN="RIGHT">Description: </TH>
     * <TD VALIGN="TOP">Specify the JPEG image encoding quality.</TD></TR>
     * </TABLE>
     */
    public static final TranscodingHints.Key KEY_QUALITY
        = new QualityKey();

    /**
     * A transcoding Key represented the JPEG image quality.
     */
    private static class QualityKey extends TranscodingHints.Key {
        public boolean isCompatibleValue(Object v) {
            if (v instanceof Float) {
                float q = ((Float)v).floatValue();
                return (q > 0 && q <= 1f);
            } else {
                return false;
            }
        }
    }

    /**
     *  This class will never throw an IOException, instead it eats
     * them and then ignores any future calls to it's interface.
     */
    private static class OutputStreamWrapper extends OutputStream {
        OutputStream os;
        /**
         * Constructs a wrapper around <tt>os</tt> that will not throw
         * IOExceptions.
         * <@param os>The Stream to wrap.
         */
        OutputStreamWrapper(OutputStream os) {
            this.os = os;
        }

        public void close() throws IOException { 
            if (os == null) return; 
            try {
                os.close();
            } catch (IOException ioe) {
                os = null;
            }
        }

        public void flush() throws IOException { 
            if (os == null) return; 
            try {
                os.flush();
            } catch (IOException ioe) {
                os = null;
            }
        }

        public void write(byte[] b) throws IOException { 
            if (os == null) return; 
            try {
                os.write(b);
            } catch (IOException ioe) {
                os = null;
            }
        }
        
        public void write(byte[] b, int off, int len) throws IOException { 
            if (os == null) return; 
            try {
                os.write(b, off, len);
            } catch (IOException ioe) {
                os = null;
            }
        }
        
        public void write(int b)  throws IOException { 
            if (os == null) return; 
            try {
                os.write(b);
            } catch (IOException ioe) {
                os = null;
            }
        }
    }
}
