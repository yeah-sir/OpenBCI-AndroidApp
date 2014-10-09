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

public class SaveBytesToFile extends AsyncTask<byte[], Integer, Integer> {

	@Override
	protected Integer doInBackground(byte[]... arduinoData) {
		byte[] packet = arduinoData[0];
		byte[] packetData = Arrays.copyOfRange(packet, 1, packet.length);
		String packetNumber = Byte.valueOf(packet[0]).toString();
		File file = new File(Environment.getExternalStorageDirectory(),"openbci.txt");
		DataOutputStream dos;
		try {
			dos = new DataOutputStream(new FileOutputStream(file.getPath(),true));
			dos.writeChars(packetNumber);
			dos.writeChars(": ");
			dos.write(packetData);
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
