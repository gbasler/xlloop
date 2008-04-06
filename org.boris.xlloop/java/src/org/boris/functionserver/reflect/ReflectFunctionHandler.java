package org.boris.functionserver.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.boris.functionserver.Function;
import org.boris.functionserver.FunctionHandler;
import org.boris.functionserver.RequestException;
import org.boris.functionserver.util.FunctionInformation;
import org.boris.functionserver.util.VariantObjectConverter;
import org.boris.variantcodec.VTCollection;
import org.boris.variantcodec.Variant;

public class ReflectFunctionHandler implements FunctionHandler
{
    private Map methods = new HashMap();
    private VariantObjectConverter converter = new VariantObjectConverter();

    public Set getFunctionList() {
        return methods.keySet();
    }

    public void addMethod(Object instance, Method m) {
        addMethod(m.getName(), instance.getClass(), instance, m);
    }

    public void addMethod(String name, Class c, Object instance, Method m) {
        if(!m.getDeclaringClass().equals(c)) return;
        Function f = (Function) methods.get(name);
        if (f instanceof InstanceMethod) {
            OverloadedMethod om = new OverloadedMethod();
            om.add((InstanceMethod) f);
            om.add(new InstanceMethod(c, instance, m, converter));
            methods.put(name, om);
        } else if (f instanceof OverloadedMethod) {
            ((OverloadedMethod) f).add(new InstanceMethod(c, instance, m,
                    converter));
        } else {
            methods.put(name, new InstanceMethod(c, instance, m, converter));
        }
    }

    public void addMethods(String namespace, Class c) {
        Method[] m = c.getMethods();
        for (int i = 0; i < m.length; i++) {
            if (Modifier.isStatic(m[i].getModifiers())) {
                if (namespace == null) {
                    addMethod(null, m[i]);
                } else {
                    addMethod(namespace + m[i].getName(), c, null, m[i]);
                }
            }
        }
    }

    public void addMethods(String namespace, Object instance) {
        Method[] m = instance.getClass().getMethods();
        for (int i = 0; i < m.length; i++) {
            if ((instance == null && Modifier.isStatic(m[i].getModifiers())) ||
                    instance != null) {
                if (namespace == null) {
                    addMethod(instance, m[i]);
                } else {
                    addMethod(namespace + m[i].getName(), instance.getClass(), instance, m[i]);
                }
            }
        }
    }

    public Variant execute(String name, VTCollection args)
            throws RequestException {
        Function f = (Function) methods.get(name);
        if (f == null) {
            throw new RequestException("#Unknown method: " + name);
        }
        return f.execute(args);
    }

    public boolean hasFunction(String name) {
        return methods.containsKey(name);
    }
    
    public VTCollection getFunctions() {
        VTCollection functions = new VTCollection();
        for(Iterator i = methods.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            Function f = (Function) methods.get(key);
            FunctionInformation fi = new FunctionInformation(key);
            if(f instanceof InstanceMethod) {
                try {
                    InstanceMethod im = (InstanceMethod) f;
                    ParameterNameExtractor pne = new ParameterNameExtractor(im.clazz);
                    String[] names = pne.getParameterNames(im.method);
                    for(int j = 0; j < names.length; j++) {
                        fi.addArgument(names[j], im.args[j].getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            functions.add(fi.encode());
        }
        System.out.println(functions);
        return functions;
    }
}
