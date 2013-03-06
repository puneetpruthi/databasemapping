/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

public class HexHelpers {
	private final static char[] hexDigits = new char[] {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};

	public static String bytesToHex(byte[] bytes) {
		StringBuffer b = new StringBuffer();
		for (int i=0; i<bytes.length; i++) {
			int lowNibble = bytes[i] & 0x0f;
			int highNibble = (bytes[i]>>4) & 0x0f;
			b.append(hexDigits[highNibble]);
			b.append(hexDigits[lowNibble]);
		}
		return b.toString();
	}

	private static int hexCharToInt(char c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		throw new NumberFormatException("Non-hex char: '" + c + "'");
	}

	public static byte[] hexToBytes(String hex) {
		if (hex.length() == 0) throw new NumberFormatException("Empty string");
		if (hex.length()%2 == 1) throw new NumberFormatException("Odd number of characters (must be even): "+hex);
		byte[] answer = new byte[hex.length()/2];
		for (int i=0; i<hex.length(); i+=2) {
			char c1 = hex.charAt(i);
			char c2 = hex.charAt(i+1);
			int highNibble = hexCharToInt(c1);
			int lowNibble  = hexCharToInt(c2);
			answer[i/2] = (byte) ( (highNibble << 4) + lowNibble );
		}
		return answer;
	}

}
