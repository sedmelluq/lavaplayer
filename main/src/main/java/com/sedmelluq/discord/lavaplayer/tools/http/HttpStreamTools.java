package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.io.InputStream;

public class HttpStreamTools {
    public static InputStream streamContent(HttpInterface httpInterface, HttpUriRequest request) {
        CloseableHttpResponse response = null;
        boolean success = false;

        try {
            response = httpInterface.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                throw new IOException("Invalid status code from " + request.getURI() + " URL: " + statusCode);
            }

            success = true;
            return response.getEntity().getContent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (response != null && !success) {
                ExceptionTools.closeWithWarnings(response);
            }
        }
    }
}
