/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.events.DocumentEvent;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.UIEvent;
import org.w3c.dom.events.MouseEvent;

/**
 * A class that manages focus on elements. Users of this class needs
 * to attached this EventListener with the 'mouseover' event type.
 *
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id$
 */
public class FocusManager {

    /**
     * The element that has the focus so far.
     */
    protected EventTarget lastFocusEventTarget;

    /**
     * The document.
     */
    protected Document document;

    /**
     * The EventListener that tracks 'mouseclick' events.
     */
    protected EventListener mouseclickListener;

    /**
     * The EventListener that tracks 'DOMFocusIn' events.
     */
    protected EventListener domFocusInListener;

    /**
     * The EventListener that tracks 'DOMFocusOut' events.
     */
    protected EventListener domFocusOutListener;

    /**
     * The EventListener that tracks 'mouseover' events.
     */
    protected EventListener mouseoverListener;

    /**
     * The EventListener that tracks 'mouseout' events.
     */
    protected EventListener mouseoutListener;

    /**
     * Constructs a new <tt>FocusManager</tt> for the specified document.
     *
     * @param doc the document
     */
    public FocusManager(Document doc) {
        document = doc;
        EventTarget target = (EventTarget)doc;

        mouseclickListener = new MouseClickTacker();
        target.addEventListener("click", mouseclickListener, true);

        mouseoverListener = new MouseOverTacker();
        target.addEventListener("mouseover", mouseoverListener, true);

        mouseoutListener = new MouseOutTacker();
        target.addEventListener("mouseout", mouseoutListener, true);

        domFocusInListener = new DOMFocusInTracker();
        target.addEventListener("DOMFocusIn", domFocusInListener, true);

        domFocusOutListener = new DOMFocusOutTracker();
        target.addEventListener("DOMFocusOut", domFocusOutListener, true);
    }

    /**
     * Returns the current element that has the focus or null if any.
     */
    public EventTarget getCurrentEventTarget() {
        return lastFocusEventTarget;
    }

    /**
     * Removes all listeners attached to the document and that manage focus.
     */
    public void dispose() {
        EventTarget target = (EventTarget)document;
        target.removeEventListener("click", mouseclickListener, true);
        target.removeEventListener("mouseover", mouseoverListener, true);
        target.removeEventListener("mouseout", mouseoutListener, true);
        target.removeEventListener("DOMFocusIn", domFocusInListener, true);
        target.removeEventListener("DOMFocusOut", domFocusOutListener, true);
    }

    /**
     * The class that is responsible for tracking 'mouseclick' changes.
     */
    protected class MouseClickTacker implements EventListener {

        public void handleEvent(Event evt) {
            MouseEvent mevt = (MouseEvent)evt;
            fireDOMActivateEvent(evt.getTarget(), mevt.getDetail());
        }
    }

    /**
     * The class that is responsible for tracking 'DOMFocusIn' changes.
     */
    protected class DOMFocusInTracker implements EventListener {

        public void handleEvent(Event evt) {
            if (lastFocusEventTarget != null && 
                lastFocusEventTarget != evt.getTarget()) {
                fireDOMFocusOutEvent(lastFocusEventTarget);
            }
            lastFocusEventTarget = evt.getTarget();
        }
    }

    /**
     * The class that is responsible for tracking 'DOMFocusOut' changes.
     */
    protected class DOMFocusOutTracker implements EventListener {

        public void handleEvent(Event evt) {
            lastFocusEventTarget = null;
        }
    }

    /**
     * The class that is responsible to update the focus according to
     * 'mouseover' event.
     */
    protected class MouseOverTacker implements EventListener {

        public void handleEvent(Event evt) {
            EventTarget target = evt.getTarget();
            fireDOMFocusInEvent(target);
        }
    }

    /**
     * The class that is responsible to update the focus according to
     * 'mouseout' event.
     */
    protected class MouseOutTacker implements EventListener {

        public void handleEvent(Event evt) {
            EventTarget target = evt.getTarget();
            fireDOMFocusOutEvent(target);
        }
    }

    /**
     * Fires a 'DOMFocusIn' event to the specified target.
     *
     * @param target the event target
     */
    protected void fireDOMFocusInEvent(EventTarget target) {
        DocumentEvent docEvt = 
            (DocumentEvent)((Element)target).getOwnerDocument();
        UIEvent uiEvt = (UIEvent)docEvt.createEvent("UIEvents");
        uiEvt.initUIEvent("DOMFocusIn", true, false, null, 0);
        target.dispatchEvent(uiEvt);
    }

    /**
     * Fires a 'DOMFocusOut' event to the specified target.
     *
     * @param target the event target
     */
    protected void fireDOMFocusOutEvent(EventTarget target) {
        DocumentEvent docEvt = 
            (DocumentEvent)((Element)target).getOwnerDocument();
        UIEvent uiEvt = (UIEvent)docEvt.createEvent("UIEvents");
        uiEvt.initUIEvent("DOMFocusOut", true, false, null, 0);
        target.dispatchEvent(uiEvt);
    }
    
    /**
     * Fires a 'DOMActivate' event to the specified target.
     *
     * @param target the event target
     * @param detailArg the detailArg parameter of the event
     */
    protected void fireDOMActivateEvent(EventTarget target, int detailArg) {
        DocumentEvent docEvt = 
            (DocumentEvent)((Element)target).getOwnerDocument();
        UIEvent uiEvt = (UIEvent)docEvt.createEvent("UIEvents");
        uiEvt.initUIEvent("DOMActivate", true, true, null, detailArg);
        target.dispatchEvent(uiEvt);
    }
}
