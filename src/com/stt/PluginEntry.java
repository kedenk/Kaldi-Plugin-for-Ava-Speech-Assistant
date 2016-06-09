package com.stt;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ava.pluginengine.PluginState;
import org.ava.pluginengine.STTPlugin;
import org.ava.util.PropertiesFileLoader;
import com.stt.executer.KaldiExecuter;
import com.stt.executer.KaldiPluginState;
import com.stt.executer.ResultListener;


/**
 * The <code>PluginEntry</code> implements a STTPlugin interface of the speech assistant Ava.
 * The class contains methods to initialize, start, interrupt, continuou and stop the recognizer.
 *
 * @author Kevin
 * @since 2016-06-05
 * @version 1
 */
public class PluginEntry implements STTPlugin {

	final Logger log = LogManager.getLogger(PluginEntry.class);

	private final int WAIT_FOR_THREAD_TERMINATION = 1500;

	private KaldiExecuter SR_EXEC = null;
	private Thread asr_thread = null;

	private ResultListener listener = null;

	private java.nio.file.Path CONFIG_PATH;
	private String CONFIG_NAME = "sttPlugin.properties";

	@Override
	public void start() {
		log.info("Starting Kaldi Speech Recognition Plugin.");

		try {
		    java.nio.file.Path basePath = new File(PluginEntry.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath().getParent();
		    CONFIG_PATH = Paths.get(basePath.toString(), "/res/" + this.CONFIG_NAME);
		} catch (URISyntaxException e) {
		    log.error("Error while creating the kaldi configuration file path: " + e.getMessage());
		    log.catching(Level.DEBUG, e);
		}

		PropertiesFileLoader fileLoader = new PropertiesFileLoader(CONFIG_PATH);
		if( !fileLoader.readPropertiesFile() )	{
			log.fatal("Error while loading the STTPlugin properties file. Plugin can't be started.");
			return;
		}

		try {

			this.SR_EXEC = new KaldiExecuter( fileLoader );
			this.asr_thread = new Thread(this.SR_EXEC);
			this.asr_thread.setName("Kaldi ASR Thread");
			this.asr_thread.start();

		} catch (Exception e) {
			log.fatal("An error occured while executing the Kaldi ASR Executer.");
			log.catching(Level.DEBUG, e);
		}
	}

	@Override
	public void stop() {
		log.info("Try to stop the Kaldi ASR Plugin. ");
		try {
			this.SR_EXEC.prepareForShutdown();

			if( this.SR_EXEC != null && this.asr_thread != null ) {
				this.SR_EXEC.stopRuntime();

				this.asr_thread.join(WAIT_FOR_THREAD_TERMINATION);

				this.SR_EXEC = null;
				this.asr_thread = null;
				KaldiPluginState.setPluginState(PluginState.STOPPED);
			}

		} catch (IOException | InterruptedException e) {
			log.error("An error occured while terminating the Kaldi ASR Executer.");
			log.catching(Level.DEBUG, e);
		}

		log.info("Kaldi ASR Plugin stopped");
	}

	@Override
	public void continueExecution() {
		log.info("Continue speech recognition.");
		if( this.SR_EXEC == null ) {
			log.error("Speech recognition plugin is not started. Can't continue recognition.");
			return;
		}

		if( KaldiPluginState.getPluginState() == PluginState.RUNNING ) {
			log.debug("Can't continue speech recognition. Recognizer is already running.");
			return;
		}

		if( this.asr_thread != null ) {
			if( KaldiPluginState.getPluginState() == PluginState.INTERRUPTED ) {
				KaldiPluginState.setPluginState(PluginState.RUNNING);
				this.asr_thread.notify();
			}
		}
	}

	@Override
	public void interruptExecution() {
		log.info("Speech recognizer will be interrupted.");
		if( this.SR_EXEC == null ) {
			log.error("Speech recognition plugin is not started. Can't interrupt recognition.");
			return;
		}

		if( KaldiPluginState.getPluginState() == PluginState.INTERRUPTED ) {
			log.debug("Can't interrupt speech recognition. Recognizer is already interrupted.");
			return;
		}

		if( this.asr_thread != null ) {
			if( this.asr_thread.isAlive() ) {
				KaldiPluginState.setPluginState(PluginState.INTERRUPTED);
			}
		}

	}

	@Override
	public String requestText() {
		log.info("Trying to get requested text from asr plugin.");
		if( this.asr_thread == null || this.SR_EXEC == null ) {
			log.error("Speech recognition is not started. You can't recognize a utterance.");
			return null;
		}

		if( this.listener == null )
			this.listener = this.SR_EXEC.getResultListener();

		this.SR_EXEC.setIsRequestedResult(true);

		String requestedText = this.listener.getResultString();
		log.debug("Requested text is '" + requestedText + "'");
		return requestedText;
	}


}
