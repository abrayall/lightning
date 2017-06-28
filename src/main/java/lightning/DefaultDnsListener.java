package lightning;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsResponse;

public class DefaultDnsListener implements DnsListener {
	public String query(InetSocketAddress client, DnsQuery query, String name) {
		return name;
	}

	public InetAddress resolution(InetSocketAddress client, DnsQuery query, DnsResponse response, String name, InetAddress address) {
		return address;
	}

	public long ttl(InetSocketAddress client, DnsQuery query, DnsResponse response, String name, InetAddress address, long ttl) {
		return ttl;
	}	
}
