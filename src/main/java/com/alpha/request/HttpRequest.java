package com.alpha.request;

import com.alpha.utils.HttpRequestParser;
import com.alpha.utils.SingleFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpRequest {
    private String method;
    private String path;    // do not contain query parameters
    private String fullPath;     // maybe contain query parameters
    private String version;
    private Map<String, String> headers;    // store header name and value
    private Map<String, String> queryParameters;
    private List<Cookie> cookies;
    private InputStream in;


    public HttpRequest(InputStream in) {
        this.headers = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.cookies = new ArrayList<>();
        this.in = new BufferedInputStream(in);
    }


    public boolean parse() throws IOException {
        byte[] header = HttpRequestParser.parse(in);

        String requestHeaders = new String(header, StandardCharsets.UTF_8);
        StringTokenizer reqTok = new StringTokenizer(requestHeaders, "\r\n");

        // parse initial line
        String initialLine = reqTok.nextToken();
        System.out.println(URLDecoder.decode(initialLine, StandardCharsets.UTF_8));
        StringTokenizer initTok = new StringTokenizer(initialLine);
        String[] components = new String[3];
        for (int i = 0; i < components.length; i++) {
            if (initTok.hasMoreTokens()) {
                components[i] = initTok.nextToken();
            } else {
                return false;
            }
        }
        method = components[0];
        path = fullPath = URLDecoder.decode(components[1], StandardCharsets.UTF_8);
        version = components[2];

        // parse request headers
        while (reqTok.hasMoreTokens()) {
            String headerLine = reqTok.nextToken();
//            System.out.println(headerLine);
            if (headerLine.length() == 0) {
                break;
            }

            int separator = headerLine.indexOf(":");
            // invalid header
            if (separator == -1) {
                return false;
            }

            String headerName = headerLine.substring(0, separator);
            String headerValue = headerLine.substring(separator + 2);

            if ("Cookie".equals(headerName)) {
                parseCookies(headerValue);
                continue;
            }

            headers.put(headerName, headerValue);
        }

        // parse request query parameters
        if (!fullPath.contains("?")) {
            path = fullPath;
        } else {
            path = fullPath.substring(0, fullPath.indexOf("?"));
            parseQueryParameters(fullPath.substring(fullPath.indexOf("?") + 1));
        }

        return true;
    }


    private void parseQueryParameters(String queryString) {
        for (String parameter : queryString.split("&")) {
            int separator = parameter.indexOf('=');
            if (separator > -1) {
                queryParameters.put(parameter.substring(0, separator),
                        parameter.substring(separator + 1));
            } else {
                queryParameters.put(parameter, "");
            }
        }
    }


    private void parseCookies(String cookieString) {
        String[] cookiePairs = cookieString.split("; ");
        for (String s : cookiePairs) {
            int equal = s.indexOf("=");
            Cookie cookie = new Cookie(s.substring(0, equal), s.substring(equal + 1));
            cookies.add(cookie);
        }
    }


    // while http method is post
    public SingleFile parsePost() throws IOException {
        return new SingleFile(in, headers);
    }


    public List<Cookie> getCookies() {
        return cookies;
    }


    // TODO support multi-value headers
    public String getHeader(String headerName) {
        return headers.get(headerName);
    }


    public Map<String, String> getHeaders() {
        return headers;
    }


    public String getParameter(String paramName) {
        return queryParameters.get(paramName);
    }


    public Map<String, String> getParameters() {
        return queryParameters;
    }


    public String getMethod() {
        return method;
    }


    public String getFullPath() {
        return fullPath;
    }


    public String getPath() {
        return path;
    }


    public String getVersion() {
        return version;
    }


    @Override
    public String toString() {
        return "Request{" +
                method + fullPath + version + '\n' +
                "headers=" + headers + "\n" +
                "cookies=" + cookies + "\n" +
                '}';
    }
}
