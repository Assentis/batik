/*
 * @(#)Painter.java %W% %E%
 *
 * Copyright 1995-1999 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */
package org.apache.batik.apps.regsvggen;

import java.awt.*;

public interface Painter {
        public void paint(Graphics2D g);
        public boolean isTextAsShapes();
        public Dimension getSize();
}

