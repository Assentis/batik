/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt;

import java.util.List;

/**
 * <tt>GVTTreeWalker</tt> objects are used to navigate a GVT tree or subtree.
 *
 * @author <a href="mailto:tkormann@apache.org">Thierry Kormann</a>
 * @version $Id$
 */
public class GVTTreeWalker {

    /** The GVT root into which text is searched. */
    protected GraphicsNode gvtRoot;

    /** The current GraphicsNode. */
    protected GraphicsNode currentNode;

    /**
     * Constructs a new <tt>GVTTreeWalker</tt>.
     *
     * @param gvtRoot the graphics node root
     */
    public GVTTreeWalker(GraphicsNode gvtRoot) {
        this.gvtRoot = gvtRoot;
        currentNode = gvtRoot;
    }

    /**
     * Returns the root graphics node.
     */
    public GraphicsNode getRoot() {
        return gvtRoot;
    }

    /**
     * Sets the current GraphicsNode to the specified node.
     *
     * @param node the new current graphics node
     * @exception IllegalArgumentException if the node is not part of the GVT Tree
     *                                     this walker is dedicated to
     */
    public void setCurrentGraphicsNode(GraphicsNode node) {
        if (node.getRoot() != gvtRoot) {
            throw new IllegalArgumentException
                ("The node "+node+" is not part of the document "+gvtRoot);
        }
        currentNode = node;
    }

    /**
     * Returns the current <tt>GraphicsNode</tt>.
     */
    public GraphicsNode getCurrentGraphicsNode() {
        return currentNode;
    }

    /**
     * Returns the previous <tt>GraphicsNode</tt>. If the current graphics node
     * does not have a previous node, returns null and retains the current node.
     */
    public GraphicsNode previousGraphicsNode() {
        GraphicsNode result = getPreviousGraphicsNode(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    /**
     * Returns the next <tt>GraphicsNode</tt>. If the current graphics node does
     * not have a next node, returns null and retains the current node.
     */
    public GraphicsNode nextGraphicsNode() {
        GraphicsNode result = getNextGraphicsNode(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    /**
     * Returns the parent of the current <tt>GraphicsNode</tt>. If the current
     * graphics node has no parent, returns null and retains the current node.
     */
    public GraphicsNode parentGraphicsNode() {
        GraphicsNode result = currentNode.getParent();
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    /**
     * Returns the next sibling of the current <tt>GraphicsNode</tt>. If the
     * current graphics node does not have a next sibling, returns null and
     * retains the current node.
     */
    public GraphicsNode getNextSibling() {
        GraphicsNode result = getNextSibling(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    /**
     * Returns the next previous of the current <tt>GraphicsNode</tt>. If the
     * current graphics node does not have a previous sibling, returns null and
     * retains the current node.
     */
    public GraphicsNode getPreviousSibling() {
        GraphicsNode result = getPreviousSibling(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    /**
     * Returns the first child of the current <tt>GraphicsNode</tt>. If the
     * current graphics node does not have a first child, returns null and
     * retains the current node.
     */
    public GraphicsNode firstChild() {
        GraphicsNode result = getFirstChild(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    /**
     * Returns the last child of the current <tt>GraphicsNode</tt>. If the
     * current graphics node does not have a last child, returns null and
     * retains the current node.
     */
    public GraphicsNode lastChild() {
        GraphicsNode result = getLastChild(currentNode);
        if (result != null) {
            currentNode = result;
        }
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    private GraphicsNode getNextGraphicsNode(GraphicsNode node) {
        if (node == null) {
            return null;
        }
        // Go to the first child
        GraphicsNode n = getFirstChild(node);
        if (n != null) {
            return n;
        }

        // Go to the next sibling
        n = getNextSibling(node);
        if (n != null) {
            return n;
        }

        // Go to the first sibling of one of the ancestors
        n = node;
        while ((n = n.getParent()) != null && n != gvtRoot) {
            GraphicsNode t = getNextSibling(n);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private GraphicsNode getPreviousGraphicsNode(GraphicsNode node) {
        if (node == null) {
            return null;
        }

        // The previous of root is null
        if (node == gvtRoot) {
            return null;
        }

        GraphicsNode n = getPreviousSibling(node);

        // Go to the parent of a first child
        if (n == null) {
            return node.getParent();
        }

        // Go to the last child of child...
        GraphicsNode t;
        while ((t = getLastChild(n)) != null) {
            n = t;
        }
        return n;
    }

    private static GraphicsNode getLastChild(GraphicsNode node) {
        if (!(node instanceof CompositeGraphicsNode)) {
            return null;
        }
        CompositeGraphicsNode parent = (CompositeGraphicsNode)node;
        List children = parent.getChildren();
        if (children == null) {
            return null;
        }
        if (children.size() >= 1) {
            return (GraphicsNode)children.get(children.size()-1);
        } else {
            return null;
        }
    }

    private static GraphicsNode getPreviousSibling(GraphicsNode node) {
        CompositeGraphicsNode parent = node.getParent();
        if (parent == null) {
            return null;
        }
        List children = parent.getChildren();
        if (children == null) {
            return null;
        }
        int index = children.indexOf(node);
        if (index-1 >= 0) {
            return (GraphicsNode)children.get(index-1);
        } else {
            return null;
        }
    }

    private static GraphicsNode getFirstChild(GraphicsNode node) {
        if (!(node instanceof CompositeGraphicsNode)) {
            return null;
        }
        CompositeGraphicsNode parent = (CompositeGraphicsNode)node;
        List children = parent.getChildren();
        if (children == null) {
            return null;
        }
        if (children.size() >= 1) {
            return (GraphicsNode)children.get(0);
        } else {
            return null;
        }
    }

    private static GraphicsNode getNextSibling(GraphicsNode node) {
        CompositeGraphicsNode parent = node.getParent();
        if (parent == null) {
            return null;
        }
        List children = parent.getChildren();
        if (children == null) {
            return null;
        }
        int index = children.indexOf(node);
        if (index+1 < children.size()) {
            return (GraphicsNode)children.get(index+1);
        } else {
            return null;
        }
    }
}
