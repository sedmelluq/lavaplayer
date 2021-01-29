package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.http.ExtendedHttpClientBuilder;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLException;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Tools for working with HttpClient
 */
public class HttpClientTools {
  private static final Logger log = LoggerFactory.getLogger(HttpClientTools.class);

  public static final RequestConfig DEFAULT_REQUEST_CONFIG = RequestConfig.custom()
      .setConnectTimeout(3000)
      .setCookieSpec(CookieSpecs.STANDARD)
      .build();

  private static final RequestConfig NO_COOKIES_REQUEST_CONFIG = RequestConfig.custom()
      .setConnectTimeout(3000)
      .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
      .build();

  /**
   * @return An HttpClientBuilder which uses the same cookie store for all clients
   */
  public static HttpClientBuilder createSharedCookiesHttpBuilder() {
    return createHttpBuilder(DEFAULT_REQUEST_CONFIG);
  }

  /**
   * @return Default HTTP interface manager with thread-local context
   */
  public static HttpInterfaceManager createDefaultThreadLocalManager() {
    return new ThreadLocalHttpInterfaceManager(createSharedCookiesHttpBuilder(), DEFAULT_REQUEST_CONFIG);
  }

  /**
   * @return HTTP interface manager with thread-local context, ignores cookies
   */
  public static HttpInterfaceManager createCookielessThreadLocalManager() {
    return new ThreadLocalHttpInterfaceManager(createHttpBuilder(NO_COOKIES_REQUEST_CONFIG), NO_COOKIES_REQUEST_CONFIG);
  }

  private static HttpClientBuilder createHttpBuilder(RequestConfig requestConfig) {
    CookieStore cookieStore = new BasicCookieStore();

    return new ExtendedHttpClientBuilder()
        .setDefaultCookieStore(cookieStore)
        .setRetryHandler(NoResponseRetryHandler.RETRY_INSTANCE)
        .setDefaultRequestConfig(requestConfig);
  }

