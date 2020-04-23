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
 * Implements a thread-safe boolean class.
 * @author Petri Kannisto
 */
class ThreadSafeBoolean
{
	private boolean m_value = false;
	
	public ThreadSafeBoolean(boolean v)
	{
		m_value = v;
	}
	
	public synchronized boolean getValue()
	{
		return m_value;
	}
	
	public synchronized void setValue(boolean v)
	{
		m_value = v;
	}
}
