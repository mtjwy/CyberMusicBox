package com.mtjwy.BeatBox;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BeatBox {
	JPanel mainPanel;
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	JFrame theFrame;
	
	JList<String> incomingList;//for incoming messages, user can SELECT a message from the list to load and play the attached beat pattern
	JTextField userMessage;
	int nextNum;
	ObjectOutputStream out;
	ObjectInputStream in;
	Map<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	Sequence mySequence = null;
	String userName;
	Vector<String> listVector = new Vector<String>();
	
	String serverIP;
	int serverPort;
	
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
			"Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
			"High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
			"Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
			"Open Hi Congo"};
	
	int[]instrumentKeys = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
	
	public static void main(String[] args) {
		new BeatBox().startUp("mtjwy");
	}
	
	//set up networking I/O, and start the reader thread
	public void startUp(String name) {
		userName = name;
		//open connection to the server
		try {
			Socket sock = new Socket(serverIP, serverPort);
			out = new ObjectOutputStream(sock.getOutputStream());
			in = new ObjectInputStream(sock.getInputStream());
			Thread remote = new Thread(new RemoteReader()); //thread for read in coming mesg
			remote.start();
		} catch (Exception e) {
			System.out.println("Couldn't connect");
		}
		setUpMidi();
		buildGUI();
	}
	
	public class RemoteReader implements Runnable {
		boolean[] checkboxState = null;
		String nameToshow = null;
		Object obj = null;
		public void run() {
			try {
				while ((obj = in.readObject()) != null) {
					System.out.println("got an object from server");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					checkboxState = (boolean[]) in.readObject();
					otherSeqsMap.put(nameToShow, checkboxState);
					listVector.add(nameToShow);
					incomingList.setListData(listVector);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	
	public void buildGUI() {
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		checkboxList = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		
		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);
		
		
        JButton savePattern = new JButton("Save Beat Pattern");  
        savePattern.addActionListener(new MySaveListener());
        buttonBox.add(savePattern);
        
        JButton restore = new JButton("Restore");     
        restore.addActionListener(new MyReadInListener());
        buttonBox.add(restore);
        
        
        JButton sendIt = new JButton("sendIt");
        sendIt.addActionListener(new MySendListener());
        buttonBox.add(sendIt);
        
        userMessage = new JTextField();
        buttonBox.add(userMessage);
        
        incomingList = new JList<String>();
        incomingList.addListSelectionListener( new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);//no data to start with
        
        
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < 16; i++) {
			nameBox.add(new Label(instrumentNames[i]));
		}
		
		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		
		theFrame.getContentPane().add(background);
		
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);
		
		for(int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		
		
		theFrame.setBounds(50, 50, 300, 300);
		theFrame.pack();
		theFrame.setVisible(true);
		
	}
	
	/**
	 * Get the Sequencer, make a Sequence, and make a Track
	 */
	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	//turn checkbox state into MIDI events, and add them to the Track
	public void buildTrackAndStart() {
		int[] trackList = null;//when item is 0, not play; when item is the key, play the key
		
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i = 0; i < 16; i++) {
			trackList = new int[16];
			
			int key = instrumentKeys[i];
			
			for (int j = 0; j < 16; j++) {
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));
				if (jc.isSelected()) {
					trackList[i] = key;
				} else {
					trackList[j] = 0;
				}
			}
			
			//for this instrument, and for all 16 beats, make events and add them to the track
			makeTracks(trackList);
			track.add(makeEvent(176, 1, 127, 0, 16));
			
		}
		
		//make sure there is a event at beat 16
		track.add(makeEvent(192, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//makes events for one instrument at a time
	public void makeTracks(int[] instrumentKeys) {
		for (int i = 0; i < 16; i++) {
			int key = instrumentKeys[i];
			if (key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}
	
	public static MidiEvent makeEvent (int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent (a, tick);
		} catch (Exception e) {
			
		}
		return event;
	}
	
	public class MyStartListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			buildTrackAndStart();
		}
		
	}
	
	public class MyStopListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			sequencer.stop();
			
		}
		
	}
	
	
	public class MyUpTempoListener implements ActionListener {
		
		//scales the sequencer's tempo, adjust +3%
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 1.03));
			
		}
		
	}
	
	public class MyDownTempoListener implements ActionListener {
		////scales the sequencer's tempo, adjust -3%
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 0.97));
			
		}
		
	}
	
	//Serializing the pattern and the message, and write them to the socket output stream
	public class MySendListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxState = new boolean[256];
			
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			
			//serialize the boolean array and meessage
			
			try {
				//FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
				//ObjectOutputStream os = new ObjectOutputStream(fileStream);
				out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
				out.writeObject(checkboxState);
				
			} catch (Exception ex) {
				System.out.println("Sorry. Could not send it to the server.");
				ex.printStackTrace();
			}
			userMessage.setText("");
			
			System.out.println("successfully serialized and send to server");
			
		}
		
	}
	
	public class MySaveListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxState = new boolean[256];
			
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			
			//serialize the boolean array 
			
			try {
				FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
				ObjectOutputStream os = new ObjectOutputStream(fileStream);
				os.writeObject(checkboxState);
			} catch (Exception ex) {
				
				ex.printStackTrace();
			}
			
			
			System.out.println("successfully serialized and store pattern");
			
		}
		
	}
	//Deserializing a pattern
	public class MyReadInListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxState = null;
			try {
				FileInputStream fileIn = new FileInputStream(new File("Checkbox.ser"));
				ObjectInputStream is = new ObjectInputStream(fileIn);
				checkboxState = (boolean[]) is.readObject();
			} catch(Exception ex) {
				ex.printStackTrace();
			}
			
			for (int i = 0; i < 256; i++) {
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (checkboxState[i]) {
					check.setSelected(true);
				} else  {
					check.setSelected(false);
				}
			}
			sequencer.stop();
			buildTrackAndStart();
			
		}
		
	}
	
	public class MyListSelectionListener implements ListSelectionListener {

		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				String selected = (String) incomingList.getSelectedValue();
				if(selected != null) {
					//now go to the map, and change the sequence
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState);
					sequencer.stop();
					buildTrackAndStart();
				}
			}
			
		}
		
	}
	private void changeSequence(boolean[] checkBoxState) { 
		for (int i = 0; i < 256; i++) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if(checkBoxState[i]) {
				check.setSelected(true);
			} else {
				check.setSelected(false);
			}
		}
		
	}
	
}























