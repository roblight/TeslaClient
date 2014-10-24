/*
 * Utils.java - Copyright(c) 2013 Joe Pasqua
 * Provided under the MIT License. See the LICENSE file for details.
 * Created: Jul 8, 2013
 */

package org.noroomattheinn.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.noroomattheinn.tesla.Tesla;

/**
 * Utils
 *
 * @author Joe Pasqua <joe at NoRoomAtTheInn dot org>
 */

public class Utils {

    // Package Class Variables
    
    static final Logger logger = Logger.getLogger(Utils.class.getName());

    //
    // Public Class Methods
    //

    /**
     * Construct a new HashMap with a list of key/value pairs as arguments.
     * The arguments are interspersed: k,v,k,v,k,v,...
     * There must be an even number of arguments and each key must be of
     * type K and each value must be of type V.
     * 
     * @param <K>   The type of the keys
     * @param <V>   The type of the values
     * @param kv    An array of (key,value) pairs
     * @return      An initialized HashMap
     */
    public static <K,V>HashMap<K,V> newHashMap(Object... kv) {
        int length = kv.length;
        if (length % 2 != 0)
            throw new IllegalArgumentException("Mismatched keys and values");
        HashMap<K,V> m = new HashMap<>();
        for (int i = 0; i < length;) {
            K key   = cast(kv[i++]);
            V value = cast(kv[i++]);
            m.put(key, value);
        }
        return m;
    }
    
    
    /**
     * Construct a new HashMap using the elements of an Enumeration as the keys
     * and an array of objects of type V as the values. The array must be the same
     * length as the number of elements in the enumeration.
     * @param <E>   An Enumeration Type
     * @param <V>   An arbitrary value type
     * @param keys  An array of enums such as the one that would be returned
     *              by SomeEnum.values();
     * @param values    An array of values to go with the enum keys
     * @return
     */
    public static <E extends Enum<E>,V> HashMap<E,V> newHashMap(E[] keys, V[] values) {
        int nKeys = keys.length;
        if (nKeys != values.length)
            throw new IllegalArgumentException("Mismatched keys and values");
        HashMap<E,V> m = new HashMap<>();
        for (int i = 0; i < nKeys; i++) {
            m.put(keys[i], values[i]);
        }
        return m;
    }
    
    public static <K,V> void fillMap(Map<K,V> m, Object... kv) {
        int length = kv.length;
        if (length % 2 != 0)
            throw new IllegalArgumentException("Mismatched keys and values");
        for (int i = 0; i < length;) {
            K key   = cast(kv[i++]);
            V value = cast(kv[i++]);
            m.put(key, value);
        }
    }
    
    /*
     * Returns a member of an Enumeration corresponding to a String value. This
     * is based on <code>Enum.valueof()</code>, but is wrapped with a try block
     * to deal with the case where the String value does not correspond to a
     * value of the Enum. In this case the Enum value "Unknown" is used. So,
     * to use this method safely, the Enum type must have Unknown as an option.
     * <P>
     * This code is a little weird because it is both parameterized by the Enum
     * Type and the class is alo passed in. This is required to handle some
     * oddities of dealing with Generic Enums.
     * 
     * @param eClass    The class of the Enum for which we want an instance
     * @param val       The String that should correspond to an Enum value.
     * @return          An instance of the specified Enum type corresponding
     *                  to the specified prefix
     */
    public static <T extends Enum<T>> T stringToEnum(Class<T> eClass, String val) {
        try {
            return Enum.valueOf(eClass, val);
        } catch (Exception e) {
            logger.log(Level.FINEST, "Problem converting " + val, e);
            return Enum.valueOf(eClass, "Unknown");
        }
    }
    
    public enum UnitType {Metric, Imperial};
    public static double cToF(double temp) { return temp * 9.0/5.0 + 32; }
    public static double fToC(double temp) { return (temp-32) * 5.0/9.0; }
    public static String yesNo(boolean b) { return b ? "Yes" : "No"; }
    public static final double KilometersPerMile = 1.60934;
    public static double kToM(double k) { return k/KilometersPerMile; }
    public static double mToK(double m) { return m * KilometersPerMile; }

    @SuppressWarnings("unchecked")
    public static <E> E cast(Object obj) {
        return (E) obj;
    }
    
    public static <T extends Comparable<T>> T clamp(T val, T min, T max) {
        if (val.compareTo(min) < 0) {
            return min;
        } else if (val.compareTo(max) > 0) {
            return max;
        } else {
            return val;
        }
    }

    public static double percentChange(double oldValue, double newValue) {
        if (oldValue == 0) return 1.0;
        return Math.abs((oldValue - newValue)/oldValue);
    }
    
    public interface Predicate { boolean eval(); }
    private static Predicate alwaysFalse =
            new Predicate() { @Override public boolean eval() { return false; } };
    
    public static void sleep(long timeInMillis, Predicate p) {
        long initialTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - initialTime < timeInMillis) {
            if (p.eval()) { Tesla.logger.info("RETURNING EARLY!"); return; }
            try { Thread.sleep(500); } catch (InterruptedException ex) { return; }
        }
    }
    
    public static void sleep(long timeInMillis) { sleep(timeInMillis, alwaysFalse); }
    
    public static void yieldFor(long timeInMillis) {
        try { Thread.sleep(timeInMillis); } catch (InterruptedException ex) { }
    }
    
    public static int compareVersions(String versionA, String versionB) {
        String[] partsOfA = versionA.split("\\.");
        String[] partsOfB = versionB.split("\\.");
        int shortest = Math.min(partsOfA.length, partsOfB.length);

        int i = 0;
        while (i < shortest && partsOfA[i].equals(partsOfB[i])) { i++; }

        if (i < shortest)
            return Integer.valueOf(partsOfA[i]) - Integer.valueOf(partsOfB[i]);

        return partsOfA.length - partsOfB.length;
    }

    public interface Callback<P,R> {
        public R call(P parameter);
    }
    
    public static String toB64(byte[] bytes) {
        return javax.xml.bind.DatatypeConverter.printBase64Binary(bytes);
    }
    
    public static byte[] fromB64(String s) {
        return javax.xml.bind.DatatypeConverter.parseBase64Binary(s);
    }
    
    public static String decodeB64(String s) {
        if (s == null) return null;
        try {
            return new String(fromB64(s), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static List<String> getJVMArgs() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMxBean.getInputArguments();
    }
    
    /**
     * Lock a global resource between instances of Java running in different
     * processes. This can be used to ensure that only one instance of an
     * application is running. The lock is obtained by locking a file in
     * the file system that is agreed upon by all parties involved in the locking
     * process.
     * @param lockName  The file to lock
     * @param folder    The folder containing the file to lock
     * @return          true if this process obtained the lock
     *                  false if the lock is already held by another process
     */
    public static boolean obtainLock(String lockName, File folder) {
        File lockFile = new File(folder, lockName);
        try {
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            FileLock instanceLock = raf.getChannel().tryLock();
            return instanceLock != null;
        } catch (IOException ex ) {
            Tesla.logger.severe(ex.getMessage());
            return false;
        }
    }
}
