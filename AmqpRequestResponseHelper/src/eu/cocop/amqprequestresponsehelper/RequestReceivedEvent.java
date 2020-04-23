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

/**
 * Holds the event data of a received request.
 * @author Petri Kannisto
 */
public class RequestReceivedEvent
{
	private final String m_replyTo;
	private final String m_correlationId;
	private final byte[] m_message;
	
	
	/**
	 * Constructor.
	 * @param repl "Reply to" reference.
	 * @param corrId Correlation ID.
	 * @param msg The received message.
	 */
	public RequestReceivedEvent(String repl, String corrId, byte[] msg)
	{
		m_replyTo = repl;
		m_correlationId = corrId;
		m_message = msg;
	}

	/**
	 * "Reply to" reference.
	 * @return "Reply to" reference.
	 */
	public String getReplyTo()
	{
		return m_replyTo;
	}

	/**
	 * Correlation ID.
	 * @return Correlation ID.
	 */
	public String getCorrelationId()
	{
		return m_correlationId;
	}

	/**
	 * The received message.
	 * @return The received message.
	 */
	public byte[] getMessage()
	{
		return m_message;
	}
}
