/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.bridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import java.net.URL;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.batik.dom.util.SAXDocumentFactory;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGOMDocument;

import org.apache.batik.dom.util.DocumentFactory;
import org.apache.batik.dom.util.XLinkSupport;

import org.apache.batik.script.Interpreter;
import org.apache.batik.script.InterpreterException;

import org.apache.batik.util.EncodingUtilities;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.RunnableQueue;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.XMLResourceDescriptor;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

/**
 * This class contains the informations needed by the SVG scripting.
 *
 * @author <a href="mailto:stephane@hillion.org">Stephane Hillion</a>
 * @version $Id$
 */
public class ScriptingEnvironment extends BaseScriptingEnvironment {

    /**
     * Used in 'parseXML()'.
     */
    protected final static String FRAGMENT_PREFIX =
        "<svg xmlns='" +
        SVGConstants.SVG_NAMESPACE_URI +
        "' xmlns:xlink='" +
        XLinkSupport.XLINK_NAMESPACE_URI +
        "'>";

    /**
     * The timer for periodic or delayed tasks.
     */
    protected Timer timer = new Timer(true);

    /**
     * The update manager.
     */
    protected UpdateManager updateManager;

    /**
     * The update runnable queue.
     */
    protected RunnableQueue updateRunnableQueue;

    /**
     * The DOMNodeInserted event listener.
     */
    protected EventListener domNodeInsertedListener;

    /**
     * The DOMNodeRemoved event listener.
     */
    protected EventListener domNodeRemovedListener;

    /**
     * The SVGAbort event listener.
     */
    protected EventListener svgAbortListener =
        new ScriptingEventListener("onabort");

    /**
     * The SVGError event listener.
     */
    protected EventListener svgErrorListener =
        new ScriptingEventListener("onerror");

    /**
     * The SVGResize event listener.
     */
    protected EventListener svgResizeListener =
        new ScriptingEventListener("onresize");

    /**
     * The SVGScroll event listener.
     */
    protected EventListener svgScrollListener =
        new ScriptingEventListener("onscroll");

    /**
     * The SVGUnload event listener.
     */
    protected EventListener svgUnloadListener =
        new ScriptingEventListener("onunload");

    /**
     * The SVGZoom event listener.
     */
    protected EventListener svgZoomListener =
        new ScriptingEventListener("onzoom");

    /**
     * The begin event listener.
     */
    protected EventListener beginListener =
        new ScriptingEventListener("onbegin");

    /**
     * The end event listener.
     */
    protected EventListener endListener =
        new ScriptingEventListener("onend");

    /**
     * The repeat event listener.
     */
    protected EventListener repeatListener =
        new ScriptingEventListener("onrepeat");

    /**
     * The focusin event listener.
     */
    protected EventListener focusinListener =
        new ScriptingEventListener("onfocusin");

    /**
     * The focusout event listener.
     */
    protected EventListener focusoutListener =
        new ScriptingEventListener("onfocusout");

    /**
     * The activate event listener.
     */
    protected EventListener activateListener =
        new ScriptingEventListener("onactivate");

    /**
     * The click event listener.
     */
    protected EventListener clickListener =
        new ScriptingEventListener("onclick");

    /**
     * The mousedown event listener.
     */
    protected EventListener mousedownListener =
        new ScriptingEventListener("onmousedown");

    /**
     * The mouseup event listener.
     */
    protected EventListener mouseupListener =
        new ScriptingEventListener("onmouseup");

    /**
     * The mouseover event listener.
     */
    protected EventListener mouseoverListener =
        new ScriptingEventListener("onmouseover");

    /**
     * The mouseout event listener.
     */
    protected EventListener mouseoutListener =
        new ScriptingEventListener("onmouseout");

    /**
     * The mousemove event listener.
     */
    protected EventListener mousemoveListener =
        new ScriptingEventListener("onmousemove");

