package com.stt.executer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ava.pluginengine.PluginState;

public class KaldiPluginState {

	final static Logger log = LogManager.getLogger(KaldiPluginState.class);
	
	private static KaldiPluginState instance = new KaldiPluginState(); 
	
	private static PluginState state = PluginState.STOPPED; 
	
	private KaldiPluginState() {
		
	}
	
	public static KaldiPluginState getInstance() { return instance; }
	
	public static void setPluginState( PluginState pState ) {
		log.debug("Kaldi Plugin state is set to: " + pState.name());
		state = pState; 
	}
	
	public static PluginState getPluginState() {
		return state; 
	}
}
