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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import com.rabbitmq.client.Channel;

import eu.cocop.amqprequestresponsehelper.IRequestReceivedEventListener;
import eu.cocop.amqprequestresponsehelper.RequestReceivedEvent;
import eu.cocop.amqprequestresponsehelper.RequestResponseServer;

/**
 * This is an example application about implementing an AMQP request-response server.
 * @author Petri Kannisto
 */
public class ServerExample extends ExampleBase
{
	private RequestResponseServer reqRespServer = null;
	
	
	/**
	 * Constructor.
	 */
	private ServerExample()
	{
		super(); // Otherwise, empty ctor body
	}
	
	/**
	 * The entry point of the program.
	 * @param args Arguments.
	 */
	public static void main(String[] args)
	{
		new ServerExample().run();
	}
	
	@Override
	protected void executeWorkflow(Scanner inputReader, Channel channel, String exchange, String topic)
	{
		try
		{
			try
			{
				// Creating a server object
				reqRespServer = new RequestResponseServer(channel, exchange, topic);
				
				// Adding an event listener to serve requests
				reqRespServer.addRequestReceivedEventListener(new IRequestReceivedEventListener()
				{							
					@Override
					public void requestReceived(Object source, RequestReceivedEvent ev)
					{
						sendResponseToClient(reqRespServer, ev);
					}
				});
			}
			catch (IOException e)
			{
				printError(e);
				printMsg("!!! Quitting due to an error.");
				return;
			}
			
			// Running until the user gives 'Q'
			while (true)
			{
				printMsg("Server running. Give Q to quit.");
				System.out.print(" > ");
				String userInput = inputReader.nextLine().trim();
				
				if (userInput.toLowerCase().equals("q"))
				{
					break;
				}
			}
		}
		finally
		{
			if (reqRespServer != null)
			{
				reqRespServer.close();
				reqRespServer = null;
			}
		}
	}
	
	private void sendResponseToClient(RequestResponseServer reqRespServer, RequestReceivedEvent ev)
	{
        try
        {
            // Printing the request
            String messageIn = new String(ev.getMessage(), StandardCharsets.UTF_8);
            printMsg("Got message \"" + messageIn + "\"");

            // Creating a response.
            // In a real scenario, you would somehow process the request (such as retrieve data
            // from a database).
            String response = "Your request arrived at " + new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date());
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

            // Sending the response
            reqRespServer.sendResponse(ev, responseBytes);
            
            printMsg("- Responded with \"" + response + "\"");
        }
        catch (Exception e)
        {
            String errMsg = String.format("Error: %s: %s", e.getClass().getName(), e.getMessage());
        	printMsg(errMsg);
        }
	}
}
