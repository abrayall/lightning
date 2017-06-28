package lightning;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.HostsFileEntriesResolver;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.resolver.dns.*;
import io.netty.util.concurrent.Future;

public class DnsResolver {
	
	protected DnsNameResolver resolver;
	protected static NioEventLoopGroup THREADS = new NioEventLoopGroup(20);
	
	@SuppressWarnings("deprecation")
	public DnsResolver() {
		this(Collections.synchronizedMap(new HashMap<String, InetAddress>()), DnsServerAddresses.defaultAddresses());
	}
	
	public DnsResolver(InetSocketAddress... addresses) {
		this(Collections.synchronizedMap(new HashMap<String, InetAddress>()), DnsServerAddresses.sequential(addresses));
	}
	
	public DnsResolver(Map<String, InetAddress> mappings, InetSocketAddress... addresses) {
		this(mappings, DnsServerAddresses.sequential(addresses));
	}
	
	public DnsResolver(Map<String, InetAddress> mappings, DnsServerAddresses addresses) {
		this.resolver = new DnsNameResolverBuilder(THREADS.next())
			.resolveCache(new DefaultDnsCache())
			.channelType(NioDatagramChannel.class)
			.hostsFileEntriesResolver(new MapHostsResolver(mappings))
			.nameServerProvider(new DsnServerAddressProvider(addresses)).build();
	}
	
	public Future<InetAddress> resolve(String name) {
		return this.resolver.resolve(name);
	}
	
	public void resolve(String name, Consumer<InetAddress> complete) {
		this.resolve(name, complete, (error) -> { complete.accept(null); });
	}
	
	public void resolve(String name, Consumer<InetAddress> complete, Consumer<Exception> error) {
		this.resolve(name).addListener((future) -> {
			try {
				complete.accept((InetAddress)future.get());
			} catch (Exception e) {
				error.accept(e);
			}
		});
	}
	
	public void close() {
		this.resolver.close();
	}
	
	protected static class DsnServerAddressProvider implements DnsServerAddressStreamProvider {
		protected DnsServerAddresses addresses;
		
		public DsnServerAddressProvider(DnsServerAddresses addresses) {
			this.addresses = addresses;
		}
		
		public DnsServerAddressStream nameServerAddressStream(String name) {
			return this.addresses.stream();
		}
		
	}
	protected static class MapHostsResolver implements HostsFileEntriesResolver {

		protected Map<String, InetAddress> mappings;
		
		protected MapHostsResolver(Map<String, InetAddress> mappings) {
			this.mappings = mappings;
		}
		
		public InetAddress address(String name) {
			return this.mappings.get(name);
		}

		public InetAddress address(String name, ResolvedAddressTypes arg1) {
			return this.mappings.get(name);
		}
	}
}