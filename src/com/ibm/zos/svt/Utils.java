package com.ibm.zos.svt;

/**
 * The class that contains utility methods. 
 * @author Stone WANG
 *
 */
public class Utils {
	/**
	 * Calculate the digits count of specific integer
	 * @param integer The integer to calculate
	 * @return	The digits count
	 */
	public static int getDigits(int integer) {
		int digits = 0;
		int remain = integer;
		while(remain > 0 ) {
			remain /= 10;
			++digits;
		}
		return digits;
	}
	
	/**
	 * Get the size in bytes from input size string with units "kb","mb","gb".
	 * Size must be larger than 0.
	 * @param str The input size string
	 * @return The size in bytes
	 */
	public static long getSize(String str) throws NumberFormatException{
		String[] units = {"kb", "mb", "gb", "tb"};
		String sizeStr = str;
		String unit = "byte";
		long multiple = 1;
		long size = -1;
		for(int i = 0 ; i < units.length; i++) {
			multiple *= 1024;
			if(str.toLowerCase().contains(units[i])) {
				sizeStr = str.substring(0, str.toLowerCase().indexOf(units[i]));
				unit = units[i];
				break;
			}
		}
		double tmpSize = Double.parseDouble(sizeStr);
		if(!unit.equals("byte")) 
			size = (long)(tmpSize *multiple);
		else
			size = (long)(tmpSize); 	
		return size;
	}
}
