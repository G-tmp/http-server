package com.alpha.request.method;

import com.alpha.response.HttpResponse;
import com.alpha.server.Constants;
import com.alpha.utils.SingleFile;
import com.alpha.request.HttpRequest;
import com.alpha.response.ContentType;
import com.alpha.response.Status;

import java.io.File;
import java.io.IOException;

public class Post implements HttpMethod , Constants {
    private HttpRequest request;
    private HttpResponse response;


    public Post(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void execute() throws IOException {
        SingleFile singleFile = request.parsePost();

        boolean success = singleFile.save(new File(HOME, request.getPath()).getPath());

        String body = null;
        if (success) {
            if (singleFile.isOverwrite())
                body = "<h1>" + "Overwrite " + singleFile.getFilename() + "</h1>";
            else
                body = "<h1>" + "Create " + singleFile.getFilename() + "</h1>";
        }else {
            body = "<h1>" + "Failed" + "</h1>";
        }

        response.setStatusCode(Status._200);
        response.enableChunked();
        response.setContentType(ContentType.HTML);
        response.sendHeader();
        assert body != null;
        response.sendChunkedFin(body.getBytes());
    }
}
