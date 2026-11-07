package io.github.vivibibi30.hyperpguard;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/** hook 安装的公共工具。 */
final class Hooks {

    interface Interceptor {
        Object intercept(XposedInterface.Chain chain) throws Throwable;
    }

    interface MethodFilter {
        boolean test(Method method);
    }

    private Hooks() {
    }

    /** 把方法整体替换为固定返回值。返回是否挂上。 */
    static boolean ret(XposedModule m, ClassLoader cl, String cls, String name,
                       Class<?>[] types, Object value) {
        try {
            Class<?> c = Class.forName(cls, false, cl);
            Method method = c.getDeclaredMethod(name, types);
            m.hook(method).intercept(chain -> value);
            Logs.i("hook ok " + cls + "#" + name);
            return true;
        } catch (Throwable t) {
            Logs.i("hook miss " + cls + "#" + name + " -> " + t);
            return false;
        }
    }

    static void allByName(XposedModule m, Class<?> c, String name, Interceptor interceptor) {
        for (Method method : c.getDeclaredMethods()) {
            if (!method.getName().equals(name)) continue;
            m.hook(method).intercept(interceptor::intercept);
        }
    }

    /**
     * 遍历进程内各 dex 的类名，按 filter 过滤，最多返回 limit 个。
     * 类名随版本变化时，靠这个替代写死的混淆类名。
     */
    static java.util.List<String> classNames(ClassLoader cl,
                                             java.util.function.Predicate<String> filter,
                                             int limit) {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            Class<?> bdcl = Class.forName("dalvik.system.BaseDexClassLoader", false, cl);
            java.lang.reflect.Field fPathList = bdcl.getDeclaredField("pathList");
            fPathList.setAccessible(true);
            Object pathList = fPathList.get(cl);
            if (pathList == null) return out;
            java.lang.reflect.Field fElems = pathList.getClass().getDeclaredField("dexElements");
            fElems.setAccessible(true);
            Object[] elems = (Object[]) fElems.get(pathList);
            for (Object el : elems) {
                if (el == null) continue;
                try {
                    java.lang.reflect.Field fDex = el.getClass().getDeclaredField("dexFile");
                    fDex.setAccessible(true);
                    Object dex = fDex.get(el);
                    if (!(dex instanceof dalvik.system.DexFile)) continue;
                    java.util.Enumeration<String> en = ((dalvik.system.DexFile) dex).entries();
                    while (en.hasMoreElements() && out.size() < limit) {
                        String n = en.nextElement();
                        if (filter == null || filter.test(n)) out.add(n);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            Logs.i("class scan fail: " + t);
        }
        return out;
    }

    /** 在候选类里按谓词挑出方法并 hook，返回挂上的数量。 */
    static int hookBySignature(XposedModule m, java.util.List<String> classNames,
                               ClassLoader cl, MethodFilter filter, Interceptor interceptor) {
        int n = 0;
        for (String name : classNames) {
            Class<?> c;
            try {
                c = Class.forName(name, false, cl);
            } catch (Throwable t) {
                continue;
            }
            for (Method method : c.getDeclaredMethods()) {
                if (!filter.test(method)) continue;
                try {
                    m.hook(method).intercept(interceptor::intercept);
                    n++;
                } catch (Throwable ignored) {
                }
            }
        }
        return n;
    }
}
