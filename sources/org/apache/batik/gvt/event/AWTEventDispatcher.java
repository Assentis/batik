/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt.event;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import java.lang.reflect.Array;

import java.util.EventListener;
import java.util.EventObject;

import javax.swing.event.EventListenerList;

import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.gvt.TextNode;

/**
 * An EventDispatcher implementation based on AWT events.
 *
 * <p>Mouse events are dispatched to their "containing" node (the
 * GraphicsNode corresponding to the mouse event coordinate). Searches
 * for containment are performed from the EventDispatcher's "root"
 * node.</p>
 *
 * @author <a href="bill.haneman@ireland.sun.com>Bill Haneman</a>
 * @author <a href="cjolif@ilog.fr>Christophe Jolif</a>
 * @author <a href="tkormann@ilog.fr>Thierry Kormann</a>
 * @version $Id$
 */
public class AWTEventDispatcher implements EventDispatcher,
                                           MouseListener,
                                           MouseMotionListener,
                                           KeyListener {

    /**
     * The root GraphicsNode as determined by setRootNode().
     */
    protected GraphicsNode root;

    /**
     * The base AffineTransform for InputEvent-to-GraphicsNodeEvent
     * coordinates as determined by setBaseTransform().
     */
    protected AffineTransform baseTransform;

    /**
     * The global listener list.
     */
    protected EventListenerList glisteners;

    /**
     * The lastest node which has been targeted by an event.
     */
    protected GraphicsNode lastHit;

    /**
     * A dummy graphics node to dispacth "deselect" mouse events to. ie.
     * when the mouse is clicked outside any nodes.
     */
    protected GraphicsNode dummyNode = new TextNode();

    /**
     * The current GraphicsNode targeted by an key events.
     */
    protected GraphicsNode currentKeyEventTarget;

    private int nodeIncrementEventID = KeyEvent.KEY_PRESSED;
    private int nodeIncrementEventCode = KeyEvent.VK_TAB;
    private int nodeIncrementEventModifiers = 0;
    private int nodeDecrementEventID = KeyEvent.KEY_PRESSED;
    private int nodeDecrementEventCode = KeyEvent.VK_TAB;
    private int nodeDecrementEventModifiers = InputEvent.SHIFT_MASK;

    /**
     * Constructs a new event dispatcher.
     */
    public AWTEventDispatcher() {
    }

    /**
     * Sets the root node for MouseEvent dispatch containment searches
     * and field selections.
     * @param root the root node
     */
    public void setRootNode(GraphicsNode root) {
        this.root = root;
    }

    /**
     * Returns the root node for MouseEvent dispatch containment
     * searches and field selections.
     */
    public GraphicsNode getRootNode() {
        return root;
    }

    /**
     * Sets the base transform applied to MouseEvent coordinates prior
     * to dispatch.
     * @param t the affine transform
     */
    public void setBaseTransform(AffineTransform t) {
        baseTransform = t;
    }

    /**
     * Returns the base transform applied to MouseEvent coordinates prior
     * to dispatch.
     */
    public AffineTransform getBaseTransform() {
        return new AffineTransform(baseTransform);
    }

    //
    // AWT listeners wrapper
    //

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mousePressed(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mouseReleased(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mouseEntered(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mouseExited(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mouseClicked(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mouseMoved(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT mouse event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeMouseEvent.
     * @param evt the mouse event to propagate
     */
    public void mouseDragged(MouseEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT key event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeKeyEvent.
     * @param evt the key event to propagate
     */
    public void keyPressed(KeyEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT key event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeKeyEvent.
     * @param evt the key event to propagate
     */
    public void keyReleased(KeyEvent evt) {
        dispatchEvent(evt);
    }

    /**
     * Dispatches the specified AWT key event down to the GVT tree.
     * The mouse event is mutated to a GraphicsNodeKeyEvent.
     * @param evt the key event to propagate
     */
    public void keyTyped(KeyEvent evt) {
        dispatchEvent(evt);
    }

    //
    // Global GVT listeners support
    //

    /**
     * Adds the specified 'global' GraphicsNodeMouseListener which is
     * notified of all MouseEvents dispatched.
     * @param l the listener to add
     */
    public void addGraphicsNodeMouseListener(GraphicsNodeMouseListener l) {
        if (glisteners == null) {
            glisteners = new EventListenerList();
        }
        glisteners.add(GraphicsNodeMouseListener.class, l);
    }

    /**
     * Removes the specified 'global' GraphicsNodeMouseListener which is
     * notified of all MouseEvents dispatched.
     * @param l the listener to remove
     */
    public void removeGraphicsNodeMouseListener(GraphicsNodeMouseListener l) {
        if (glisteners != null) {
            glisteners.remove(GraphicsNodeMouseListener.class, l);
        }
    }

    /**
     * Adds the specified 'global' GraphicsNodeKeyListener which is
     * notified of all KeyEvents dispatched.
     * @param l the listener to add
     */
    public void addGraphicsNodeKeyListener(GraphicsNodeKeyListener l) {
        if (glisteners == null) {
            glisteners = new EventListenerList();
        }
        glisteners.add(GraphicsNodeKeyListener.class, l);
    }

    /**
     * Removes the specified 'global' GraphicsNodeKeyListener which is
     * notified of all KeyEvents dispatched.
     * @param l the listener to remove
     */
    public void removeGraphicsNodeKeyListener(GraphicsNodeKeyListener l) {
        if (glisteners != null) {
            glisteners.remove(GraphicsNodeKeyListener.class, l);
        }
    }

    /**
     * Returns an array of listeners that were added to this event
     * dispatcher and of the specified type.
     * @param listenerType the type of the listeners to return
     */
    public EventListener [] getListeners(Class listenerType) {
        Object array =
            Array.newInstance(listenerType,
                              glisteners.getListenerCount(listenerType));
        Object[] pairElements = glisteners.getListenerList();
        for (int i = 0, j = 0;i < pairElements.length-1; i+=2) {
            if (pairElements[i].equals(listenerType)) {
                Array.set(array, j, pairElements[i+1]);
                ++j;
            }
        }
        return (EventListener[]) array;
    }

    //
    // Event dispatch implementation
    //

    /**
     * Dispatches the specified AWT event.
     * @param evt the event to dispatch
     */
    public void dispatchEvent(EventObject evt) {
        if (root == null)
            return;
        if (evt instanceof MouseEvent) {
            dispatchMouseEvent((MouseEvent) evt);
        } else if (evt instanceof KeyEvent) {
            InputEvent e = (InputEvent)evt;
            if (isNodeIncrementEvent(e)) {
                incrementKeyTarget();
            } else if (isNodeDecrementEvent(e)) {
                decrementKeyTarget();
            } else {
                dispatchKeyEvent((KeyEvent) evt);
            }
        }
    }

    /**
     * Dispatches the specified AWT key event.
     * @param evt the key event to dispatch
     */
    protected void dispatchKeyEvent(KeyEvent evt) {
        currentKeyEventTarget = lastHit;
        if (currentKeyEventTarget != null) {
            processKeyEvent
                (new GraphicsNodeKeyEvent(currentKeyEventTarget,
                                          evt.getID(),
                                          evt.getWhen(),
                                          evt.getModifiers(),
                                          evt.getKeyCode(),
                                          evt.getKeyChar()));
        }
    }
    
    /**
     * Dispatches the specified AWT mouse event.
     * @param evt the mouse event to dispatch
     */
    protected void dispatchMouseEvent(MouseEvent evt) {
        GraphicsNodeMouseEvent gvtevt;
        Point2D p = new Point2D.Float(evt.getX(), evt.getY());
        Point2D gnp = p;
        if (baseTransform != null) {
            gnp = baseTransform.transform(p, null);
        }

        GraphicsNode node = root.nodeHitAt(gnp);

        // If the receiving node has changed, send a notification
        // check if we enter a new node
        Point screenPos = evt.getComponent().getLocationOnScreen();
        screenPos.x += evt.getX();
        screenPos.y += evt.getY();


        if (lastHit != node) {
            // post an MOUSE_EXITED
            if (lastHit != null) {
                gvtevt = new GraphicsNodeMouseEvent(lastHit,
                                                    MouseEvent.
                                                    MOUSE_EXITED,
                                                    evt.getWhen(),
                                                    evt.getModifiers(),
                                                    (float)gnp.getX(),
                                                    (float)gnp.getY(),
                                                    (int)Math.floor(p.getX()),
                                                    (int)Math.floor(p.getY()),
                                                    screenPos.x,
                                                    screenPos.y,
                                                    evt.getClickCount(),
                                                    node);
                processMouseEvent(gvtevt);
                // lastHit.processMouseEvent(gvtevt);
            }
            // post an MOUSE_ENTERED
            if (node != null) {
                gvtevt = new GraphicsNodeMouseEvent(node,
                                                    MouseEvent.
                                                    MOUSE_ENTERED,
                                                    evt.getWhen(),
                                                    evt.
                                                    getModifiers(),
                                                    (float)gnp.getX(),
                                                    (float)gnp.getY(),
                                                    (int)Math.floor(p.getX()),
                                                    (int)Math.floor(p.getY()),
                                                    screenPos.x,
                                                    screenPos.y,
                                                    evt.getClickCount(),
                                                    lastHit);
                processMouseEvent(gvtevt);
                // node.processMouseEvent(gvtevt);
            }
        }
        // In all cases, dispatch the original event
        if (node != null) {
            gvtevt = new GraphicsNodeMouseEvent(node,
                                                evt.getID(),
                                                evt.getWhen(),
                                                evt.getModifiers(),
                                                (float)gnp.getX(),
                                                (float)gnp.getY(),
                                                (int)Math.floor(p.getX()),
                                                (int)Math.floor(p.getY()),
                                                screenPos.x,
                                                screenPos.y,
                                                evt.getClickCount(),
                                                null);

            // node.processMouseEvent(gvtevt);
            processMouseEvent(gvtevt);

        } else if (node == null && evt.getID() == evt.MOUSE_CLICKED
              && evt.getClickCount() == 1) {
            // this is a deselect event, dispatch it to dummy node
            gvtevt = new GraphicsNodeMouseEvent(dummyNode,
                                                evt.getID(),
                                                evt.getWhen(),
                                                evt.getModifiers(),
                                                (float)gnp.getX(),
                                                (float)gnp.getY(),
                                                (int)Math.floor(p.getX()),
                                                (int)Math.floor(p.getY()),
                                                screenPos.x,
                                                screenPos.y,
                                                evt.getClickCount(),
                                                null);

            processMouseEvent(gvtevt);
        }
        lastHit = node;
    }

    /**
     * Processes the specified event by firing the 'global' listeners
     * attached to this event dispatcher.
     * @param evt the event to process
     */
    protected void processMouseEvent(GraphicsNodeMouseEvent evt) {
        if (glisteners != null) {
            GraphicsNodeMouseListener[] listeners =
                (GraphicsNodeMouseListener[])
                getListeners(GraphicsNodeMouseListener.class);
            switch (evt.getID()) {
            case GraphicsNodeMouseEvent.MOUSE_MOVED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mouseMoved(evt);
                }
                break;
            case GraphicsNodeMouseEvent.MOUSE_DRAGGED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mouseDragged(evt);
                }
                break;
            case GraphicsNodeMouseEvent.MOUSE_ENTERED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mouseEntered(evt);
                }
                break;
            case GraphicsNodeMouseEvent.MOUSE_EXITED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mouseExited(evt);
                }
                break;
            case GraphicsNodeMouseEvent.MOUSE_CLICKED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mouseClicked(evt);
                }
                break;
            case GraphicsNodeMouseEvent.MOUSE_PRESSED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mousePressed(evt);
                }
                break;
            case GraphicsNodeMouseEvent.MOUSE_RELEASED:
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].mouseReleased(evt);
                }
                break;
            default:
                throw new Error("Unknown Mouse Event type: "+evt.getID());
            }
        }
    }

    /**
     * Dispatches a graphics node key event to by firing the 'global'
     * listeners attached to this event dispatcher.
     *
     * @param evt the evt to dispatch
     */
    public void processKeyEvent(GraphicsNodeKeyEvent evt) {
        if ((glisteners != null)) {
            GraphicsNodeKeyListener[] listeners =
                (GraphicsNodeKeyListener[])
                getListeners(GraphicsNodeKeyListener.class);

            switch (evt.getID()) {
            case GraphicsNodeKeyEvent.KEY_PRESSED:
                for (int i=0; i<listeners.length; ++i) {
                    listeners[i].keyPressed(evt);
                }
                break;
            case GraphicsNodeKeyEvent.KEY_RELEASED:
                for (int i=0; i<listeners.length; ++i) {
                    listeners[i].keyReleased(evt);
                }
                break;
            case GraphicsNodeKeyEvent.KEY_TYPED:
                for (int i=0; i<listeners.length; ++i) {
                    listeners[i].keyTyped(evt);
                }
                break;
            default:
                throw new Error("Unknown Key Event type: "+evt.getID());
            }
        }
        evt.consume();
    }


    private void incrementKeyTarget() {
        // <!> FIXME TODO: Not implemented.
        throw new Error("Increment not implemented.");
    }

    private void decrementKeyTarget() {
        // <!> FIXME TODO: Not implemented.
        throw new Error("Decrement not implemented.");
    }

    /**
     * Associates all InputEvents of type <tt>e.getID()</tt>
     * with "incrementing" of the currently selected GraphicsNode.
     */
    public void setNodeIncrementEvent(InputEvent e) {
        nodeIncrementEventID = e.getID();
        if (e instanceof KeyEvent) {
            nodeIncrementEventCode = ((KeyEvent) e).getKeyCode();
        }
        nodeIncrementEventModifiers = e.getModifiers();
    }

    /**
     * Associates all InputEvents of type <tt>e.getID()</tt>
     * with "decrementing" of the currently selected GraphicsNode.
     * The notion of "currently selected" GraphicsNode is used
     * for dispatching KeyEvents.
     */
    public void setNodeDecrementEvent(InputEvent e) {
        nodeDecrementEventID = e.getID();
        if (e instanceof KeyEvent) {
            nodeDecrementEventCode = ((KeyEvent) e).getKeyCode();
        }
        nodeDecrementEventModifiers = e.getModifiers();
    }

    /**
     * Returns true if the input event e is a node increment event,
     * false otherwise.
     * @param e the input event
     */
    protected boolean isNodeIncrementEvent(InputEvent e) {
        // TODO: Improve code readability!
        return ((e.getID() == nodeIncrementEventID) &&
                ((e instanceof KeyEvent) ?
                     (((KeyEvent) e).getKeyCode() == nodeIncrementEventCode) : true) &&
                ((e.getModifiers() & nodeIncrementEventModifiers) != 0));
    }

    /**
     * Returns true if the input event e is a node decrement event,
     * false otherwise.
     */
    protected boolean isNodeDecrementEvent(InputEvent e) {
        // TODO: Improve code readability!
        return ((e.getID() == nodeDecrementEventID) &&
                ((e instanceof KeyEvent) ?
                     ( ((KeyEvent) e).getKeyCode() == nodeDecrementEventCode) : true) &&
                ((e.getModifiers() & nodeDecrementEventModifiers) != 0  ));

    }
}


