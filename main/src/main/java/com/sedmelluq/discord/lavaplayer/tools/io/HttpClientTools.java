package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.DataFormatTools;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Tools for working with HttpClient
 */
public class HttpClientTools {
  private static final Logger log = LoggerFactory.getLogger(HttpClientTools.class);

  private static final SSLContext defaultSslContext = setupSslContext();

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

    return new CustomHttpClientBuilder()
        .setDefaultCookieStore(cookieStore)
        .setRetryHandler(NoResponseRetryHandler.RETRY_INSTANCE)
        .setDefaultRequestConfig(requestConfig);
  }

  private static SSLContext setupSslContext() {
    try {
      X509TrustManager trustManager = new TrustManagerBuilder()
          .addBuiltinCertificates()
          .addFromResourceDirectory("/certificates")
          .build();

      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, new X509TrustManager[] { trustManager }, null);
      return context;
    } catch (Exception e) {
      log.error("Failed to build custom SSL context, using default one.", e);
      return SSLContexts.createDefault();
    }
  }

  private static class GarbageAllergicHttpResponseParser extends DefaultHttpResponseParser {
    public GarbageAllergicHttpResponseParser(SessionInputBuffer buffer, LineParser lineParser, HttpResponseFactory responseFactory, MessageConstraints constraints) {
      super(buffer, lineParser, responseFactory, constraints);
    }

    @Override
    protected boolean reject(CharArrayBuffer line, int count) {
      if (line.length() > 4 && "ICY ".equals(line.substring(0, 4))) {
        throw new FriendlyException("ICY protocol is not supported.", COMMON, null);
      } else if (count > 10) {
        throw new FriendlyException("The server is giving us garbage.", SUSPICIOUS, null);
      }

      return false;
    }
  }

  private static class IcyHttpLineParser extends BasicLineParser {
    private static final IcyHttpLineParser ICY_INSTANCE = new IcyHttpLineParser();
    private static final ProtocolVersion ICY_PROTOCOL = new ProtocolVersion("HTTP", 1, 0);

    @Override
    public ProtocolVersion parseProtocolVersion(CharArrayBuffer buffer, ParserCursor cursor) {
      int index = cursor.getPos();
      int bound = cursor.getUpperBound();

      if (bound >= index + 4 && "ICY ".equals(buffer.substring(index, index + 4))) {
        cursor.updatePos(index + 4);
        return ICY_PROTOCOL;
      }

      return super.parseProtocolVersion(buffer, cursor);
    }

    @Override
    public boolean hasProtocolVersion(CharArrayBuffer buffer, ParserCursor cursor) {
      int index = cursor.getPos();
      int bound = cursor.getUpperBound();

      if (bound >= index + 4 && "ICY ".equals(buffer.substring(index, index + 4))) {
        return true;
      }

      return super.hasProtocolVersion(buffer, cursor);
    }
  }

  /**
   * Custom HTTP client builder which applies our custom modifications.
   */
  public static class CustomHttpClientBuilder extends HttpClientBuilder {
    private SSLContext sslContextOverride;

    @Override
    public synchronized CloseableHttpClient build() {
      setConnectionManager(createConnectionManager());
      CloseableHttpClient httpClient = super.build();
      setConnectionManager(null);
      return httpClient;
    }

    /**
     * @param sslContextOverride SSL context to make the built clients use. Note that calling
     *                           {@link #setSSLContext(SSLContext)} has no effect because this class cannot access the
     *                           instance set with that nor override the method.
     */
    public void setSslContextOverride(SSLContext sslContextOverride) {
      this.sslContextOverride = sslContextOverride;
    }

    private HttpClientConnectionManager createConnectionManager() {
      PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(createConnectionSocketFactory(),
          createConnectionFactory());

      manager.setMaxTotal(3000);
      manager.setDefaultMaxPerRoute(1500);

      return manager;
    }

    private Registry<ConnectionSocketFactory> createConnectionSocketFactory() {
      HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());
      ConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContextOverride != null ?
          sslContextOverride : defaultSslContext, hostnameVerifier);

      return RegistryBuilder.<ConnectionSocketFactory>create()
          .register("http", PlainConnectionSocketFactory.getSocketFactory())
          .register("https", sslSocketFactory)
          .build();
    }

    private static ManagedHttpClientConnectionFactory createConnectionFactory() {
      return new ManagedHttpClientConnectionFactory(null, (buffer, constraints) -> {
        return new GarbageAllergicHttpResponseParser(buffer, IcyHttpLineParser.ICY_INSTANCE, DefaultHttpResponseFactory.INSTANCE, constraints);
      });
    }
  }

  /**
   * A redirect strategy which does not follow any redirects.
   */
  public static class NoRedirectsStrategy implements RedirectStrategy {
    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
      return false;
    }

    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
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
    return statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT;
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
        isRetriableConscryptException(exception);
  }

  private static boolean isConnectionResetException(Throwable exception) {
    return exception instanceof SocketException && "Connection reset".equals(exception.getMessage());
  }

  private static boolean isSocketTimeoutException(Throwable exception) {
    return exception instanceof SocketTimeoutException && "Read timed out".equals(exception.getMessage());
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
      } else if (statusCode != HttpStatus.SC_OK) {
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
      if (statusCode != HttpStatus.SC_OK) {
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
