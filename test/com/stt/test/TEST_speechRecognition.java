package com.stt.test;

import com.stt.PluginEntry;

public class TEST_speechRecognition {

	public static void main(String[] args) {
		
		System.out.println("Starting test main...");
		
		PluginEntry entry = new PluginEntry(); 
		
		entry.start();

		System.out.println("Test main terminats.");
	}

}
