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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;

import eu.cocop.amqprequestresponsehelper.RequestResponseClient;

/**
 * This is an example application about implementing an AMQP request-response client.
 * @author Petri Kannisto
 */
public class ClientExample extends ExampleBase
{
	/**
	 * Constructor.
	 */
	private ClientExample()
	{
		super(); // Otherwise, empty ctor body
	}
	
	/**
	 * The entry point of the program.
	 * @param args Arguments.
	 */
	public static void main(String[] args)
	{
		new ClientExample().run();
	}
	
	@Override
	protected void executeWorkflow(Scanner inputReader, Channel channel, String exchange, String topic)
	{
		// Implementing the client workflow
		
		RequestResponseClient client = null;
		
		try
		{
			// Create client object
			client = new RequestResponseClient(channel, exchange, topic);
	
			// Send requests with the client
			sendRequests(inputReader, client);
		}
		catch (IOException | InterruptedException e)
		{
			String errMsg = String.format("!!! %s %s", e.getClass().getSimpleName(), e.getMessage());
			System.err.println(errMsg);
			printMsg("!!! Quitting due to an error.");
		}
		finally
		{
			if (client != null) client.close();
		}
	}
	
	private void sendRequests(Scanner inputReader, RequestResponseClient client)
			throws IOException, InterruptedException
	{
		// Reading user input and sending requests accordingly
		
		final int timeout_ms = 5000; // This is the request timeout in milliseconds
		
		while (true)
		{
			try
			{
				printMsg("Give Q to quit or any other input to send a request:");
				System.out.print(" > ");
				String userInput = inputReader.nextLine().trim();
				
				if (userInput.toLowerCase().equals("q"))
				{
					break;
				}
				
				printMsg("Requesting...");

                byte[] inputBytes = userInput.getBytes(StandardCharsets.UTF_8);
                byte[] responseBytes = client.performRequest(inputBytes, timeout_ms);
                String response = new String(responseBytes, StandardCharsets.UTF_8);

                printMsg("Response: \"" + response + "\"");
			}
			catch (TimeoutException e)
			{
				printMsg("The request timed out.");
			}
		}
	}
}
