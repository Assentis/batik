/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package org.apache.batik.test.svg;

import java.io.File;

/**
 * Convenience class for creating a SVGRenderingAccuracyTest with predefined
 * rules for the various configuration parameters.
 *
 * @author <a href="vhardy@apache.org">Vincent Hardy</a>
 * @version $Id$
 */
public abstract class PreconfiguredRenderingTest extends SVGRenderingAccuracyTest {
    /**
     * Generic constants
     */
    public static final String PNG_EXTENSION = ".png";

    public static final String SVG_EXTENSION = ".svg";
    public static final String SVGZ_EXTENSION = ".svgz";

    public static final char PATH_SEPARATOR = '/';

    /**
     * For preconfigured tests, the configuration has to be 
     * derived from the test identifier. The identifier should
     * characterize the SVG file to be tested.
     */
    public void setId(String id){
        super.setId(id);

        String svgFile = id;

        String[] dirNfile = breakSVGFile(svgFile);

        setConfig(buildSVGURL(dirNfile[0], dirNfile[1], dirNfile[2]),
                  buildRefImgURL(dirNfile[0], dirNfile[1]));

        setVariationURL(buildVariationURL(dirNfile[0], dirNfile[1]));
        setSaveVariation(new File(buildSaveVariationFile(dirNfile[0], dirNfile[1])));
        setCandidateReference(new File(buildCandidateReferenceFile(dirNfile[0],dirNfile[1])));
    }

    /**
     * Make the name as simple as possible. For preconfigured SVG files, 
     * we use the test id, which is the relevant identifier for the test
     * user.
     */
    public String getName(){
        return getId();
    }

    /**
     * Gives a chance to the subclass to prepend a prefix to the 
     * svgFile name.
     * The svgURL is built as:
     * getSVGURLPrefix() + svgDir + svgFile
     */
    protected String buildSVGURL(String svgDir, String svgFile, String svgExt){
        return getSVGURLPrefix() + svgDir + svgFile + svgExt;
    }

    protected abstract String getSVGURLPrefix();

    
    /**
     * Gives a chance to the subclass to control the construction
     * of the reference PNG file from the svgFile name
     * The refImgURL is built as:
     * getRefImagePrefix() + svgDir + getRefImageSuffix() + svgFile
     */
    protected String buildRefImgURL(String svgDir, String svgFile){
        return getRefImagePrefix() + svgDir + getRefImageSuffix() + svgFile + PNG_EXTENSION;
    }

    protected abstract String getRefImagePrefix();

    protected abstract String getRefImageSuffix();

    /**
     * Gives a chance to the subclass to control the construction
     * of the variation URL, which is built as:
     * getVariationPrefix() + svgDir + getVariationSuffix() + svgFile + PNG_EXTENSION
     */
    public String buildVariationURL(String svgDir, String svgFile){
        return getVariationPrefix() + svgDir + getVariationSuffix() + svgFile + PNG_EXTENSION;
    }

    protected abstract String getVariationPrefix();

    protected abstract String getVariationSuffix();

    /**
     * Gives a chance to the subclass to control the construction
     * of the saveVariation URL, which is built as:
     * getSaveVariationPrefix() + svgDir + getSaveVariationSuffix() + svgFile + PNG_EXTENSION
     */
    public String  buildSaveVariationFile(String svgDir, String svgFile){
        return getSaveVariationPrefix() + svgDir + getSaveVariationSuffix() + svgFile + PNG_EXTENSION;
    }

    protected abstract String getSaveVariationPrefix();

    protected abstract String getSaveVariationSuffix();

    /**
     * Gives a chance to the subclass to control the construction
     * of the candidateReference URL, which is built as:
     * getCandidatereferencePrefix() + svgDir + getCandidatereferenceSuffix() + svgFile + PNG_EXTENSION
     */
    public String  buildCandidateReferenceFile(String svgDir, String svgFile){
        return getCandidateReferencePrefix() + svgDir + getCandidateReferenceSuffix() + svgFile + PNG_EXTENSION;
    }

    protected abstract String getCandidateReferencePrefix();

    protected abstract String getCandidateReferenceSuffix();


    protected String[] breakSVGFile(String svgFile){
        if(svgFile == null) {
            throw new IllegalArgumentException(svgFile);
        }

        String [] ret = new String[3];

        if (svgFile.endsWith(SVG_EXTENSION)) {
            ret[2] = SVG_EXTENSION;
        } else if (svgFile.endsWith(SVGZ_EXTENSION)) {
            ret[2] = SVGZ_EXTENSION;
        } else {
            throw new IllegalArgumentException(svgFile);
        }

        svgFile = svgFile.substring(0, svgFile.length()-ret[2].length());

        int fileNameStart = svgFile.lastIndexOf(PATH_SEPARATOR);
        String svgDir = "";
        if(fileNameStart != -1){
            if(svgFile.length() < fileNameStart + 2){
                // Nothing after PATH_SEPARATOR
                throw new IllegalArgumentException(svgFile);
            }
            svgDir = svgFile.substring(0, fileNameStart + 1);
            svgFile = svgFile.substring(fileNameStart + 1);
        }
        ret[0] = svgDir;
        ret[1] = svgFile;
        return ret;
    }

}
