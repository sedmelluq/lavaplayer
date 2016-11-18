package com.sedmelluq.discord.lavaplayer.tools.io;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
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
import javax.net.ssl.X509TrustManager;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

/**
 * Tools for working with HttpClient
 */
public class HttpClientTools {
  private static final Logger log = LoggerFactory.getLogger(HttpClientTools.class);

  private static final SSLContext sslContext = setupSslContext();

  /**
   * @return An HttpClientBuilder which uses the same cookie store for all clients
   */
  public static HttpClientBuilder createSharedCookiesHttpBuilder() {
    CookieStore cookieStore = new BasicCookieStore();
    final HttpClientBuilder httpClientBuilder = new CustomHttpClientBuilder();
    httpClientBuilder.setDefaultCookieStore(cookieStore);
    httpClientBuilder.setDefaultRequestConfig(
        RequestConfig.custom()
            .setConnectTimeout(3000)
            .build()
    );

    return httpClientBuilder;
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
      return null;
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

  private static class CustomHttpClientBuilder extends HttpClientBuilder {
    @Override
    public synchronized CloseableHttpClient build() {
      setConnectionManager(createConnectionManager());
      CloseableHttpClient httpClient = super.build();
      setConnectionManager(null);
      return httpClient;
    }

    private static HttpClientConnectionManager createConnectionManager() {
      return new PoolingHttpClientConnectionManager(createConnectionSocketFactory(), createConnectionFactory());
    }

    private static Registry<ConnectionSocketFactory> createConnectionSocketFactory() {
      HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());
      ConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext != null ? sslContext :
          SSLContexts.createDefault(), hostnameVerifier);

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
   * @param response Response object.
   * @return A redirect location if the status code indicates a redirect and the Location header is present.
   */
  public static String getRedirectLocation(HttpResponse response) {
    if (!isRedirectStatus(response.getStatusLine().getStatusCode())) {
      return null;
    }

    Header header = response.getFirstHeader("Location");
    return header != null ? header.getValue() : null;
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
}
