package lavaplayer.extensions.iprotator.tools.ip;

import kotlin.Pair;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import kotlin.jvm.Throws

object IpAddressTools {

    @Throws(HttpException::class)
    @JvmStatic
    fun getRandomAddressesFromHost(host: HttpHost): Pair<Inet4Address, Inet6Address> {
        val ipList: List<InetAddress>;
        try {
            ipList = InetAddress.getAllByName(host.hostName)
                .toList()
                .reversed();
        } catch (e: UnknownHostException) {
            throw HttpException("Could not resolve ${host.hostName}", e);
        }

        val ipv6 = ipList.filterIsInstance<Inet6Address>();
        val ipv4 = ipList.filterIsInstance<Inet4Address>();

        return ipv4.random() to ipv6.random()
    }
}
