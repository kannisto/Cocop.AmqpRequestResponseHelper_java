//
// Please make sure to read and understand the files README.md and LICENSE.txt.
// 
// This file was prepared in the research project COCOP (Coordinating
// Optimisation of Complex Industrial Processes).
// https://cocop-spire.eu/
//
// Author: Petri Kannisto, Tampere University, Finland
// File created: 7/2018
// Last modified: 3/2020

package eu.cocop.amqprequestresponsehelper;

import java.io.IOException;
import java.util.HashSet;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;

/**
 * A class that acts as a request-response server for an AMQP message bus. This class *does not* recover from 
 * a connection loss or channel shutdown. In such a situation, you must re-create each server object.
 * @author Petri Kannisto
 */
public class RequestResponseServer extends ConsumerHolderBase
{
	private final Channel m_channel;
    private final String m_exchangeName;
    private final HashSet<IRequestReceivedEventListener> m_eventListeners;
    
    // Because of server-generated events, there could be thread sync issues without
    // appropriate synchronisation
    private final Object m_lockObject = new Object();
    
    
    /**
     * Constructor.
     * @param channel Channel.
     * @param excName Exchange name.
     * @param servTopic Server topic name.
     * @throws IOException Thrown if an error occurs.
     */
    public RequestResponseServer(Channel channel, String excName, String servTopic)
    		throws IOException
    {
    	// Create the server queue and pass its name to the superclass constructor
    	super(channel, excName, servTopic);
    	
    	m_channel = channel;
    	m_exchangeName = excName;
    	m_eventListeners = new HashSet<>();
    }
    
    /**
     * Adds a listener for the RequestReceivedEvent.
     * @param lis Listener object.
     * @throws IOException Thrown if the object is in an unusable state.
     */
    public void addRequestReceivedEventListener(IRequestReceivedEventListener lis)
    		throws IOException
    {
    	expectConsumerIsActive();
    	
    	synchronized (m_lockObject)
    	{
    		m_eventListeners.add(lis);
		}
    }
    
    /**
     * Removes a listener for the RequestReceivedEvent.
     * @param lis Listener object.
     * @throws IOException Thrown if the object is in an unusable state.
     */
    public void removeRequestReceivedEventListener(IRequestReceivedEventListener lis)
    		throws IOException
    {
    	expectConsumerIsActive();
    	
    	synchronized (m_lockObject)
    	{
    		m_eventListeners.remove(lis);
    	}
    }
    
    /**
     * Sends a response to a request.
     * @param args Event arguments.
     * @param msg Message.
     * @throws IOException Thrown if publishing fails or if the object is in an unusable state.
     */
    public void sendResponse(RequestReceivedEvent args, byte[] msg)
    		throws IOException
    {
    	expectConsumerIsActive();
    	
    	BasicProperties replyProps = new BasicProperties
    			.Builder()
    			.correlationId(args.getCorrelationId())
    			.build();
    	
    	m_channel.basicPublish(m_exchangeName, args.getReplyTo(), replyProps, msg);
    	
    	// AutoAck is enabled -> no manual acking
        // C#: channel.BasicAck(deliveryTag: eventArgs.DeliveryTag, multiple: false);
    }
    
    @Override
    protected void handleDeliveryImpl(BasicProperties properties, byte[] body)
    {
    	// A request has arrived in the queue!
    	
    	// Creating an event object to notify listeners.
    	RequestReceivedEvent eventObj = new RequestReceivedEvent(
    			properties.getReplyTo(), properties.getCorrelationId(), body);
    	
    	// Notifying listeners. Not iterating the listener list but a copy,
    	// because this loop could take a long time to execute and this
    	// could block another thread.
    	HashSet<IRequestReceivedEventListener> copyOfListeners = null;
    	
    	synchronized (m_lockObject)
    	{
    		copyOfListeners = new HashSet<>(m_eventListeners);
    	}
    	
    	for (IRequestReceivedEventListener lis : copyOfListeners)
    	{
    		try
    		{
    			lis.requestReceived(this, eventObj);
    		}
    		catch (Exception e)
    		{} // No can do
    	}
    }
}
