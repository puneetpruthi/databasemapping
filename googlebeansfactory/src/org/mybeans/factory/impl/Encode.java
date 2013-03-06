package org.mybeans.factory.impl;

public class Encode {
	private Encode() { }
	
	private static final int NULL_CODE    = -10;
	private static final int BOOLEAN_CODE = -11;
	private static final int DATE_CODE    = -12;
	private static final int DOUBLE_CODE  = -13;
	private static final int FLOAT_CODE   = -14;
	private static final int INT_CODE     = -15;
	private static final int LONG_CODE    = -16;
	
	private static void encodeInt(byte[] a, int code) {
		a[0] = (byte) (code >> 24);
		a[1] = (byte) (code >> 16);
		a[2] = (byte) (code >> 8);
		a[3] = (byte) (code);
	}

	public static byte[] getBooleanBytes(Boolean xObj) {
		byte[] a = new byte[5];
		encodeInt(a,BOOLEAN_CODE);
		
		boolean x = xObj;
		if (x) {
			a[4] = 1;
		} else {
			a[4] = 0;
		}

		return a;
	}
	
	public static byte[] getBytesBytes(byte[] inputBytes) {
		int    len      = inputBytes.length;
		byte[] answer   = new byte[len+4];
		encodeInt(answer,len);  // The code for arrays is just a positive length

		for (int i=0; i<len; i++) answer[i+4] = inputBytes[i];
		return answer;
		
	}
	
	public static byte[] getDateBytes(java.util.Date date) {
		byte[] a = new byte[12];
		encodeInt(a,DATE_CODE);
		
		long bits = date.getTime();
		a[4]   = (byte) (bits >> 56);
		a[5]   = (byte) (bits >> 48);
		a[6]   = (byte) (bits >> 40);
		a[7]   = (byte) (bits >> 32);
		a[8]   = (byte) (bits >> 24);
		a[9]   = (byte) (bits >> 16);
		a[10]  = (byte) (bits >> 8);
		a[11]  = (byte) (bits);
		return a;
	}
	
	public static byte[] getDoubleBytes(double x) {
		byte[] a = new byte[8];
		encodeInt(a,DOUBLE_CODE);
		
		long bits = Double.doubleToLongBits(x);
		a[4]   = (byte) (bits >> 56);
		a[5]   = (byte) (bits >> 48);
		a[6]   = (byte) (bits >> 40);
		a[7]   = (byte) (bits >> 32);
		a[8]   = (byte) (bits >> 24);
		a[9]   = (byte) (bits >> 16);
		a[10]  = (byte) (bits >> 8);
		a[11]  = (byte) (bits);
		return a;
	}
	
	public static byte[] getFloatBytes(float x) {
		byte[] a = new byte[8];
		encodeInt(a,FLOAT_CODE);
		
		int bits = Float.floatToRawIntBits(x);
		a[4] = (byte) (bits >> 24);
		a[5] = (byte) (bits >> 16);
		a[6] = (byte) (bits >> 8);
		a[7] = (byte) (bits);
		return a;
	}
	
	public static byte[] getIntBytes(int x) {
		byte[] a = new byte[8];
		encodeInt(a,INT_CODE);
		
		a[4] = (byte) (x >> 24);
		a[5] = (byte) (x >> 16);
		a[6] = (byte) (x >> 8);
		a[7] = (byte) (x);
		return a;
	}
	
	public static byte[] getLongBytes(long x) {
		byte[] a = new byte[12];
		encodeInt(a,LONG_CODE);
		
		a[4]   = (byte) (x >> 56);
		a[5]   = (byte) (x >> 48);
		a[6]   = (byte) (x >> 40);
		a[7]   = (byte) (x >> 32);
		a[8]   = (byte) (x >> 24);
		a[9]   = (byte) (x >> 16);
		a[10]  = (byte) (x >> 8);
		a[11]  = (byte) (x);
		return a;
	}

	public static byte[] getNullBytes() {
		byte[] a = new byte[4];
		encodeInt(a,NULL_CODE);
		return a;
	}
	
	public static byte[] getStringBytes(String s) {
		byte[] sBytes = s.getBytes();
		byte[] a = new byte[sBytes.length+4];
		encodeInt(a,sBytes.length);
		for (int i=0; i<sBytes.length; i++) a[i+4] = sBytes[i];
		return a;
	}

	public static byte[] getRawIntBytes(int x) {
		byte[] a = new byte[4];
		encodeInt(a,x);
		return a;
	}
}