  /**
   * A redirect strategy which does not follow any redirects.
   */
  public static class NoRedirectsStrategy implements RedirectStrategy {
    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
      return false;
    }

    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) {
      return null;
    }
  }

  /**
   * @param requestUrl URL of the original request.
   * @param response Response object.
   * @return A redirect location if the status code indicates a redirect and the Location header is present.
   */
  public static String getRedirectLocation(String requestUrl, HttpResponse response) {
    if (!isRedirectStatus(response.getStatusLine().getStatusCode())) {
      return null;
    }

    Header header = response.getFirstHeader("Location");
    if (header == null) {
      return null;
    }

    String location = header.getValue();

    try {
      return new URI(requestUrl).resolve(location).toString();
    } catch (URISyntaxException e) {
      log.debug("Failed to parse URI.", e);
      return location;
    }
  }

  private static boolean isRedirectStatus(int statusCode) {
    switch (statusCode) {
      case HttpStatus.SC_MOVED_PERMANENTLY:
      case HttpStatus.SC_MOVED_TEMPORARILY:
      case HttpStatus.SC_SEE_OTHER:
      case HttpStatus.SC_TEMPORARY_REDIRECT:
        return true;
      default:
        return false;
    }
  }

  /**
   * @param statusCode The status code of a response.
   * @return True if this status code indicates a success with a response body
   */
  public static boolean isSuccessWithContent(int statusCode) {
    return statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT ||
        statusCode == HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION;
  }

  /**
   * @param response The response.
   * @param context Additional string to include in exception message.
   * @return True if this status code indicates a success with a response body
   */
  public static void assertSuccessWithContent(HttpResponse response, String context) throws IOException {
    int statusCode = response.getStatusLine().getStatusCode();

    if (!isSuccessWithContent(statusCode)) {
      throw new IOException("Invalid status code for " + context +  ": " + statusCode);
    }
  }

  public static String getRawContentType(HttpResponse response) {
    Header header = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
    return header != null ? header.getValue() : null;
  }

  public static boolean hasJsonContentType(HttpResponse response) {
    String contentType = getRawContentType(response);
    return contentType != null && contentType.startsWith(ContentType.APPLICATION_JSON.getMimeType());
  }

  public static void assertJsonContentType(HttpResponse response) throws IOException {
    if (!HttpClientTools.hasJsonContentType(response)) {
      throw ExceptionTools.throwWithDebugInfo(
          log,
          null,
          "Expected JSON content type, got " + HttpClientTools.getRawContentType(response),
          "responseContent",
          EntityUtils.toString(response.getEntity())
      );
    }
  }

  /**
   * @param exception Exception to check.
   * @return True if retrying to connect after receiving this exception is likely to succeed.
   */
  public static boolean isRetriableNetworkException(Throwable exception) {
    return isConnectionResetException(exception) ||
        isSocketTimeoutException(exception) ||
        isIncorrectSslShutdownException(exception) ||
        isPrematureEndException(exception) ||
        isRetriableConscryptException(exception) ||
        isRetriableNestedSslException(exception);
  }

  public static boolean isConnectionResetException(Throwable exception) {
    return (exception instanceof SocketException || exception instanceof SSLException)
        && "Connection reset".equals(exception.getMessage());
  }

  private static boolean isSocketTimeoutException(Throwable exception) {
    return (exception instanceof SocketTimeoutException || exception instanceof SSLException)
        && "Read timed out".equals(exception.getMessage());
  }

  private static boolean isIncorrectSslShutdownException(Throwable exception) {
    return exception instanceof SSLException && "SSL peer shut down incorrectly".equals(exception.getMessage());
  }

  private static boolean isPrematureEndException(Throwable exception) {
    return exception instanceof ConnectionClosedException && exception.getMessage() != null &&
        exception.getMessage().startsWith("Premature end of Content-Length");
  }

  private static boolean isRetriableConscryptException(Throwable exception) {
    if (exception instanceof SSLException) {
      String message = exception.getMessage();

      if (message != null && message.contains("I/O error during system call")) {
        return message.contains("No error") ||
            message.contains("Connection reset by peer") ||
            message.contains("Connection timed out");
      }
    }

    return false;
  }

  private static boolean isRetriableNestedSslException(Throwable exception) {
    return exception instanceof SSLException && isRetriableNetworkException(exception.getCause());
  }

  /**
   * Executes an HTTP request and returns the response as a JsonBrowser instance.
   *
   * @param httpInterface HTTP interface to use for the request.
   * @param request Request to perform.
   * @return Response as a JsonBrowser instance. null in case of 404.
   * @throws IOException On network error or for non-200 response code.
   */
  public static JsonBrowser fetchResponseAsJson(HttpInterface httpInterface, HttpUriRequest request) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == HttpStatus.SC_NOT_FOUND) {
        return null;
      } else if (!isSuccessWithContent(statusCode)) {
        throw new FriendlyException("Server responded with an error.", SUSPICIOUS,
            new IllegalStateException("Response code from channel info is " + statusCode));
      }

      return JsonBrowser.parse(response.getEntity().getContent());
    }
  }

  /**
   * Executes an HTTP request and returns the response as an array of lines.
   *
   * @param httpInterface HTTP interface to use for the request.
   * @param request Request to perform.
   * @param name Name of the operation to include in exception messages.
   * @return Array of lines from the response
   * @throws IOException On network error or for non-200 response code.
   */
  public static String[] fetchResponseLines(HttpInterface httpInterface, HttpUriRequest request, String name) throws IOException {
    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (!isSuccessWithContent(statusCode)) {
        throw new IOException("Unexpected response code " + statusCode + " from " + name);
      }

      return DataFormatTools.streamToLines(response.getEntity().getContent(), StandardCharsets.UTF_8);
    }
  }

  /**
   * @param response Http response to get the header value from.
   * @param name Name of the header.
   * @return Value if header was present, null otherwise.
   */
  public static String getHeaderValue(HttpResponse response, String name) {
    Header header = response.getFirstHeader(name);
    return header != null ? header.getValue() : null;
  }

  private static class NoResponseRetryHandler extends DefaultHttpRequestRetryHandler {
    private static final NoResponseRetryHandler RETRY_INSTANCE = new NoResponseRetryHandler();

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
      boolean retry = super.retryRequest(exception, executionCount, context);

      if (!retry && exception instanceof NoHttpResponseException && executionCount < 5) {
        return true;
      } else {
        return retry;
      }
    }
  }
}
