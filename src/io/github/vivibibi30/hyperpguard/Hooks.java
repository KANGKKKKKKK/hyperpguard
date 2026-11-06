package io.github.vivibibi30.hyperpguard;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/** hook 安装的公共工具。 */
final class Hooks {

    interface Interceptor {
        Object intercept(XposedInterface.Chain chain) throws Throwable;
    }

    private Hooks() {
    }

    /** 把方法整体替换为固定返回值。 */
    static void ret(XposedModule m, ClassLoader cl, String cls, String name,
                    Class<?>[] types, Object value) {
        try {
            Class<?> c = Class.forName(cls, false, cl);
            Method method = c.getDeclaredMethod(name, types);
            m.hook(method).intercept(chain -> value);
            Logs.i("hook ok " + cls + "#" + name);
        } catch (Throwable t) {
            Logs.i("hook miss " + cls + "#" + name + " -> " + t);
        }
    }

    static void allByName(XposedModule m, Class<?> c, String name, Interceptor interceptor) {
        for (Method method : c.getDeclaredMethods()) {
            if (!method.getName().equals(name)) continue;
            m.hook(method).intercept(interceptor::intercept);
        }
    }
}
