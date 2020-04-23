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

package eu.cocop.amqprequestresponsehelper.clienttest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import eu.cocop.amqprequestresponsehelper.RequestResponseClient;

/**
 * Runs a client. Use this for testing.
 * @author Petri Kannisto
 */
public class ClientTest
{
	// **************************************************************
    // The purpose of this test program is to test:
    // (1) Basic request-response functionality
    // (2) Re-connection when the connection fails
    // **************************************************************
    /*
    To run the test:
    1 Start "RequestResponseServerTest".
    2 Start "RequestResponseClientTest" respectively.
    3 Send message "Hello" with the client. Make sure that the server receives the message and 
      responds to the client (this should be seen in the console window).
      Make sure that the client receives the response.
    4 Send message "Hello 2" with the client. Perform the checks similar to the previous step.
    5 Disconnect your computer from Internet. Wait for the message "the broker is unreachable"
      to appear in the server window (this should take less than two minutes).
    6 Try to send message "Hello 3" with the client. "The broker is unreachable"
      should appear in its window. (If this gives a timeout instead, retry this step.)
    7 Try to reconnect the client by giving "y" in its window. The connection should fail but
      the application should ask you to reconnect.
    8 Try to reconnect the server by giving "y" in its window. The connection should fail but
      the application should ask you to reconnect.
    9 Re-connect your computer to Internet. Wait for 30 seconds.
    10 Try to reconnect both the server and the client. They both should now recover the connection.
    11 Send message "Hello 4" with the client. Perform the checks similar to step 3.
    */
	
	
    private static final String EXCHANGE_NAME = "cocoptest-durable";
    private static final String TARGET_TOPIC_NAME = "cocoptest.my_request_topic_2";
	
	
    /**
	 * Entry point of the program.
	 * @param args Arguments.
	 * @throws Exception Thrown if an error occurs.
	 */
	public static void main(String[] args) throws Exception
	{
		Scanner inputReader = new Scanner(System.in);
		
		try
		{
			// Getting host, username and password
			System.out.println("Give host:");
			String host = inputReader.nextLine();
			System.out.println("Give username:");
			String username = inputReader.nextLine();
			System.out.println("Give password:");
			String password = inputReader.nextLine();
			
			// Setting connection parameters
	        // For more information, see https://www.rabbitmq.com/ssl.html
			ConnectionFactory factory = new ConnectionFactory();
			factory.useSslProtocol();
			factory.setUri(buildHostUrl(host, username, password));
			
			// The loop enables reconnection if the connection fails
			while (true)
			{
				printWithoutLineBreak("Setting up a connection... ");
				
				try
	            {
					Connection connection = null;
					Channel channel = null;
					RequestResponseClient client = null;
					
					try
					{
						connection = factory.newConnection();
						channel = connection.createChannel();
						
		            	printMsg("done!");
		
		            	client = new RequestResponseClient(channel, EXCHANGE_NAME, TARGET_TOPIC_NAME);
		
		                sendMessages(inputReader, client);
		                return;
					}
					finally
					{
						if (client != null) client.close();
						if (channel != null) channel.close();
						if (connection != null) connection.close();
					}
	            }
	            // Instead of exceptions, another way to react to connection loss is to 
	            // sign up for the event "ModelShutdown" of the channel object.
	            catch (AlreadyClosedException | IOException | TimeoutException e)
				{
	            	printMsg("!!! " + e.getClass().getSimpleName() + ": " + e.getMessage());
				}
				
	            // Asking for reconnect
				printMsg("The broker is unreachable. Reconnect (y/n)?");
				String input = inputReader.nextLine().trim().toLowerCase();
				
				if (input.equals("n"))
				{
					return;
				}
			}
		}
		finally
		{
			inputReader.close();
		}
	}
	
	private static void sendMessages(Scanner inputReader, RequestResponseClient client)
			throws IOException, InterruptedException
	{
		while (true)
        {
            try
            {
                printMsg("Please give a message to be sent or 'q' to quit:");
                printWithoutLineBreak("> ");
                String input = inputReader.nextLine();

                if (input.trim().toLowerCase().equals("q"))
                {
                    return;
                }

                printMsg("Requesting...");

                byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
                byte[] responseBytes = client.performRequest(inputBytes, 5000);
                String response = new String(responseBytes, StandardCharsets.UTF_8);

                printMsg("Response: \"" + response + "\"");
            }
            catch (TimeoutException e)
            {
            	printMsg("The request has timed out.");
            }
        }
	}
	
	private static String buildHostUrl(String host, String user, String pwd)
	{
		final int port = 5671;
		return String.format("amqps://%s:%s@%s:%d", user, pwd, host, port);
	}
	
	private static void printWithoutLineBreak(String msg)
	{
		System.out.print(msg);
	}
	
	private static void printMsg(String msg)
	{
		System.out.println(msg);
	}
}
