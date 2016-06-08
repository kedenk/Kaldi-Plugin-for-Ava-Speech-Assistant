package com.stt.executer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResultListener {
	
	final Logger log = LogManager.getLogger(ResultListener.class);

	private String resultString = null; 
	
	public ResultListener() {
	}
	
	public synchronized void addResultString( String result ) {
		if( result == null )
			return; 
		
		this.resultString = result; 
		log.debug("Parameter 'result = " + result + "' is set in ResultListener class.");
		this.notify();
	}
	
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
