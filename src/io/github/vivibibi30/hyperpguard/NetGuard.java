package io.github.vivibibi30.hyperpguard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedModule;

/** 网络出口拦截：URL / InetAddress / Socket 三处。 */
final class NetGuard {

    static final class Rule {
        final String host;
        final String path;

        Rule(String host) {
            this(host, null);
        }

        Rule(String host, String path) {
            this.host = host;
            this.path = path;
        }
    }

    private static final ConcurrentHashMap<String, Long> SEEN = new ConcurrentHashMap<>();

    private final XposedModule m;

    NetGuard(XposedModule m) {
        this.m = m;
    }

    void install(ClassLoader cl, final String tag, final List<Rule> rules,
                 final boolean strictAll) {
        final boolean pathOn = Config.on(Config.K_PATH, true);
        final boolean sniffOn = Config.on(Config.K_SNIFF, false);

        try {
            Method open = URL.class.getDeclaredMethod("openConnection");
            m.hook(open).intercept(chain -> {
                URL u = (URL) chain.getThisObject();
                String host = u.getHost();
                if (host != null && check(tag, host, u.getPath(), rules, strictAll, pathOn, sniffOn)) {
                    throw new java.io.IOException("blocked by HyperPGuard: " + host);
                }
                return chain.proceed();
            });
        } catch (Throwable t) {
            Logs.i(tag + " url hook miss: " + t);
        }

        for (final String n : new String[]{"getByName", "getAllByName"}) {
            try {
                Method method = InetAddress.class.getDeclaredMethod(n, String.class);
                m.hook(method).intercept(chain -> {
                    String host = (String) chain.getArg(0);
                    if (host != null && check(tag, host, null, rules, strictAll, pathOn, false)) {
                        throw new java.net.UnknownHostException("blocked by HyperPGuard: " + host);
                    }
                    return chain.proceed();
                });
            } catch (Throwable t) {
                Logs.i(tag + " dns hook miss: " + t);
            }
        }

        try {
            for (Constructor<?> ctor : java.net.Socket.class.getDeclaredConstructors()) {
                m.hook(ctor).intercept(chain -> {
                    String host = pickHost(chain.getArgs());
                    if (host != null && check(tag, host, null, rules, strictAll, false, false)) {
                        throw new java.io.IOException("blocked by HyperPGuard: " + host);
                    }
                    return chain.proceed();
                });
            }
            for (Method method : java.net.Socket.class.getDeclaredMethods()) {
                if (!method.getName().equals("connect")) continue;
                if (method.getParameterCount() < 1) continue;
                if (!SocketAddress.class.isAssignableFrom(method.getParameterTypes()[0])) continue;
                m.hook(method).intercept(chain -> {
                    String host = null;
                    Object a0 = chain.getArg(0);
                    if (a0 instanceof InetSocketAddress) {
                        host = ((InetSocketAddress) a0).getHostString();
                    }
                    if (host != null && check(tag, host, null, rules, strictAll, false, false)) {
                        throw new java.io.IOException("blocked by HyperPGuard: " + host);
                    }
                    return chain.proceed();
                });
            }
        } catch (Throwable t) {
            Logs.i(tag + " socket hook miss: " + t);
        }
    }

    private static String pickHost(List<Object> args) {
        for (Object a : args) {
            if (a instanceof InetSocketAddress) return ((InetSocketAddress) a).getHostString();
            if (a instanceof String) return (String) a;
            if (a instanceof InetAddress) return ((InetAddress) a).getHostAddress();
        }
        return null;
    }

    private boolean check(String tag, String host, String path, List<Rule> rules,
                          boolean strictAll, boolean pathOn, boolean sniffOn) {
        String h = host.toLowerCase();

        if (strictAll) {
            if (!isLocal(h)) {
                Logs.i(tag + " X(strict) " + h);
                return true;
            }
            return false;
        }

        for (Rule r : rules) {
            if (!hostMatch(h, r.host)) continue;
            if (r.path == null) {
                Logs.i(tag + " X(host) " + h);
                return true;
            }
            if (pathOn && path != null && path.startsWith(r.path)) {
                Logs.i(tag + " X(host+path) " + h + path);
                return true;
            }
        }

        if (sniffOn) sniff(tag, h, path);
        return false;
    }

    private void sniff(String tag, String host, String path) {
        String key = tag + "|" + host;
        long now = System.currentTimeMillis();
        Long last = SEEN.get(key);
        if (last != null && now - last < 60000L) return;
        SEEN.put(key, now);
        Logs.i("? " + tag + " " + host + (path == null ? "" : path));
    }

    private static boolean hostMatch(String h, String suffix) {
        String s = suffix.toLowerCase();
        return h.equals(s) || h.endsWith("." + s);
    }

    /**
     * 是否内网地址。
     * 只有「合法的 IP 字面量」才按网段判断，避免 10.evil.com 这类域名靠前缀冒充内网，
     * 从而绕过整进程断网的严格模式。
     */
    private static boolean isLocal(String h) {
        if (h == null) return false;
        String s = h.trim().toLowerCase();
        if (s.isEmpty()) return false;
        if (s.equals("localhost") || s.endsWith(".local")) return true;

        byte[] ip = parseIpLiteral(s);
        if (ip == null) return false;

        if (ip.length == 4) {
            int a = ip[0] & 0xff;
            int b = ip[1] & 0xff;
            return a == 10                                  // 10.0.0.0/8
                    || a == 127                             // 127.0.0.0/8
                    || (a == 192 && b == 168)               // 192.168.0.0/16
                    || (a == 172 && b >= 16 && b <= 31)     // 172.16.0.0/12
                    || (a == 169 && b == 254);              // 169.254.0.0/16
        }

        // IPv6
        int b0 = ip[0] & 0xff;
        int b1 = ip[1] & 0xff;
        if ((b0 & 0xfe) == 0xfc) return true;               // fc00::/7
        if (b0 == 0xfe && (b1 & 0xc0) == 0x80) return true; // fe80::/10
        boolean loopback = ip[15] == 1;
        for (int i = 0; i < 15 && loopback; i++) if (ip[i] != 0) loopback = false;
        return loopback;                                    // ::1
    }

    /** 只解析 IP 字面量，不做任何 DNS 查询；不是合法 IP 就返回 null */
    private static byte[] parseIpLiteral(String s) {
        try {
            if (s.indexOf(':') >= 0) {
                java.net.InetAddress ia = java.net.InetAddress.getByName(s);
                return ia instanceof java.net.Inet6Address ? ia.getAddress() : null;
            }
            String[] p = s.split("\\.");
            if (p.length != 4) return null;
            byte[] out = new byte[4];
            for (int i = 0; i < 4; i++) {
                if (p[i].isEmpty() || p[i].length() > 3) return null;
                int v = Integer.parseInt(p[i]);
                if (v < 0 || v > 255) return null;
                out[i] = (byte) v;
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }
}