    /**
     * The keypress event listener.
     */
    protected EventListener keypressListener =
        new ScriptingEventListener("onkeypress");

    /**
     * The keydown event listener.
     */
    protected EventListener keydownListener =
        new ScriptingEventListener("onkeydown");

    /**
     * The keyup event listener.
     */
    protected EventListener keyupListener =
        new ScriptingEventListener("onkeyup");



    /**
     * Creates a new ScriptingEnvironment.
     * @param ctx the bridge context
     */
    public ScriptingEnvironment(BridgeContext ctx) {
        super(ctx);
        updateManager = ctx.getUpdateManager();
        updateRunnableQueue = updateManager.getUpdateRunnableQueue();
        
        // Add the scripting listeners.
        addScriptingListeners(document.getDocumentElement());

        // Add the listeners responsible of updating the event attributes
        EventTarget et = (EventTarget)document;
        domNodeInsertedListener = new DOMNodeInsertedListener();
        et.addEventListener("DOMNodeInserted",
                            domNodeInsertedListener,
                            false);
        domNodeRemovedListener = new DOMNodeRemovedListener();
        et.addEventListener("DOMAttrRemoved",
                            domNodeRemovedListener,
                            false);
    }

    /**
     * Creates a new Window object.
     */
    public org.apache.batik.script.Window createWindow(Interpreter interp,
                                                       String lang) {
        return new Window(interp, lang);
    }

    /**
     * Runs an event handler.
     */
    public void runEventHandler(String script, Event evt, String lang) {
        Interpreter interpreter = bridgeContext.getInterpreter(lang);
        if (interpreter == null) {
            if (userAgent != null) {
                userAgent.displayError
                    (new Exception("unknow language: " + lang));
            }
        }

        interpreter.bindObject(EVENT_NAME, evt);
        interpreter.bindObject(ALTERNATE_EVENT_NAME, evt);
            
        try {
            checkCompatibleScriptURL(lang, docPURL);
            interpreter.evaluate(script);
        } catch (InterpreterException ie) {
            handleInterpreterException(ie);
        } catch (SecurityException se) {
            handleSecurityException(se);
        }
    }

    /**
     * Interrupts the periodic tasks and dispose this ScriptingEnvironment.
     */
    public void interrupt() {
        timer.cancel();
        // Remove the scripting listeners.
        removeScriptingListeners(document.getDocumentElement());

        // Remove the listeners responsible of updating the event attributes
        EventTarget et = (EventTarget)document;
        et.removeEventListener("DOMNodeInserted",
                               domNodeInsertedListener,
                               false);
        et.removeEventListener("DOMAttrRemoved",
                               domNodeRemovedListener,
                               false);
    }

