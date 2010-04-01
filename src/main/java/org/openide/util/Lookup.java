/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openide.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Mock of the real org.openide.util.Lookup which delegates to JDK 6
 * ServiceLoader.
 * 
 * @author Tim Boudreau
 */
public abstract class Lookup {

    public abstract <T> T lookup(Class<T> type);

    public abstract <T> Collection<? extends T> lookupAll(Class<T> type);

    public static Lookup getDefault() {
        return new MyFakeDefaultLookup();
    }

    private static final class MyFakeDefaultLookup extends Lookup {

        @Override
        public <T> T lookup(Class<T> type) {
            System.out.println("my lookup=" + type);
            Iterator<T> ldr;
            try {
                ldr = load(type).iterator();
                return ldr.hasNext() ? ldr.next() : null;
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        @Override
        public <T> Collection<? extends T> lookupAll(Class<T> type) {
            System.out.println("my lookup=" + type);
            Set<T> result = new HashSet<T>();
            try {
                for (T t : load(type)) {
                    result.add(t);
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            System.out.println("res=" + result.size());
            return result;
        }
    }

    public <S> Iterable<S> load(Class<S> ifc) throws Exception {
        ClassLoader ldr = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> e = ldr.getResources("META-INF/services/" + ifc.getName());
        Collection<S> services = new ArrayList<S>();
        while (e.hasMoreElements()) {
            URL url = e.nextElement();
            InputStream is = url.openStream();
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    int comment = line.indexOf('#');
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    String name = line.trim();
                    if (name.length() == 0) {
                        continue;
                    }
                    Class<?> clz = Class.forName(name, true, ldr);
                    Class<? extends S> impl = clz.asSubclass(ifc);
                    Constructor<? extends S> ctor = impl.getConstructor();
                    S svc = ctor.newInstance();
                    services.add(svc);
                }
            } finally {
                is.close();
            }
        }
        return services;
    }
}
