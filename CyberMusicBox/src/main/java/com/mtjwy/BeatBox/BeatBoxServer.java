package com.mtjwy.BeatBox;

import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class BeatBoxServer {
	ArrayList<ObjectOutputStream> clientOutputStreams;
	
	public class ClientHandler implements Runnable {

		public void run() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public void go() {
		
	}
	
	public void tellEveryone(Object one, Object two) {
		
	}
	
	public static void main(String[] args) {
		new BeatBoxServer().go();
	}
}
