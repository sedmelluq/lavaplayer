package com.sedmelluq.lavaplayer.core.http;

import com.sedmelluq.lavaplayer.core.tools.exception.ExceptionTools;
import com.sedmelluq.lavaplayer.core.http.HttpClientTools;
import com.sedmelluq.lavaplayer.core.http.HttpInterface;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

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
