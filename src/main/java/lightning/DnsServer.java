package lightning;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsQueryDecoder;
import io.netty.handler.codec.dns.DatagramDnsResponseEncoder;

public class DnsServer {
	
	protected int port = 53;
	protected List<InetSocketAddress> forwarding = new ArrayList<InetSocketAddress>();
	protected List<DnsResolver> resolvers;

	protected Map<String, InetAddress> mappings = Collections.synchronizedMap(new HashMap<String, InetAddress>());
	protected Map<InetAddress, DnsListener> listeners = Collections.synchronizedMap(new HashMap<InetAddress, DnsListener>());

	protected Bootstrap configuration = new Bootstrap();
	protected EventLoopGroup group = new NioEventLoopGroup(20); 
	protected ChannelFuture server;
	
	public DnsServer() {
		this.resolvers = this.resolvers(20);
		this.configuration.group(this.group);
		this.configuration.channel(NioDatagramChannel.class);
		this.configuration.option(ChannelOption.SO_BROADCAST, true);
		this.configuration.handler(new ChannelInitializer<Channel>() {
            public void initChannel(Channel channel) throws Exception {
            	channel.pipeline().addLast("decoder", new DatagramDnsQueryDecoder());
            	channel.pipeline().addLast("encoder", new DatagramDnsResponseEncoder());
            	channel.pipeline().addLast(new DnsHandler(resolvers, listeners));
            }
        });	
	}
	
	public DnsServer setPort(int port) {
		this.port = port;
		return this;
	}
	
	public DnsServer addForwarding(String address) {
		this.addForwarding(address, 53);
		return this;
	}

	public DnsServer addForwarding(String address, int port) {
		this.addForwarding(new InetSocketAddress(address, port));
		return this;
	}

	public DnsServer addForwarding(InetSocketAddress address) {
		this.forwarding.add(address);
		return this;
	}
	
	public DnsServer setForwarding(String address) {
		this.setForwarding(address, 53);
		return this;
	}

	public DnsServer setForwarding(String address, int port) {
		this.setForwarding(new InetSocketAddress(address, port));
		return this;
	}
	
	public DnsServer setForwarding(InetSocketAddress... addresses) {
		this.forwarding = Arrays.asList(addresses);
		return this;
	}
	
	public DnsServer addMapping(String name, InetAddress address) {
		this.mappings.put(name, address);
		return this;
	}
	
	public DnsServer addMappings(Map<String, InetAddress> mappings) {
		this.mappings.putAll(mappings);
		return this;
	}
	
	public DnsServer setMappings(Map<String, InetAddress> mappings) {
		this.mappings.clear();
		this.addMappings(mappings);
		return this;
	}
	
	public DnsServer addListener(DnsListener listener) {
		this.addListener(InetAddress.getLoopbackAddress(), listener);
		return this;
	}

	public DnsServer addListener(InetAddress address, DnsListener listener) {
		this.listeners.put(address, listener);
		return this;
	}
	
	public DnsServer removeListener(InetAddress address) {
		this.listeners.remove(address);
		return this;
	}
	
	public DnsListener getListener(InetAddress address) {
		return this.listeners.get(address);
	}
	
	public DnsServer start() throws Exception {
		this.server = this.configuration.bind(this.port).sync();
		return this;
	}
	
	public DnsServer stop() {
		this.server.channel().close();
		this.server.awaitUninterruptibly();
		this.group.shutdownGracefully();
		return this;
	}
	
	public boolean running() {
		return this.server != null;
	}
	
	protected List<DnsResolver> resolvers(int count) {
		ArrayList<DnsResolver> resolvers = new ArrayList<DnsResolver>();
		for (int i = 0; i < count; i++)
			resolvers.add(new DnsResolver(mappings, forwarding.toArray(new InetSocketAddress[0])));
		
		return resolvers;
	}
}