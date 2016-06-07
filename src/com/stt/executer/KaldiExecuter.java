package com.stt.executer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ava.util.PropertiesFileLoader;

public class KaldiExecuter {

	final Logger log = LogManager.getLogger(KaldiExecuter.class);
	
	private String kaldi_root = null;
	private String program; 
	private String ac_model = null; 
	
	private Runtime runtime; 
	private Process process; 
	
	private BufferedReader stdInput; 
	private BufferedReader stdError;
	
	public KaldiExecuter(String kaldi_root, String program, String ac_model_type, String ac_model ) throws Exception {
		this.kaldi_root = kaldi_root; 
		this.program = this.kaldi_root + "/src/onlinebin/" + program; 
		this.ac_model = ac_model + "/" + ac_model_type; 
		
		this.startKaldiProgram();

	}
	
	
	public KaldiExecuter( PropertiesFileLoader fileLoader ) throws Exception {
		
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
		
		this.startKaldiProgram(); 
	}
	
	
	public void startKaldiProgram() throws IOException {
		
		String model = this.ac_model + "/model"; 
		String fst = this.ac_model + "/HCLG.fst"; 
		String words = this.ac_model + "/words.txt"; 
		String sils = "1:2:3:4:5"; 
		
		
		this.runtime = Runtime.getRuntime();
		String[] commands = {this.program, "--acoustic-scale=0.3", "--rt-min=0.3", "--rt-max=0.6", "--beam=14", "--max-active=4000", model, fst, words, sils};
		this.process = runtime.exec(commands);

		this.stdInput = new BufferedReader(new 
		     InputStreamReader(this.process.getInputStream()));

		this.stdError = new BufferedReader(new 
		     InputStreamReader(this.process.getErrorStream()));

		// read the output from the command
		System.out.println("Here is the standard output of the command:\n");
		String s = null;
		while ((s = this.stdInput.readLine()) != null) {
		    System.out.println(s);
		}

		// read any errors from the attempted command
		System.out.println("Here is the standard error of the command (if any):\n");
		while ((s = this.stdError.readLine()) != null) {
		    System.out.println(s);
		}
	}
	
	public void stopRuntime() {
		
	}
}
