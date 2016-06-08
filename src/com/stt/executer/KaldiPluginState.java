package com.stt.executer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ava.pluginengine.PluginState;

/**
 * KaldiPluginState class holds the actual state of the Kaldi ASR Plugin for Ava. 
 * This class implements a singleton pattern. 
 * 
 * @author Kevin
 * @since 2016-06-05
 * @version 1
 */
public class KaldiPluginState {

	final static Logger log = LogManager.getLogger(KaldiPluginState.class);
	
	private static KaldiPluginState instance = new KaldiPluginState(); 
	
	private static PluginState state = PluginState.STOPPED; 
	
	private KaldiPluginState() {
		
	}
	
	/**
	 * Returns the instance of this class. 
	 * 
	 * @return KaldiPluginState
	 */
	public static KaldiPluginState getInstance() { return instance; }
	
	/**
	 * With this method you can set the plugin state of the Kaldi ASR plugin.
	 * Only the variable will be set. 
	 * 
	 * @param pState PluginState
	 */
	public static void setPluginState( PluginState pState ) {
		log.debug("Kaldi Plugin state is set to: " + pState.name());
		state = pState; 
	}
	
	/**
	 * Returns the actual plugin state. 
	 * 
	 * @return PluginState
	 */
	public static PluginState getPluginState() {
		return state; 
	}
}
