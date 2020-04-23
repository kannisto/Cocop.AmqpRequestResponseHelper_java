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
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP.BasicProperties;

/**
 * A helper class to implement a request-response client for AMQP. This class *does not* recover from 
 * a connection loss or channel shutdown. In such a situation, you must re-create each client object.
 * @author Petri Kannisto
 */
public class RequestResponseClient extends ConsumerHolderBase
{
	private final Channel m_channel;
	private final String m_exchangeName;
	private final String m_targetName;
    
    // This queue will block execution until a message arrives
    private final BlockingQueue<byte[]> m_responseQueue = new ArrayBlockingQueue<byte[]>(1);
    
    // Correlation ID enables the association of a response to a particular request
    private String m_currentCorrelationId = "";
    
    // Because of server-generated events, there could be thread sync issues without
    // appropriate synchronisation
    private final Object m_lockObject = new Object();
    
	
	/**
	 * Constructor.
	 * @param channel Channel.
	 * @param excName Exchange name.
	 * @param tgtName Target topic name.
	 * @throws IOException Thrown if an error occurs.
	 */
	public RequestResponseClient(Channel channel, String excName, String tgtName)
			throws IOException
	{
		super(channel, excName);
		
		m_channel = channel;
		m_exchangeName = excName;
    	m_targetName = tgtName;
	}
	
	/**
	 * Performs a request in the synchronous (blocking) fashion.
	 * 
	 * Please note that this class does not support concurrent requests.
	 * That is, when a request has been sent and a response is awaited,
	 * it is not possible to send another request before the response arrives 
	 * for the first request. Therefore, to execute requests concurrently, 
	 * create multiple instances of this class.
	 * @param message Message to be sent.
	 * @param timeout Timeout value in milliseconds.
	 * @return Response.
	 * @throws IOException Thrown if an error occurs.
	 * @throws InterruptedException Thrown if the operation is interrupted.
	 * @throws TimeoutException Thrown if a timeout occurs while waiting for response.
	 */
	public byte[] performRequest(byte[] message, long timeout)
			throws IOException, InterruptedException, TimeoutException
	{
		expectConsumerIsActive();
		
		try
		{
			// Creating properties
			BasicProperties props = null;
			
			synchronized (m_lockObject)
			{
				m_currentCorrelationId = UUID.randomUUID().toString();
				
				props = new BasicProperties
		    			.Builder()
		    			.correlationId(m_currentCorrelationId)
		    			.replyTo(getTopicName())
		    			.build();
			}
			
			// Sending the message
			m_channel.basicPublish(m_exchangeName, m_targetName, props, message);
			
			// Waiting for response to arrive in the response queue...
	        byte[] response = m_responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
	        
	        if (response == null)
	        {
	        	// Timeout!
	        	throw new TimeoutException("The request timed out");
	        }
	        
	        return response;
		}
		finally
		{
			synchronized (m_lockObject)
			{
				m_currentCorrelationId = "";
			}
		}
	}
	
	@Override
	protected void handleDeliveryImpl(BasicProperties properties, byte[] body)
	{
		// A message has arrived in the "reply to" queue!
		
		synchronized (m_lockObject)
		{
			if (!properties.getCorrelationId().equals(m_currentCorrelationId))
			{
				// Unexpected correlation ID!
				return;
			}
		}
		
		// This will trigger any object waiting for content in the response queue
		m_responseQueue.offer(body);
	}
}
