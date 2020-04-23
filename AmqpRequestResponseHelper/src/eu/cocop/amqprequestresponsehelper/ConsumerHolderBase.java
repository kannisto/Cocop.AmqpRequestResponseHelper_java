//
// Please make sure to read and understand the files README.md and LICENSE.txt.
// 
// This file was prepared in the research project COCOP (Coordinating
// Optimisation of Complex Industrial Processes).
// https://cocop-spire.eu/
//
// Author: Petri Kannisto, Tampere University, Finland
// File created: 3/2020
// Last modified: 3/2020

package eu.cocop.amqprequestresponsehelper;

import java.io.IOException;

import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

/**
 * Base class for classes that manage a consumer object bound to a queue.
 * @author Petri Kannisto
 */
public abstract class ConsumerHolderBase
{
	private final Channel m_channel;
	private final String m_exchange;
	private final String m_topicName;
	
	// Due to server-generated events, there can be a situation
    // where the consumer is cancelled right after this class
    // has confirmed it is still active.
    // Still, m_lockObject at least enables data synchronisation between threads.
    private final Object m_lockObject = new Object();
	
	private String m_consumerTag = null;
	
	// This indicates the reason why the object cannot be used if any
    private String m_consumerInactiveReason = "No consumer created successfully";
	
    /**
     * Constructor. Use this when the topic of to consume shall be generated.
     * @param channel Channel.
     * @param excName Exchange name.
     * @throws IOException Thrown if an error occurs.
     */
    ConsumerHolderBase(Channel channel, String excName)
    		throws IOException
    {
    	this(channel, excName, null);
    }
    
    /**
     * Constructor. Use this to explicitly specify the topic to consume.
     * @param channel Channel.
     * @param excName Exchange name.
     * @param topic The topic to consume. 
     * @throws IOException Thrown if an error occurs.
     */
	ConsumerHolderBase(Channel channel, String excName, String topic)
			throws IOException
	{
		m_channel = channel;
    	m_exchange = excName;
		
    	try
        {
	    	// Declaring an exchange.
	        // Request-response could use a direct exchange, which is simpler than a topic-based exchange.
	        // However, as topics are utilised in publish-subscribe scenarios anyway, this code uses
	        // topics here as well to enable re-using an already existing topic exchange.
	    	boolean exDurable = true;
	    	boolean exAutoDelete = false;
	    	m_channel.exchangeDeclare(m_exchange, BuiltinExchangeType.TOPIC, exDurable, exAutoDelete, null);
	    	
	    	// Declaring a queue.
	        // Empty queue name -> use a generated name.
	        // The queue is durable -> survive restart.
	        // However, "autodelete" makes sure (?) the queue is deleted if no-one uses it.
	        // It is assumed that if the broker reboots quickly, this client will not notice it and keeps
	        // using the same queue. In such a case, the channel object should reconnect by itself.
	    	boolean qDurable = true;
	    	boolean qExclusive = false;
	    	boolean qAutoDelete = true;
	        String queueName = channel.queueDeclare("", qDurable, qExclusive, qAutoDelete, null).getQueue();
	        
	        // If the topic has not been specified, generating one from the queue name.
	        m_topicName = topic == null ? "topic-" + queueName : topic;
	        
	        // Binding the queue to the topic
	        channel.queueBind(queueName, excName, m_topicName);
    	
			// Creating a consumer for the queue.
	        // autoAck = true -> "no manual acks"
	        boolean autoAck = true;
	        m_consumerTag = m_channel.basicConsume(queueName, autoAck, new DefaultConsumer(m_channel)
	        {
	        	@Override
	            public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
	            		throws IOException
	            {
	        		if (!consumerTagEquals(consumerTag))
	            	{
	            		return; // Unexpected consumer tag
	            	}
	        		
	        		handleDeliveryImpl(properties, body);
	            	super.handleDelivery(consumerTag, envelope, properties, body);
	            }
	        	
	        	@Override
	        	public void handleCancel(String consumerTag)
	        			throws IOException
	        	{
	        		if (!consumerTagEquals(consumerTag))
	            	{
	            		return; // Unexpected consumer tag
	            	}
	        		
	        		markConsumerInactive("Consumer has been cancelled");
	        		super.handleCancel(consumerTag);
	        	}
	        	
	        	@Override
	        	public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig)
	        	{
	        		if (!consumerTagEquals(consumerTag))
	            	{
	            		return; // Unexpected consumer tag
	            	}
	        		
	        		markConsumerInactive("Shutdown has occurred");
	        		super.handleShutdownSignal(consumerTag, sig);
	        	}
	        });
        }
        catch (Exception e)
        {
			// Clean up resources
        	close(); 
        	
        	throw new IOException("Failed to init consumer: " + e.getMessage(), e);
		}
	}
	
	
	// ### Public or protected methods ###
	
	/**
     * Closes the object.
     */
    public void close()
    {
    	// Cancelling the consumer
    	if (consumerIsActive())
    	{
    		String consumerTagTemp = null;
    		
    		// At this point, the consumer may have become inactive, although the
    		// following code assumes otherwise. However, if the stored consumer
    		// tag has become null, the following exception handling block
    		// supposedly takes care of the situation.
    		
    		synchronized (m_lockObject)
    		{
				consumerTagTemp = m_consumerTag;
			}
    		
    		try
    		{
    			m_channel.basicCancel(consumerTagTemp);
    		}
    		catch (Exception e)
    		{
				// No can do! :/
    			// Not leaking any exceptions from this method
			}
    		
    		markConsumerInactive("User has closed the object");
    	}
    }
	
    /**
     * Returns the name of the topic associated to the consumed queue.
     * @return Topic name.
     */
    protected String getTopicName()
    {
    	return m_topicName;
    }
    
    /**
     * Checks if the consumer held is active. Throws an exception if not.
     * @throws IOException Thrown if the consumer is inactive.
     */
	protected void expectConsumerIsActive() throws IOException
    {
		String reason = "";
		
		synchronized (m_lockObject)
    	{
			// This block nests "synchronized (m_lockObject)", but this does not cause a deadlock:
			// "--  a thread can acquire a lock that it already owns"; please see
			// https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html
			if (consumerIsActive())
			{
				return;
			}
			
			// Consumer not active -> throw an exception
			reason = m_consumerInactiveReason;
    	}
		
		throw new IOException("The object is unusable. Reason: " + reason);
    }
	
	/**
	 * Implements the handling of a delivery received by the consumer.
	 * @param properties Properties.
	 * @param body Message body.
	 */
	protected abstract void handleDeliveryImpl(BasicProperties properties, byte[] body);
	
	
	// ### Private methods ###
	
	private boolean consumerIsActive()
    {
    	// Whether the consumer is active
		synchronized (m_lockObject)
    	{
			return m_consumerTag != null && !m_consumerTag.equals("");
		}
    }
    
    private void markConsumerInactive(String reason)
    {
    	// Mark that the consumer is inactive
    	synchronized (m_lockObject)
		{
			m_consumerTag = null;
			m_consumerInactiveReason = reason;
		}
    }
    
    private boolean consumerTagEquals(String tag)
    {
    	// Check equality of the consumer tag
    	synchronized (m_lockObject)
    	{
			return m_consumerTag != null && m_consumerTag.equals(tag);
		}
    }
}
