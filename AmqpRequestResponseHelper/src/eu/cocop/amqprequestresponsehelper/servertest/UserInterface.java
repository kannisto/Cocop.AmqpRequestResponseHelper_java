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

import java.util.Scanner;

import eu.cocop.amqprequestresponsehelper.servertest.CommandManager.CommandType;

/**
 * As CTRL-C does not work in the Eclipse console, this class listens to "q" input from the user.
 * This also listens to other keyboard inputs.
 * @author Petri Kannisto
 */
public class UserInterface extends Thread
{
	private final Scanner m_scanner;
	private final ThreadSafeBoolean m_endThreadFlag;
	private final CommandManager m_commandManager;
	
	
	public UserInterface(Scanner sc, CommandManager comm)
	{
		m_scanner = sc;
		m_endThreadFlag = new ThreadSafeBoolean(false);
		m_commandManager = comm;
	}
	
	@Override
	public void run()
	{
		while (!m_endThreadFlag.getValue())
		{
			try
			{
				// TODO: The problem of this is that it blocks execution,
				// which is a very silly specification. Therefore, the UI thread
				// prevents program from exiting. Later: use another implementation
				// instead of the Scanner class?
				if (m_scanner.hasNextLine())
				{
					String line = m_scanner.nextLine().trim().toLowerCase();
					
					// Recognising the command
					CommandType command = CommandType.Other;
					
					if (line.equals("q"))
						command = CommandType.Quit;
					else if (line.equals("y"))
						command = CommandType.Yes;
					else if (line.equals("n"))
						command = CommandType.No;
					
					m_commandManager.setCommand(command);
				}
				
				sleep(100);
			}
			catch (InterruptedException e)
			{
				System.out.println("UI thread interrupted!");
				return;
			}
		}
	}
	
	public void endThread()
	{
		m_endThreadFlag.setValue(true);
	}
}
