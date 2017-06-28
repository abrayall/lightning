package lightning;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.dns.DatagramDnsQuery;
import io.netty.handler.codec.dns.DatagramDnsResponse;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsOpCode;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsSection;

public class DnsHandler extends ChannelInboundHandlerAdapter {
	
	protected Iterator<DnsResolver> resolvers;
	protected Map<InetAddress, DnsListener> listeners;
	
	public DnsHandler(List<DnsResolver> resolvers, Map<InetAddress, DnsListener> listeners) {
		this.resolvers = forever(resolvers);
		this.listeners = listeners;
	}
	
    public void channelRead(ChannelHandlerContext context, Object message) {
    	if (DatagramDnsQuery.class.isInstance(message)) {
    		DatagramDnsQuery query = (DatagramDnsQuery)message;
    		
    		String name = query.recordAt(DnsSection.QUESTION).name();
    		resolvers.next().resolve(review(query, name), (address) -> {
    			context.writeAndFlush(address == null ? error(query) : response(address, query));	
    			query.release();
    		});
    	}
    }
    
    private String review(DatagramDnsQuery query, String name) {
    	DnsListener listener = listener(query);
    	if (listener != null)
    		return listener.query(query.sender(), query, name);
    	
    	return name;
    }
    
    private DatagramDnsResponse response(InetAddress address, DatagramDnsQuery query) {
    	return response(address, 86400l, query);
    }
    
    private DatagramDnsResponse response(InetAddress address, long ttl, DatagramDnsQuery query) {
    	DnsQuestion question = query.recordAt(DnsSection.QUESTION);
    	DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id());
    	
    	DnsListener listener = listener(query);
    	if (listener != null) {
    		address = listener.resolution(query.sender(), query, response, question.name(), address);
    		ttl = listener.ttl(query.sender(), query, response, question.name(), address, ttl);
    	}
    	
    	response.addRecord(DnsSection.QUESTION, question);
    	response.addRecord(DnsSection.ADDITIONAL, query.recordAt(DnsSection.ADDITIONAL));
    	response.addRecord(DnsSection.ANSWER, new DefaultDnsRawRecord(question.name(), question.type(), question.dnsClass(), ttl, Unpooled.wrappedBuffer(address.getAddress())));
    	return response;
    }
    
    private DatagramDnsResponse error(DatagramDnsQuery query) {
    	DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id(), DnsOpCode.QUERY, DnsResponseCode.BADNAME);
    	response.addRecord(DnsSection.QUESTION, query.recordAt(DnsSection.QUESTION));
    	return response;
    }
    
    private DnsListener listener(DatagramDnsQuery query) {
    	return listeners.getOrDefault(query.sender().getAddress(), listeners.get(InetAddress.getLoopbackAddress()));
    }
    
    private Iterator<DnsResolver> forever(List<DnsResolver> list) {
    	return new InfiniteIterator<DnsResolver>(list);
    }
    
    public static class InfiniteIterator<T> implements Iterator<T> {
    	
    	List<T> resolvers;
    	Iterator<T> iterator;
    	
    	public InfiniteIterator(List<T> resolvers) {
    		this.resolvers = resolvers;
    	}
		
    	public boolean hasNext() {
			return true;
		}

		public T next() {
			if (this.iterator == null || this.iterator.hasNext() == false)
				this.iterator = this.resolvers.iterator();
			
			return this.iterator.next();
		}
    	
    }
}