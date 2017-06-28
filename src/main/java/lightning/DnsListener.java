package lightning;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsResponse;

public interface DnsListener {
	public String query(InetSocketAddress client, DnsQuery query, String name);
	public InetAddress resolution(InetSocketAddress client, DnsQuery query, DnsResponse repsonse, String name, InetAddress address);
	public long ttl(InetSocketAddress client, DnsQuery query, DnsResponse response, String name, InetAddress address, long ttl);
}
