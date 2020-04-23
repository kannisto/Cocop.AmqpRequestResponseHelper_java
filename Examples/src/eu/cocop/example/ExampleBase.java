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

package eu.cocop.example;

import java.util.Scanner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Base class for example applications. Inheritance is applied to reduce the amount of required code.
 * @author Petri Kannisto
 */
public abstract class ExampleBase
{
	// Change these as needed
	private static final String ExchangeName = "cocoptest-durable.reqrespexample";
    private static final String TopicName = "cocoptest.mytopic";
    
	
	/**
	 * Constructor.
	 */
	public ExampleBase()
	{
		// Empty ctor body
	}
	
	/**
	 * Runs the program.
	 */
	public void run()
	{
		Scanner inputReader = null;
		
		try
        {
			inputReader = new Scanner(System.in);
			
			// Getting host, username and password
			printMsg("Give host:");
			String host = inputReader.nextLine();
			printMsg("Give username:");
			String username = inputReader.nextLine();
			printMsg("Give password:");
			String password = inputReader.nextLine();
			
			// Setting connection parameters
	        // For more information, see https://www.rabbitmq.com/ssl.html
			ConnectionFactory factory = new ConnectionFactory();
			factory.useSslProtocol();
			factory.setUri(buildHostUrl(host, username, password));
			
			Connection connection = null;
			Channel channel = null;
			
			try
			{
				// TODO: If connection or channel shuts down, re-connect and re-start the workflow
				// (neither the RequestResponseClient nor the RequestResponseServer can recover from a shutdown)
				connection = factory.newConnection();
				channel = connection.createChannel();
				
				printMsg("Connection opened.");

				// Creating a client and executing the example workflow
            	executeWorkflow(inputReader, channel, ExchangeName, TopicName);
			}
			finally
			{
				if (channel != null) channel.close();
				if (connection != null) connection.close();
			}
        }
        catch (Exception e)
		{
        	printError(e);
        	printMsg("!!! Quitting due to an error.");
		}
		finally
		{
			if (inputReader != null)
			{
				inputReader.close();
			}
		}
	}
	
	private String buildHostUrl(String host, String user, String pwd)
	{
		final int port = 5671; // Default port of secure connection
		return String.format("amqps://%s:%s@%s:%d", user, pwd, host, port);
	}
	
	/**
	 * Prints an error message.
	 * @param e Related exception.
	 */
	protected void printError(Exception e)
	{
		String errMsg = String.format("!!! %s %s", e.getClass().getSimpleName(), e.getMessage());
		System.err.println(errMsg);
	}
	
	/**
	 * Prints a message.
	 * @param msg Message.
	 */
	protected void printMsg(String msg)
	{
		System.out.println(msg);
	}
	
	/**
	 * Abstract method for the application workflow.
	 * @param inputReader Input reader.
	 * @param channel Channel object.
	 * @param exchange Exchange name.
	 * @param topic Topic name.
	 */
	protected abstract void executeWorkflow(Scanner inputReader, Channel channel, String exchange, String topic);
}
