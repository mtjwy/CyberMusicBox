package com.mtjwy.BeatBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;



public class BeatBoxServer {
	ArrayList<ObjectOutputStream> clientOutputStreams;
	int serverPort = 4040;
	
	public class ClientHandler implements Runnable {

		ObjectInputStream in;
		Socket sock;

		public ClientHandler(Socket clientSOcket) {
			try {
				sock = clientSOcket;
				in = new ObjectInputStream(sock.getInputStream());

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		public void run() {
			Object o1 = null;
			Object o2 = null;
			try {
				while ((o1 = in.readObject()) != null) {
					o2 = in.readObject();
					System.out.println("read two objects");
					tellEveryone(o1, o2);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	public void go() {
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		try {
			ServerSocket serverSock = new ServerSocket(serverPort);
			while (true) {
				Socket clientSocket = serverSock.accept();
				ObjectOutputStream out = new ObjectOutputStream(
						clientSocket.getOutputStream());
				clientOutputStreams.add(out);

				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
				System.out.println("got a connection");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void tellEveryone(Object one, Object two) {
		Iterator<ObjectOutputStream> it = clientOutputStreams.iterator();
		while (it.hasNext()) {
			try {
				ObjectOutputStream out = it.next();
				out.writeObject(one);
				out.writeObject(two);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		new BeatBoxServer().go();
	}
}
