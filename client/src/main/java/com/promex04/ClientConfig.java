package com.promex04;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientConfig {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final int DEFAULT_HTTP_PORT = 8080;

    private static final Properties props = new Properties();

    static {
        try (InputStream is = ClientConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ignored) {
        }
    }

    public static String getDefaultHost() {
        String env = System.getenv("LTM_SERVER_HOST");
        if (env == null || env.isBlank()) env = System.getenv("SERVER_HOST");
        if (env != null && !env.isBlank()) return env.trim();

        String sys = System.getProperty("client.server.host");
        if (sys != null && !sys.isBlank()) return sys.trim();

        String file = props.getProperty("client.server.host");
        if (file != null && !file.isBlank()) return file.trim();

        return DEFAULT_HOST;
    }

    public static int getDefaultPort() {
        String env = System.getenv("LTM_SERVER_PORT");
        if (env == null || env.isBlank()) env = System.getenv("SERVER_PORT");
        if (env != null && !env.isBlank()) {
            try { return Integer.parseInt(env.trim()); } catch (Exception ignored) {}
        }

        String sys = System.getProperty("client.server.port");
        if (sys != null && !sys.isBlank()) {
            try { return Integer.parseInt(sys.trim()); } catch (Exception ignored) {}
        }

        String file = props.getProperty("client.server.port");
        if (file != null && !file.isBlank()) {
            try { return Integer.parseInt(file.trim()); } catch (Exception ignored) {}
        }

        return DEFAULT_PORT;
    }

    public static int getDefaultHttpPort() {
        String env = System.getenv("LTM_HTTP_PORT");
        if (env == null || env.isBlank()) env = System.getenv("SERVER_HTTP_PORT");
        if (env != null && !env.isBlank()) {
            try { return Integer.parseInt(env.trim()); } catch (Exception ignored) {}
        }

        String sys = System.getProperty("client.server.http.port");
        if (sys != null && !sys.isBlank()) {
            try { return Integer.parseInt(sys.trim()); } catch (Exception ignored) {}
        }

        String file = props.getProperty("client.server.http.port");
        if (file != null && !file.isBlank()) {
            try { return Integer.parseInt(file.trim()); } catch (Exception ignored) {}
        }

        return DEFAULT_HTTP_PORT;
    }

    public static String getHttpBaseUrl() {
        String host = getDefaultHost();
        int port = getDefaultHttpPort();
        return "http://" + host + ":" + port;
    }

    public static boolean showServerFields() {
        String env = System.getenv("LTM_SHOW_SERVER_FIELDS");
        if (env != null && !env.isBlank()) {
            return env.equalsIgnoreCase("true") || env.equals("1") || env.equalsIgnoreCase("yes");
        }

        String sys = System.getProperty("client.showServerFields");
        if (sys != null && !sys.isBlank()) {
            return sys.equalsIgnoreCase("true") || sys.equals("1") || sys.equalsIgnoreCase("yes");
        }

        String file = props.getProperty("client.showServerFields");
        if (file != null && !file.isBlank()) {
            return file.equalsIgnoreCase("true") || file.equals("1") || file.equalsIgnoreCase("yes");
        }

        // Mặc định: hiển thị các ô nhập máy chủ
        return true;
    }
}
