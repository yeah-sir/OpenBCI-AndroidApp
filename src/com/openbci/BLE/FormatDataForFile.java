package com.openbci.BLE;

public class FormatDataForFile {
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
}
