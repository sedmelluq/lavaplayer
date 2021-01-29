package com.sedmelluq.discord.lavaplayer.tools.http;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.TrustManagerBuilder;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ProtocolVersion;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpClientConnectionOperator;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultHttpResponseParser;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class ExtendedHttpClientBuilder extends HttpClientBuilder {
  private static final Logger log = LoggerFactory.getLogger(ExtendedHttpClientBuilder.class);

  private static final SSLContext defaultSslContext = setupSslContext();

  private SSLContext sslContextOverride;
  private String[] sslSupportedProtocols;
  private ConnectionManagerFactory connectionManagerFactory = ExtendedHttpClientBuilder::createDefaultConnectionManager;

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

  public void setSslSupportedProtocols(String[] protocols) {
    this.sslSupportedProtocols = protocols;
  }

  public void setConnectionManagerFactory(ConnectionManagerFactory factory) {
    this.connectionManagerFactory = factory;
  }

  @Override
  protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
    return mainExec;
  }

  private HttpClientConnectionManager createConnectionManager() {
    return connectionManagerFactory.create(
        new ExtendedConnectionOperator(createConnectionSocketFactory(), null, null),
        createConnectionFactory()
    );
  }

  private Registry<ConnectionSocketFactory> createConnectionSocketFactory() {
    HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());
    ConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContextOverride != null ?
        sslContextOverride : defaultSslContext, sslSupportedProtocols, null, hostnameVerifier);

    return RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", sslSocketFactory)
        .build();
  }

  private static ManagedHttpClientConnectionFactory createConnectionFactory() {
    return new ManagedHttpClientConnectionFactory(null, (buffer, constraints) ->
        new GarbageAllergicHttpResponseParser(
            buffer,
            IcyHttpLineParser.ICY_INSTANCE,
            DefaultHttpResponseFactory.INSTANCE,
            constraints
        ));
  }

  private static HttpClientConnectionManager createDefaultConnectionManager(
      HttpClientConnectionOperator operator,
      HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connectionFactory
  ) {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
        operator,
        connectionFactory,
        -1,
        TimeUnit.MILLISECONDS
    );

    manager.setMaxTotal(3000);
    manager.setDefaultMaxPerRoute(1500);

    return manager;
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
    public GarbageAllergicHttpResponseParser(
        SessionInputBuffer buffer,
        LineParser lineParser,
        HttpResponseFactory responseFactory,
        MessageConstraints constraints
    ) {
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

  public interface ConnectionManagerFactory {
    HttpClientConnectionManager create(
        HttpClientConnectionOperator operator,
        HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connectionFactory
    );
  }
}