    /**
     * Adds the scripting listeners to the given element.
     */
    protected void addScriptingListeners(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            // Attach the listeners
            Element elt = (Element)node;
            EventTarget target = (EventTarget)elt;
            if (SVGConstants.SVG_NAMESPACE_URI.equals(elt.getNamespaceURI())) {
                if (SVGConstants.SVG_SVG_TAG.equals(elt.getLocalName())) {
                    // <svg> listeners
                    if (elt.hasAttributeNS(null, "onabort")) {
                        target.addEventListener("SVGAbort",
                                                svgAbortListener, false);
                    }
                    if (elt.hasAttributeNS(null, "onerror")) {
                        target.addEventListener("SVGError",
                                                svgErrorListener, false);
                    }
                    if (elt.hasAttributeNS(null, "onresize")) {
                        target.addEventListener("SVGResize",
                                                svgResizeListener, false);
                    }
                    if (elt.hasAttributeNS(null, "onscroll")) {
                        target.addEventListener("SVGScroll",
                                            svgScrollListener, false);
                    }
                    if (elt.hasAttributeNS(null, "onunload")) {
                        target.addEventListener("SVGUnload",
                                                svgUnloadListener, false);
                    }
                    if (elt.hasAttributeNS(null, "onzoom")) {
                        target.addEventListener("SVGZoom",
                                                svgZoomListener, false);
                    }
                } else {
                    String name = elt.getLocalName();
                    if (name.equals(SVGConstants.SVG_SET_TAG) ||
                        name.startsWith("animate")) {
                        // animation listeners
                        if (elt.hasAttributeNS(null, "onbegin")) {
                            target.addEventListener("beginEvent",
                                                    beginListener ,
                                                    false);
                        }
                        if (elt.hasAttributeNS(null, "onend")) {
                            target.addEventListener("endEvent",
                                                    endListener,
                                                    false);
                        }
                        if (elt.hasAttributeNS(null, "onrepeat")) {
                            target.addEventListener("repeatEvent",
                                                    repeatListener ,
                                                    false);
                        }
                        return;
                    }
                }
            }

            // UI listeners
            if (elt.hasAttributeNS(null, "onfocusin")) {
                target.addEventListener("DOMFocusIn", focusinListener, false);
            }
            if (elt.hasAttributeNS(null, "onfocusout")) {
                target.addEventListener("DOMFocusOut", focusoutListener,
                                        false);
            }
            if (elt.hasAttributeNS(null, "onactivate")) {
                target.addEventListener("DOMActivate", activateListener,
                                        false);
            }
            if (elt.hasAttributeNS(null, "onclick")) {
                target.addEventListener("click", clickListener, false);
            } 
            if (elt.hasAttributeNS(null, "onmousedown")) {
                target.addEventListener("mousedown", mousedownListener, false);
            }
            if (elt.hasAttributeNS(null, "onmouseup")) {
                target.addEventListener("mouseup", mouseupListener, false);
            }
            if (elt.hasAttributeNS(null, "onmouseover")) {
                target.addEventListener("mouseover", mouseoverListener, false);
            }
            if (elt.hasAttributeNS(null, "onmouseout")) {
                target.addEventListener("mouseout", mouseoutListener, false);
            }
            if (elt.hasAttributeNS(null, "onmousemove")) {
                target.addEventListener("mousemove", mousemoveListener, false);
            }
            if (elt.hasAttributeNS(null, "onkeypress")) {
                target.addEventListener("keypress", keypressListener, false);
            }
            if (elt.hasAttributeNS(null, "onkeydown")) {
                target.addEventListener("keydown", keydownListener, false);
            }
            if (elt.hasAttributeNS(null, "onkeyup")) {
                target.addEventListener("keyup", keyupListener, false);
            }
        }

