/* Copyright (c) 2014 OpenBCI
 * See the file license.txt for copying permission.
 * */

package com.openbci.BLE;

public class FormatDataForFile {
	
	//Helper function to convert byte array to a string of hex values
	public static String convertBytesToHex(byte[] input){
		StringBuilder sb = new StringBuilder();
		int len = input.length;
		
		for(int i=0;i<len-1;i++){
			sb.append(String.format("%02X", input[i]));
			sb.append(",");
		}
		sb.append(String.format("%02X", input[len-1]));
		
		return sb.toString();
	}
	
	//Helper function to convert float array to comma separated string
	public static String convertFloatArrayToString(float[] input){
		StringBuilder sb = new StringBuilder();
		int len = input.length;
		
		for(int i=0;i<len;i++){
			sb.append(Float.toString(input[i]));
			sb.append(", ");
		}
		
		return sb.toString();
	}
}
