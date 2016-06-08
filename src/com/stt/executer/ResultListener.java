package com.stt.executer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class holds the actual requested utterance by a speech recognition class, if it was set. 
 * 
 * @author Kevin
 * @since 2016-06-05
 * @version 1
 */
public class ResultListener {
	
	final Logger log = LogManager.getLogger(ResultListener.class);

	private String resultString = null; 
	
	public ResultListener() {
	}
	
	/**
	 * This method adds the utterance to the resultString. 
	 * 
	 * @param result String
	 */
	public synchronized void addResultString( String result ) {
		if( result == null )
			return; 
		
		this.resultString = result; 
		log.debug("Parameter 'result = " + result + "' is set in ResultListener class.");
		this.notify();
	}
	
	/**
	 * Returns the current resultString. 
	 * 
	 * @return String
	 */
	public synchronized String getResultString() {
		try {
			log.debug("ResultListener class will wait for a requested utterance.");
			this.wait();
		} catch (InterruptedException e) {
			log.catching(Level.DEBUG, e); 
		} 
		return this.resultString; 
	}
}
