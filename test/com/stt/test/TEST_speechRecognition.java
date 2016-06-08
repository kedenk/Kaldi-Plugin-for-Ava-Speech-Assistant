package com.stt.test;

import com.stt.PluginEntry;

public class TEST_speechRecognition {

	public static void main(String[] args) {
		
		System.out.println("Starting test main...");
		
		PluginEntry entry = new PluginEntry(); 
		System.out.println("Starting Kaldi plugin...");
		entry.start();


		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Stopping Kaldi plugin.");
		entry.stop(); 
	}

}
