package com.ibm.zos.svt;

import java.util.ArrayList;

/**
 * Pair of String delimited by "="
 * @author Stone WANG
 *
 */
public class StringPair {
	private String first;
	private String second;
	
	/**
	 * Create a new StringPair
	 * @param name
	 * @param value
	 */
	public StringPair(String first, String second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * Create a new StringPair
	 * @param str	The string contains the string pair
	 * @throws Exception
	 */
	public StringPair(String str) throws Exception {
		String[] pair = str.split("[= ]+");
		if(pair == null || pair.length != 2) {
			throw new Exception("Invalid string pair:\"" + str + "\"");
		}
		this.first = pair[0];
		this.second = pair[1];
	}
	
	/**
	 * Parse input string to create a series of StringPair
	 * @param str	The input string contains the string pairs delimited by " "
	 * @param regExp1 The splitter between String Pairs
	 * @param regExp2 the splitter inside the String Pair
	 * @return	The list of StringPairs
	 * @throws Exception
	 */
	public static ArrayList<StringPair> parse(String str, String regExp1, String regExp2) throws Exception {
		String[] pairs = str.split(regExp1);
		if(pairs == null)
			return null;
		ArrayList<StringPair> pairNodes = new ArrayList<StringPair>();
		for(int i=0; i<pairs.length; i++) {
			pairs[i] = pairs[i].trim();
			//Empty token
			if(pairs[i].equals(""))
				continue;
			String[] pair = pairs[i].split(regExp2);
			if(pair == null || pair.length != 2) {
				throw new Exception("Invalid string pair:\"" + str + "\"");
			}
			pairNodes.add(new StringPair(pair[0], pair[1]));
		}
		return pairNodes;
	}
	
	/**
	 * Get the first part of the string pair
	 * @return	The first part of the string pair
	 */
	public String getFirst() {
		return first;
	}
	
	/**
	 * Get the second part of the string pair
	 * @return	The second part of the string pair
	 */
	public String getSecond() {
		return second;
	}
	
	/**
	 * Convert StringPair to String
	 */
	public String toString() {
		return first + "=" + second;
	}
}