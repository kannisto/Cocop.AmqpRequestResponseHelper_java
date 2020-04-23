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

/**
 * A class to deliver user commands to the application.
 * @author Petri Kannisto
 */
public class CommandManager
{
	/**
	 * Specifies a command.
	 * @author Petri Kannisto
	 *
	 */
	public enum CommandType
	{
		Other,
		Yes,
		No,
		Quit
	}
	
	private CommandType m_currentCommand = CommandType.Other;
	private boolean m_quitReceived = false;
	
	
	/**
	 * Constructor.
	 */
	public CommandManager()
	{
		// Empty ctor body
	}
	
	/**
	 * Returns and acknowledges the most recent command.
	 * @return Command.
	 */
	public synchronized CommandType getAndAckCommand()
	{
		CommandType retval = m_currentCommand;
		m_currentCommand = CommandType.Other;
		return retval;
	}
	
	/**
	 * Whether quit has been received.
	 * @return
	 */
	public synchronized boolean quitReceived()
	{
		return m_quitReceived;
	}
	
	/**
	 * Sets the command.
	 * @param ev Command.
	 */
	public synchronized void setCommand(CommandType ev)
	{
		if (ev == CommandType.Quit)
		{
			// Once set, this flag is never cleared
			m_quitReceived = true;
		}
		
		m_currentCommand = ev;
	}
}
