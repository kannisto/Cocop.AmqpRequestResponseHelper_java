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

package eu.cocop.amqprequestresponsehelper.servertest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import eu.cocop.amqprequestresponsehelper.IRequestReceivedEventListener;
import eu.cocop.amqprequestresponsehelper.RequestReceivedEvent;
import eu.cocop.amqprequestresponsehelper.RequestResponseServer;

/**
 * Runs a server. Use this class for testing.
 * @author Petri Kannisto
 */
public class ServerTest
{
	// **************************************************************
    // The purpose of this test program is to test:
    // (1) Basic request-response functionality
    // (2) Re-connection when the connection fails
    // **************************************************************
    /*
    To run the test, see the "ClientTest" class of the client test program.
    */
	
    private static final String EXCHANGE_NAME = "cocoptest-durable";
    private static final String SERVER_TOPIC_NAME = "cocoptest.my_request_topic_2";

    // This indicates a connection loss
    private ThreadSafeBoolean m_connectionLost = new ThreadSafeBoolean(false);
    
    private RequestResponseServer m_reqRespServer = null;
    
    private CommandManager m_commandManager = null;
    
    
	/**
	 * Entry point of the program.
	 * @param args Arguments.
	 * @throws Exception Thrown if an error occurs.
	 */
	public static void main(String[] args) throws Exception
	{
		ServerTest serverTest = null;
		
		try
		{
			serverTest = new ServerTest();
			serverTest.run();
		}
		finally
		{
			if (serverTest != null) serverTest.close();
		}
	}
	
	/**
	 * Constructor.
	 */
	private ServerTest()
	{
		m_commandManager = new CommandManager();
	}
	
	/**
	 * Releases resources.
	 */
	private void close()
	{
		if (m_reqRespServer != null)
		{
			m_reqRespServer.close();
			m_reqRespServer = null;
		}
	}
	
	private void run() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, InterruptedException
	{
		Scanner inputReader = new Scanner(System.in);
		UserInterface userInterface = null;
		
		try
		{
			// Getting username and password
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
			
			// Instantiating the UI only after getting username and pwd
			// to avoid interruptions
			userInterface = new UserInterface(inputReader, m_commandManager);
    		userInterface.start();
			
			// The loop enables reconnection
            while (true)
            {
            	try
            	{
            		connectAndWork(factory);
            	}
	            catch (AlreadyClosedException | IOException | TimeoutException e)
				{
	            	printMsg("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
				}
            	
            	// Quit?
            	if (askForQuit())
            	{
            		printMsg("Quitting.");
            		return;
            	}
            	else
            	{
            		printMsg("Retrying connection.");
            	}
            }
		}
		finally
		{
			// TODO: The user interface will actually never exit.
			// See more information in the UserInterface class.
			if (userInterface != null)
			{
				userInterface.endThread();
			}
			
			inputReader.close();
		}
	}
	
	private void connectAndWork(ConnectionFactory factory) throws IOException, TimeoutException, InterruptedException
	{
		m_connectionLost.setValue(false);
		
		Connection connection = null;
		Channel channel = null;
		
		try
		{
			printMsg("Connecting...");
			connection = factory.newConnection();
			channel = connection.createChannel();
			
			// Listen to shutdown events from the AMQP stack
			channel.addShutdownListener(new ShutdownListener()
			{							
				@Override
				public void shutdownCompleted(ShutdownSignalException cause)
				{
					//askForReconnect.setValue(true);
					m_connectionLost.setValue(true);
				}
			});
			
			// Serve clients
			m_reqRespServer = new RequestResponseServer(channel, EXCHANGE_NAME, SERVER_TOPIC_NAME);
			m_reqRespServer.addRequestReceivedEventListener(new IRequestReceivedEventListener()
			{							
				@Override
				public void requestReceived(Object source, RequestReceivedEvent ev)
				{
					sendResponseToClient(m_reqRespServer, ev);
				}
			});
			
			printMsg("Awaiting requests.");
            printMsg("Give Q to quit.");
            
            // Waiting for either quit or shutdown
            while (!m_connectionLost.getValue() && !m_commandManager.quitReceived())
            {
            	Thread.sleep(200);
            }
		}
		finally
		{
			if (channel != null) channel.close();
			if (connection != null) connection.close();
			
			// Set to null; presumably, garbage collection can now take the object
			m_reqRespServer = null;
		}
	}
	
	private void sendResponseToClient(RequestResponseServer reqRespServer, RequestReceivedEvent ev)
	{
        try
        {
            // Printing the request
            String messageIn = new String(ev.getMessage(), StandardCharsets.UTF_8);
            printMsg("Got message \"" + messageIn + "\"");

            // Creating a response
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
	
	private boolean askForQuit() throws InterruptedException
    {
		if (m_commandManager.quitReceived())
		{
			return true;
		}
		
		printMsg("The broker is unreachable. Reconnect (y/n)?");
        
        while (true)
        {
	        // Processing user input
	        CommandManager.CommandType command = m_commandManager.getAndAckCommand();
	    	
	        switch (command)
	        {
	        case No:
	        case Quit:
	        	return true;
	        
	        case Yes:
	        	return false;
	        	
			default:
	        	break; // Break the switch and wait for another answer
	        }
	        
	        Thread.sleep(100);
        }
    }
	
	private String buildHostUrl(String host, String user, String pwd)
	{
		final int port = 5671;
		return String.format("amqps://%s:%s@%s:%d", user, pwd, host, port);
	}
	
	private void printMsg(String msg)
	{
		System.out.println(msg);
	}
}