        // Adds the listeners to the children
        for (Node n = node.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            addScriptingListeners(n);
        }
    }

    /**
     * Removes the scripting listeners from the given element.
     */
    protected void removeScriptingListeners(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            // Detach the listeners
            Element elt = (Element)node;
            EventTarget target = (EventTarget)elt;
            if (SVGConstants.SVG_NAMESPACE_URI.equals(elt.getNamespaceURI())) {
                if (SVGConstants.SVG_SVG_TAG.equals(elt.getLocalName())) {
                    // <svg> listeners
                    target.removeEventListener("SVGAbort",
                                               svgAbortListener, false);
                    target.removeEventListener("SVGError",
                                               svgErrorListener, false);
                    target.removeEventListener("SVGResize",
                                               svgResizeListener, false);
                    target.removeEventListener("SVGScroll",
                                               svgScrollListener, false);
                    target.removeEventListener("SVGUnload",
                                               svgUnloadListener, false);
                    target.removeEventListener("SVGZoom",
                                               svgZoomListener, false);
                } else {
                    String name = elt.getLocalName();
                    if (name.equals(SVGConstants.SVG_SET_TAG) ||
                        name.startsWith("animate")) {
                        // animation listeners
                        target.removeEventListener("beginEvent",
                                                   beginListener ,
                                                   false);
                        target.removeEventListener("endEvent",
                                                   endListener,
                                                   false);
                        target.removeEventListener("repeatEvent",
                                                   repeatListener ,
                                                   false);
                        return;
                    }
                }
            }

            // UI listeners
            target.removeEventListener("DOMFocusIn", focusinListener, false);
            target.removeEventListener("DOMFocusOut", focusoutListener, false);
            target.removeEventListener("DOMActivate", activateListener, false);
            target.removeEventListener("click", clickListener, false);
            target.removeEventListener("mousedown", mousedownListener, false);
            target.removeEventListener("mouseup", mouseupListener, false);
            target.removeEventListener("mouseover", mouseoverListener, false);
            target.removeEventListener("mouseout", mouseoutListener, false);
            target.removeEventListener("mousemove", mousemoveListener, false);
            target.removeEventListener("keypress", keypressListener, false);
            target.removeEventListener("keydown", keydownListener, false);
            target.removeEventListener("keyup", keyupListener, false);
        }

        // Removes the listeners from the children
        for (Node n = node.getFirstChild();
             n != null;
             n = n.getNextSibling()) {
            removeScriptingListeners(n);
        }
    }

    /**
     * To interpret a script.
     */
    protected class EvaluateRunnable implements Runnable {
        protected Interpreter interpreter;
        protected String script;
        public EvaluateRunnable(String s, Interpreter interp) {
            interpreter = interp;
            script = s;
        }
        public void run() {
            try {
                interpreter.evaluate(script);
            } catch (InterpreterException ie) {
                handleInterpreterException(ie);
            }
        }
    }

    /**
     * To interpret a script.
     */
    protected class EvaluateIntervalRunnable implements Runnable {
        /**
         * Incremented each time this runnable is added to the queue.
         */
        public int count;
        public boolean error;

        protected Interpreter interpreter;
        protected String script;

        public EvaluateIntervalRunnable(String s, Interpreter interp) {
            interpreter = interp;
            script = s;
        }
        public void run() {
            if (error) {
                return;
            }
            count--;
            try {
                interpreter.evaluate(script);
            } catch (InterpreterException ie) {
                handleInterpreterException(ie);
                error = true;
            } catch (Exception e) {
                if (userAgent != null) {
                    userAgent.displayError(e);
                }
                error = true;
            }
        }
    }

    /**
     * To call a Runnable.
     */
    protected class EvaluateRunnableRunnable implements Runnable {
        /**
         * Incremented each time this runnable is put in the queue.
         */
        public int count;
        public boolean error;

        protected Runnable runnable;

        public EvaluateRunnableRunnable(Runnable r) {
            runnable = r;
        }
        public void run() {
            if (error) {
                return;
            }
            count--;
            try {
                runnable.run();
            } catch (Exception e) {
                if (userAgent != null) {
                    userAgent.displayError(e);
                }
                error = true;
            }
        }
    }

    /**
     * Represents the window object of this environment.
     */
    protected class Window implements org.apache.batik.script.Window {

        /**
         * The associated interpreter.
         */
        protected Interpreter interpreter;

        /**
         * The associated language.
         */
        protected String language;

        /**
         * Creates a new Window for the given language.
         */
        public Window(Interpreter interp, String lang) {
            interpreter = interp;
            language = lang;
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#setInterval(String,long)}.
         */
        public Object setInterval(final String script, long interval) {
            TimerTask tt = new TimerTask() {
                    EvaluateIntervalRunnable eir =
                        new EvaluateIntervalRunnable(script, interpreter);
                    public void run() {
                        if (eir.count > 1) {
                            return;
                        }
                        eir.count++;
                        updateRunnableQueue.invokeLater(eir);
                        if (eir.error) {
                            cancel();
                        }
                    }
                };

            timer.schedule(tt, interval, interval);
            return tt;
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#setInterval(Runnable,long)}.
         */
        public Object setInterval(final Runnable r, long interval) {
            TimerTask tt = new TimerTask() {
                    EvaluateRunnableRunnable eihr =
                        new EvaluateRunnableRunnable(r);
                    public void run() {
                        if (eihr.count > 1) {
                            return;
                        }
                        eihr.count++;
                        updateRunnableQueue.invokeLater(eihr);
                        if (eihr.error) {
                            cancel();
                        }
                    }
                };
            
            timer.schedule(tt, interval, interval);
            return tt;
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#clearInterval(Object)}.
         */
        public void clearInterval(Object interval) {
            ((TimerTask)interval).cancel();
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#setTimeout(String,long)}.
         */
        public Object setTimeout(final String script, long timeout) {
            TimerTask tt = new TimerTask() {
                    public void run() {
                        updateRunnableQueue.invokeLater
                            (new EvaluateRunnable(script, interpreter));
                    }
                };

            timer.schedule(tt, timeout);
            return tt;
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#setTimeout(Runnable,long)}.
         */
        public Object setTimeout(final Runnable r, long timeout) {
            TimerTask tt = new TimerTask() {
                    public void run() {
                        updateRunnableQueue.invokeLater(new Runnable() {
                                public void run() {
                                    try {
                                        r.run();
                                    } catch (Exception e) {
                                        if (userAgent != null) {
                                            userAgent.displayError(e);
                                        }
                                    }
                                }
                            });
                    }
                };

            timer.schedule(tt, timeout);
            return tt;
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#clearTimeout(Object)}.
         */
        public void clearTimeout(Object timeout) {
            ((TimerTask)timeout).cancel();
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#parseXML(String,Document)}.
         */
        public DocumentFragment parseXML(String text, Document doc) {
            // System.out.println("Text: " + text);
            SAXSVGDocumentFactory df = new SAXSVGDocumentFactory
                (XMLResourceDescriptor.getXMLParserClassName());
            String uri = ((SVGOMDocument)bridgeContext.getDocument()).
                getURLObject().toString();
            DocumentFragment result = null;
            try {
                Document d = df.createDocument(uri, new StringReader(text));
                result = doc.createDocumentFragment();
                result.appendChild(doc.importNode(d.getDocumentElement(),
                                                  true));
            } catch (Exception ex) {
                StringBuffer sb = new StringBuffer(text.length() +
                                                   FRAGMENT_PREFIX.length() +
                                                   "</svg>".length());
                sb.append(FRAGMENT_PREFIX);
                sb.append(text);
                sb.append("</svg>");
                String newText = sb.toString();
                try {
                    Document d = df.createDocument
                        (uri, new StringReader(newText));
                    for (Node n = d.getDocumentElement().getFirstChild();
                         n != null;
                         n = n.getNextSibling()) {
                        if (n.getNodeType() == n.ELEMENT_NODE) {
                            n = doc.importNode(n, true);
                            result = doc.createDocumentFragment();
                            result.appendChild(n);
                            break;
                        }
                    }
                } catch (Exception exc) {
                    SAXDocumentFactory sdf = new SAXDocumentFactory
                        (doc.getImplementation(),
                         XMLResourceDescriptor.getXMLParserClassName());
                    try {
                        Document d = sdf.createDocument
                            (uri, new StringReader(text));
                        result = doc.createDocumentFragment();
                        result.appendChild(doc.importNode(d.getDocumentElement(), true));
                    } catch (Exception ext) {
                        if (userAgent != null)
                            userAgent.displayError(ext);
                    }
                }
            }
            return result;
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#getURL(String,GetURLHandler)}.
         */
        public void getURL(String uri, GetURLHandler h) {
            getURL(uri, h, "UTF8");
        }

        /**
         * Implements {@link
         * org.apache.batik.script.Window#getURL(String,GetURLHandler,String)}.
         */
        public void getURL(final String uri,
                           final GetURLHandler h,
                           final String enc) {
            Thread t = new Thread() {
                    public void run() {
                        try {
                            URL burl;
                            burl = ((SVGOMDocument)document).getURLObject();
                            final ParsedURL purl = new ParsedURL(burl, uri);
                            String e = EncodingUtilities.javaEncoding(enc);
                            e = ((e == null) ? enc : e);
                            Reader r =
                                new InputStreamReader(purl.openStream(), e);
                            r = new BufferedReader(r);
                            final StringWriter sw = new StringWriter();
                            int read;
                            char[] buf = new char[4096];
                            while ((read = r.read(buf, 0, buf.length)) != -1) {
                                sw.write(buf, 0, read);
                            }

                            updateRunnableQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        try {
                                            h.getURLDone(true,
                                                         purl.getContentType(),
                                                         sw.toString());
                                        } catch (Exception e){
                                            if (userAgent != null) {
                                                userAgent.displayError(e);
                                            }
                                        }
                                    }
                                });
                        } catch (Exception e) {
                            if (e instanceof SecurityException) {
                                userAgent.displayError(e);
                            }
                            updateRunnableQueue.invokeLater(new Runnable() {
                                    public void run() {
                                        try {
                                            h.getURLDone(false, null, null);
                                        } catch (Exception e){
                                            if (userAgent != null) {
                                                userAgent.displayError(e);
                                            }
                                        }
                                    }
                                });
                        }
                    }

                };
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();

                    
        }


        /**
         * Displays an alert dialog box.
         */
        public void alert(String message) {
            if (userAgent != null) {
                userAgent.showAlert(message);
            }
        }

        /**
         * Displays a confirm dialog box.
         */
        public boolean confirm(String message) {
            if (userAgent != null) {
                return userAgent.showConfirm(message);
            }
            return false;
        }

        /**
         * Displays an input dialog box.
         */
        public String prompt(String message) {
            if (userAgent != null) {
                return userAgent.showPrompt(message);
            }
            return null;
        }

        /**
         * Displays an input dialog box, given the default value.
         */
        public String prompt(String message, String defVal) {
            if (userAgent != null) {
                return userAgent.showPrompt(message, defVal);
            }
            return null;
        }

        /**
         * Returns the current BridgeContext.
         */
        public BridgeContext getBridgeContext() {
            return bridgeContext;
        }

        /**
         * Returns the associated interpreter.
         */
        public Interpreter getInterpreter() {
            return interpreter;
        }
    }

    /**
     * The listener class for 'DOMNodeInserted' event.
     */
    protected class DOMNodeInsertedListener implements EventListener {
        public void handleEvent(Event evt) {
            addScriptingListeners((Node)evt.getTarget());
        }
    }

    /**
     * The listener class for 'DOMNodeRemoved' event.
     */
    protected class DOMNodeRemovedListener implements EventListener {
        public void handleEvent(Event evt) {
            removeScriptingListeners((Node)evt.getTarget());
        }
    }

    /**
     * To handle a scripting event.
     */
    protected class ScriptingEventListener implements EventListener {

        /**
         * The script attribute.
         */
        protected String attribute;
        
        /**
         * Creates a new ScriptingEventListener.
         */
        public ScriptingEventListener(String attr) {
            attribute = attr;
        }

        /**
         * Runs the script.
         */
        public void handleEvent(Event evt) {
            Element elt = (Element)evt.getCurrentTarget();
            // Find the scripting language
            Element e = elt;
            while (e != null &&
                   (!SVGConstants.SVG_NAMESPACE_URI.equals
                    (e.getNamespaceURI()) ||
                    !SVGConstants.SVG_SVG_TAG.equals(e.getLocalName()))) {
                e = SVGUtilities.getParentElement(e);
            }
            if (e == null) {
                return;
            }
            String lang = e.getAttributeNS
                (null, SVGConstants.SVG_CONTENT_SCRIPT_TYPE_ATTRIBUTE);

            // Evaluate the script
            String script = elt.getAttributeNS(null, attribute);
            if (script.length() > 0) {
                runEventHandler(script, evt, lang);
            }
        }
    }
}
