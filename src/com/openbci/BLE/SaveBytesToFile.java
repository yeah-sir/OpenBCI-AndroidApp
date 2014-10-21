package com.openbci.BLE;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class SaveBytesToFile extends AsyncTask<byte[], Void, Void> {
	

	@Override
	protected Void doInBackground(byte[]... arduinoData) {
		//Get file name for current session
		String filename = new String(arduinoData[0]);
		//Store sample byte
		byte[] packet = arduinoData[1];
		//Store packet data
		byte[] packetData = Arrays.copyOfRange(packet, 1, packet.length);
		//Get the sample number
		int pNumber = packet[0] & 0xFF;
		String packetNumber = Integer.valueOf(pNumber).toString();
		//Create the file for the current session
		File directory = new File(Environment.getExternalStorageDirectory(),"OpenBCI");
		File file = new File(directory,filename);
		DataOutputStream dos;
		try {
			dos = new DataOutputStream(new FileOutputStream(file.getPath(),true));
			dos.writeChars(packetNumber);
			//dos.writeChars(": ");
			dos.writeChars(",");
			dos.writeChars(FormatDataForFile.convertBytesToHex(packetData));
			dos.write(System.getProperty("line.separator").getBytes());
			dos.close();
		} catch (FileNotFoundException e) {
			Log.e("Save to File AsyncTask", "Error finding file");
		} catch (IOException e) {
			Log.e("Save to File AsyncTask", "Error saving data");
		}
		return null;
	}

}
