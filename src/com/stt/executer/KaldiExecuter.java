package com.stt.executer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ava.eventhandling.STTEventBus;
import org.ava.eventhandling.UtteranceRecognizedEvent;
import org.ava.pluginengine.PluginState;
import org.ava.util.PropertiesFileLoader;


public class KaldiExecuter implements Runnable {

	final Logger log = LogManager.getLogger(KaldiExecuter.class);
	
	private String kaldi_root = null;
	private String program; 
	private String ac_model = null; 
	
	private Runtime runtime; 
	private Process process; 
	private ResultListener resultListener; 
	
	private BufferedReader stdInput; 
	private BufferedReader stdError;
	
	private STTEventBus eventBus; 
	
	private Boolean isClosing = false; 
	private Boolean isRequestedResult = false; 
	
	public KaldiExecuter(String kaldi_root, String program, String ac_model_type, String ac_model ) throws Exception {
		log.debug("Initializing KaldiExecuter class");
		
		this.kaldi_root = kaldi_root; 
		this.program = this.kaldi_root + "/src/onlinebin/" + program; 
		this.ac_model = ac_model + "/" + ac_model_type; 
	}
	
	
	public KaldiExecuter( PropertiesFileLoader fileLoader ) throws Exception {
		log.debug("Initializing KaldiExecuter class");
		
		ArrayList<String> neededProperties = new ArrayList<String>(); 		
		neededProperties.add("plugin.name"); 
		neededProperties.add("plugin.version"); 
		neededProperties.add("plugin.id"); 
		neededProperties.add("plugin.fqnpluginclass"); 
		neededProperties.add("KALDI_ROOT"); 
		neededProperties.add("KALDI_PROGRAM"); 
		neededProperties.add("AC_MODEL_TYPE"); 
		neededProperties.add("AC_MODEL");

		
		if( fileLoader.isPropertiesFileValid(neededProperties) != null ) 
			throw new Exception("Missing properties in properties file."); 
		
		String program = fileLoader.getPropertie("KALDI_PROGRAM");
		String ac_model_type = fileLoader.getPropertie("AC_MODEL_TYPE");
		String ac_model = fileLoader.getPropertie("AC_MODEL");
		
		this.kaldi_root = fileLoader.getPropertie("KALDI_ROOT");
		this.program = this.kaldi_root + "/src/onlinebin/" + program; 
		this.ac_model = ac_model + "/" + ac_model_type; 
	}
	
	
	private void init() {
		
		this.resultListener = new ResultListener(); 
	}

	
	@Override
	public void run() {
		
		init(); 
		try {
			
			this.startKaldiProgram();
			
		} catch (IOException e) {
			log.fatal("An error occured while executing the Kaldi ASR Executer.");
			log.catching(Level.DEBUG, e);
		} 
	}
	
	
	public void startKaldiProgram() throws IOException {
		
		log.debug("Starting Kaldi Program " + this.program);
		log.debug("Options are: "
				+ "\nkaldi_root > " + this.kaldi_root
				+ "\nprogram > " + this.program 
				+ "\nAC_Model > " + this.ac_model);
		
		this.eventBus = STTEventBus.getInstance(); 
		
		String model = this.ac_model + "/model"; 
		String fst = this.ac_model + "/HCLG.fst"; 
		String words = this.ac_model + "/words.txt"; 
		String sils = "1:2:3:4:5"; 
		
		
		this.runtime = Runtime.getRuntime();
		String[] commands = {this.program, "--acoustic-scale=0.3", "--rt-min=0.3", "--rt-max=0.6", "--beam=14", "--max-active=4000", model, fst, words, sils};
		this.process = runtime.exec(commands);
		
		KaldiPluginState.setPluginState(PluginState.RUNNING);

		this.stdInput = new BufferedReader(new 
		     InputStreamReader(this.process.getInputStream()));

		this.stdError = new BufferedReader(new 
		     InputStreamReader(this.process.getErrorStream()));

		// read the output from the command
		String hypothesis = null;
		while (!isClosing && (KaldiPluginState.getPluginState() == PluginState.RUNNING) &&(hypothesis = this.stdInput.readLine()) != null) {
		    
			if( KaldiPluginState.getPluginState() == PluginState.INTERRUPTED ) {
				try {
					this.wait();
					continue; 
				} catch (InterruptedException e) {
					log.catching(Level.DEBUG, e);
				} 
			}
			
			if( this.isRequestedResult ) {
				this.isRequestedResult = false; 
				this.resultListener.addResultString(hypothesis);
			}
			
			if( !hypothesis.equals("") && !this.isRequestedResult && !isClosing ) {
				log.debug("Recognized Utterance: " + hypothesis);
				this.eventBus.fireUtteranceRecognizedEvent(new UtteranceRecognizedEvent(hypothesis));
			}				
		}

		// read any errors from the attempted command
		String err = null; 
		while ((err = this.stdError.readLine()) != null) {
		    log.error(err); 
		}
		
		log.debug("Recognition loop terminated.");
	}
	
	public void stopRuntime() throws IOException {
		
		if( this.stdInput != null )
			this.stdInput.close();
		
		if( this.stdError != null )
			this.stdError.close();
		
		this.process.destroyForcibly(); 
		
		if( this.process.exitValue() != 0 ) {
			log.info("Kaldi ASR terminated with exit code != 0. Exit code: " + this.process.exitValue());
		}
	}
	
	public void setIsRequestedResult(Boolean b) {
		this.isRequestedResult = b; 
	}
	
	public ResultListener getResultListener() {
		return this.resultListener; 
	}
}
