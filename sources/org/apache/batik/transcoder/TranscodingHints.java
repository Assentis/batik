/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.transcoder;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The <tt>TranscodingHints</tt> class defines a way to pass
 * transcoding parameters or options to any transcoders.
 *
 * @author <a href="mailto:Thierry.Kormann@sophia.inria.fr">Thierry Kormann</a>
 * @version $Id$
 */
public class TranscodingHints extends HashMap {

    /**
     * Constructs a new empty <tt>TranscodingHints</tt>.
     */
    public TranscodingHints() {
        this(null);
    }

    /**
     * Constructs a new <tt>TranscodingHints</tt> with keys and values
     * initialized from the specified Map object (which may be null).
     *
     * @param init a map of key/value pairs to initialize the hints
     *          or null if the object should be empty
     */
    public TranscodingHints(Map init) {
        super(7);
        if (init != null) {
            putAll(init);
        }
    }

    /**
     * Returns <tt>true</tt> if this <tt>TranscodingHints</tt> contains a
     * mapping for the specified key, false otherwise.
     *
     * @param key key whose present in this <tt>TranscodingHints</tt>
     * is to be tested.
     * @exception ClassCastException key is not of type
     * <tt>TranscodingHints.Key</tt>
     */
    public boolean containsKey(Object key) {
        return super.containsKey((Key)key);
    }

    /**
     * Returns the value to which the specified key is mapped.
     *
     * @param key a trancoding hint key
     * @exception ClassCastException key is not of type
     * <tt>TranscodingHints.Key</tt>
     */
    public Object get(Object key) {
        return super.get((Key) key);
    }

    /**
     * Maps the specified <tt>key</tt> to the specified <tt>value</tt>
     * in this <tt>TranscodingHints</tt> object.
     *
     * @param key the trancoding hint key.
     * @param value the trancoding hint value.
     * @exception <tt>IllegalArgumentException</tt> value is not
     * appropriate for the specified key.
     * @exception ClassCastException key is not of type
     * <tt>TranscodingHints.Key</tt>
     */
    public Object put(Object key, Object value) {
        if (!((Key) key).isCompatibleValue(value)) {
            throw new IllegalArgumentException(value+
                                               " incompatible with "+
                                               key);
        }
        return super.put(key, value);
    }

    /**
     * Removes the key and its corresponding value from this
     * <tt>TranscodingHints</tt> object.
     *
     * @param key the trancoding hints key that needs to be removed
     * @exception ClassCastException key is not of type
     * <tt>TranscodingHints.Key</tt>
     */
    public Object remove(Object key) {
        return super.remove((Key) key);
    }

    /**
     * Copies all of the keys and corresponding values from the
     * specified <tt>TranscodingHints</tt> object to this
     * <tt>TranscodingHints</tt> object.
     */
    public void putAll(TranscodingHints hints) {
        super.putAll(hints);
    }

    /**
     * Copies all of the mappings from the specified <tt>Map</tt>
     * to this <tt>TranscodingHints</tt>.
     *
     * @param t mappings to be stored in this <tt>TranscodingHints</tt>.
     * @exception ClassCastException key is not of type
     * <tt>TranscodingHints.Key</tt>
     */
    public void putAll(Map m) {
        if (m instanceof TranscodingHints) {
            putAll(((TranscodingHints) m));
        } else {
            Iterator iter = m.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Defines the base type of all keys used to control various
     * aspects of the transcoding operations. Instances of this class
     * are immutable and unique which means that tests for matches can
     * be made using the == operator instead of the more expensive
     * equals() method.
     */
    public abstract static class Key {

        private static Map identitymap = new HashMap(17);

        private String getIdentity() {
            return "Instance("+privatekey+") of "+getClass().getName();
        }

        private synchronized static void recordIdentity(Key k) {
            Object identity = k.getIdentity();
            if (identitymap.containsKey(identity)) {
                throw new IllegalArgumentException(identity+
                                                   " already registered");
            }
            identitymap.put(identity, k);
        }

        private int privatekey;

        /**
         * Construcst a key using the indicated private key.  Each
         * subclass of Key maintains its own unique domain of integer
         * keys. No two objects with the same integer key and of the
         * same specific subclass can be constructed.  An exception
         * will be thrown if an attempt is made to construct another
         * object of a given class with the same integer key as a
         * pre-existing instance of that subclass of Key.
         */
        protected Key(int privatekey) {
            this.privatekey = privatekey;
            recordIdentity(this);
        }

        /**
         * Returns true if the specified object is a valid value for
         * this key, false otherwise.
         */
        public abstract boolean isCompatibleValue(Object val);

        /**
         * Returns the private integer key that the subclass
         * instantiated this Key with.
         */
        protected final int intKey() {
            return privatekey;
        }

        /**
         * The hash code for all Key objects will be the same as the
         * system identity code of the object as defined by the
         * System.identityHashCode() method.
         */
        public final int hashCode() {
            return System.identityHashCode(this);
        }

        /**
         * The equals method for all Key objects will return the same
         * result as the equality operator '=='.
         */
        public final boolean equals(Object o) {
            return this == o;
        }
    }
}
