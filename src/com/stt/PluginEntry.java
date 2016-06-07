package com.stt;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ava.pluginengine.STTPlugin;
import org.ava.util.PropertiesFileLoader;

import com.stt.executer.KaldiExecuter;

public class PluginEntry implements STTPlugin {

	final Logger log = LogManager.getLogger(PluginEntry.class);
	
	private KaldiExecuter SR_EXEC; 
	
	private java.nio.file.Path CONFIG_PATH; 
	private String CONFIG_NAME = "sttPlugin.properties";

	@Override
	public void start() {
		log.info("Starting Kaldi Speech Recognition Plugin.");
		
		try {
		    java.nio.file.Path basePath = new File(PluginEntry.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
		    CONFIG_PATH = Paths.get(basePath.toString(), "/res/" + this.CONFIG_NAME);
		} catch (URISyntaxException e) {
		    log.error("Error while creating the spotify configuration file path: " + e.getMessage());
		    log.catching(Level.DEBUG, e);
		}
		
		PropertiesFileLoader fileLoader = new PropertiesFileLoader(CONFIG_PATH); 
		if( !fileLoader.readPropertiesFile() )	{
			log.fatal("Error while loading the STTPlugin properties file. Plugin can't be started.");
			return; 
		}
		
		try {
			this.SR_EXEC = new KaldiExecuter( fileLoader );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void continueExecution() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void interruptExecution() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String requestText() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
