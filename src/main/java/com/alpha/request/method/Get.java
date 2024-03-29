package com.alpha.request.method;

import com.alpha.request.Cookie;
import com.alpha.request.HttpRequest;
import com.alpha.response.ContentType;
import com.alpha.response.HttpResponse;
import com.alpha.response.Status;
import com.alpha.server.Constants;
import com.alpha.utils.HTMLMaker;
import com.alpha.utils.HttpRequestParser;

import java.io.*;
import java.util.ArrayList;


public class Get implements HttpMethod, Constants {
    private HttpRequest request;
    private HttpResponse response;


    public Get(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }


    @Override
    public void execute() throws IOException {
        Status statusCode = getStatusCode(request);

        switch (statusCode) {
            case _404:
                response.setStatusCode(Status._404);
                String body = HTMLMaker._404();
                response.setContentLength(body.getBytes().length);
                response.setBody(body);
                response.send();
                break;
            case _403:
                response.setStatusCode(Status._403);
                body = HTMLMaker._403();
                response.setContentLength(body.getBytes().length);
                response.setBody(body);
                response.send();
                break;
            case _301:
            case _302:
                response.redirect(request.getPath() + "/");
                break;
            case _206:
                String range = request.getHeader("Range");
                File file = new File(HOME, request.getPath());
                long fsize = file.length();
                long sendSize;


                // Only use first range here
                if (range != null && range.contains("bytes")) {
                    ArrayList<HttpRequestParser.Range> arrayList = HttpRequestParser.parseRange(range);
                    HttpRequestParser.Range range1 = arrayList.remove(0);

                    long start = range1.start;
                    long end = range1.end;
                    sendSize = fsize - start;

                    if (range1.start != -1 && range1.end != -1 && range1.start > range1.end) {
                        response.setContentLength(0);
                        response.setStatusCode(Status._416);
                        response.addHeader("Content-Range", String.format("bytes */%d", fsize));
                        response.sendHeader();
                        break;
                    }

                    if (range1.end == -1) {
                        if (fsize > start + sendSize) {
                            end = start + sendSize - 1;
                        } else {
                            end = fsize - 1;
                        }
                    } else if (end > fsize) {
                        end = fsize - 1;
                    }

                    long len = end - start + 1;
                    response.setStatusCode(Status._206);
                    response.addHeader("Content-Range", String.format("bytes %d-%d/%d", start, end, fsize));
                    response.guessContentType(request.getPath());
                    response.setContentLength(len);
                    response.sendHeader();

                    byte[] b = new byte[BUFFER_SIZE];
                    int c = 0;
                    long read = 0;

                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    	bis.skip(start);

                        while (read < len) {
                            c = bis.read(b, 0, read + b.length > len ? (int) (len - read) : b.length);
                            if (c == -1)
                                break;

                            read += c;
                            response.sendBody(b, 0, c);
                        }
                    }

                }
                break;
            case _200:
                File localFile = new File(HOME, request.getPath());
                // html
                if (localFile.isDirectory()) {

                    // check parameter showHidden, then redirect
                    String showHidden = request.getParameter("showHidden");
                    if (showHidden != null) {
                        if (showHidden.equals("true")) {
                            Cookie cookie = new Cookie("showHidden", "true");
                            cookie.setMaxAge(60 * 60 * 2);
                            cookie.setPath("/");
                            response.addCookie(cookie);
                            response.redirect(request.getPath());
                        } else {
                            Cookie cookie = new Cookie("showHidden", "false");
                            cookie.setMaxAge(60 * 60 * 2);
                            cookie.setPath("/");
                            response.addCookie(cookie);
                            response.redirect(request.getPath());
                        }
                    }


                    String html = mappingLocal(request.getPath());

                    // check cookie
                    for (Cookie cookie : request.getCookies()) {
                        if (cookie.getName().equals("showHidden")) {
                            String show = cookie.getValue();

                            if (show.equals("true")) {
                                html = mappingLocal(request.getPath(), true);
                            } else {
                                html = mappingLocal(request.getPath(), false);
                            }
                        }
                    }

                    response.setStatusCode(Status._200);
//                    response.enableChunked();
                    response.setContentLength(html.getBytes().length);
                    response.setContentType(ContentType.HTML);
                    response.sendHeader();

//                    response.sendChunkedFin(html.getBytes());
                    response.sendBody(html.getBytes());
                } else {   // file

                    // check parameter 'download'
                    String download = request.getParameter("download");
                    if (download != null) {
                        response.addHeader("Content-Disposition", "attachment");
                    }

                    response.setStatusCode(Status._200);
                    response.guessContentType(request.getPath());
                    response.setContentLength(localFile.length());
                    response.sendHeader();

                    byte[] buffer = new byte[BUFFER_SIZE];
                    int n = 0;
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localFile));

                    while ((n = bis.read(buffer)) != -1) {
                        response.sendBody(buffer, 0, n);
                    }

                    bis.close();
                }
                break;
            default:
                throw new IOException("unhandled status code");
        }
    }


    private Status getStatusCode(HttpRequest request) {
        String path = request.getPath();

        File file = new File(HOME, path);

        String range = request.getHeader("Range");
        if (range != null && range.contains("bytes")) {
            return Status._206;
        }

        if (!file.exists()) {
            return Status._404;
        }

        if (!file.canRead()) {
            return Status._403;
        }

        if (file.isDirectory()) {
            if (!path.endsWith("/")) {
                return Status._302;
            } else {
                return Status._200;
            }
        } else {
            // is file
            return Status._200;
        }
    }


    private static String mappingLocal(String path) throws UnsupportedEncodingException {
        return mappingLocal(path, false);
    }


    private static String mappingLocal(String path, boolean showHidden) throws UnsupportedEncodingException {
        return HTMLMaker.makeIndex(path, showHidden);
    }

}
