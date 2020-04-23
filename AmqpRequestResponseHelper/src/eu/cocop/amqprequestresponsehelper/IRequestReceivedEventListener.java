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
 * Interface to listen for the RequestReceivedEvent.
 * @author Petri Kannisto
 */
public interface IRequestReceivedEventListener
{
	/**
	 * Event listener method.
	 * @param source Event source.
	 * @param ev Event data.
	 */
	void requestReceived(Object source, RequestReceivedEvent ev);
}
