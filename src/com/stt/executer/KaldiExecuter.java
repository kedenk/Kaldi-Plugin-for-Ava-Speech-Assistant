package com.stt.executer;

import java.io.BufferedReader;
import java.io.File;
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

/**
 * The class KaldiExecuter can execute the Kaldi ASR online-gmm-decode-faster appliaction and implements the Runnable interface. 
 * After the execution of this class in a seperated thread the online-gmm-decode-faster will executet and provides speech recognition as text from the microphone. 
 * 
 * @author Kevin
 * @since 2016-06-05
 * @version 1
 */
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
	
	
	/**
	 * Initialize this class. 
	 */
	private void init() {
		
		this.resultListener = new ResultListener(); 
	}

	
	/**
	 * Starts the Kaldi program execution routine and is automatically executet when the thread with this runnable is started. 
	 */
	@Override
	public void run() {
		
		init(); 
		try {
			
			this.startKaldiProgram();
			
		} catch (IOException e) {
			log.fatal("An error occured while executing the Kaldi ASR Executer or starting the kaldi program.");
			log.catching(Level.DEBUG, e);
		} 
	}
	
	
	/**
	 * This method starts the online-gmm-decode-faster applikation of Kaldi ASR. 
	 * The recognized utterances will be triggered <code>eventBus.fireUtteranceRecognizedEvent</code>. 
	 * 
	 * @throws IOException
	 */
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
		
		if( !isProgramAvailable() ) {
			log.fatal("Given Kaldi program is not available or executable."); 
			return; 
		}
		
		this.runtime = Runtime.getRuntime();
		String[] commands = {this.program, "--acoustic-scale=0.3", "--rt-min=0.3", "--rt-max=0.6", "--beam=14", "--max-active=4000", model, fst, words, sils, ac_model + "/matrix"};
		this.process = runtime.exec(commands);
		
		KaldiPluginState.setPluginState(PluginState.RUNNING);

		this.stdInput = new BufferedReader(new 
		     InputStreamReader(this.process.getInputStream()));

		this.stdError = new BufferedReader(new 
		     InputStreamReader(this.process.getErrorStream()));

		// read the output from the command
		try {
			if( this.stdInput != null ) {
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
			}
		}
		catch(Exception e) {
			if( !isClosing ) {
				throw e; 
			}
		}

			
		// read any errors from the attempted command
		if( this.stdError != null ) {
			String err = null; 
			while ((err = this.stdError.readLine()) != null && !isClosing) {
			    log.error(err); 
			}
		}
		
		log.debug("Recognition loop terminated.");
	}
	
	
	/**
	 * <code>stopRuntime</code> stops the process with the online-gmm-decode-faster. 
	 * 
	 * @throws IOException
	 */
	public void stopRuntime() throws IOException {
			
		if( this.stdInput != null ) {
			this.stdInput = null; 
			//this.stdInput.close();
		}
		
		if( this.stdError != null )
			//this.stdError.close();
			stdError = null; 
		
		this.process.destroy(); 
		/*
		if( this.process.exitValue() != 0 ) {
			log.info("Kaldi ASR terminated with exit code != 0. Exit code: " + this.process.exitValue());
		}
		*/
	}
	
	/**
	 * If the next recognized utterance should be put to the ResultListener the parameter b must be set. 
	 * 
	 * @param b Boolean
	 */
	public void setIsRequestedResult(Boolean b) {
		this.isRequestedResult = b; 
	}
	
	/**
	 *  Returns the ResultListener of this class. 
	 *  
	 * @return ResultListener
	 */
	public ResultListener getResultListener() {
		return this.resultListener; 
	}
	
	/**
	 * Validate the given Kaldi program, if it is available. 
	 * 
	 * @return Boolean
	 */
	private Boolean isProgramAvailable() {
		log.debug("Validate Kaldi program path");
		File p = new File(this.program); 
		
		if( p.exists() && p.canExecute() ) {
			log.debug("Kaldi program path is valid");
			return true; 
		}
		log.debug("Not a valid Kaldi program path. Path: " + this.program);
		return false; 
	}
	
	/**
	 * This methode prepares this class for shuting down. 
	 * The member variable <code>isClosing</code> will be set to true. 
	 * 
	 * This method does not shut down the given program of this class. 
	 */
	public void prepareForShutdown() {
		this.isClosing = true; 
	}
}
