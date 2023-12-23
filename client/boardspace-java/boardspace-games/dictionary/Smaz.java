/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package dictionary;

import lib.ByteOutputStream;

/**
 * This version by ddyer, Sep 29 2020
 * converts the compression process to use byte arrays, and skip massive garbage, creating strings
 * and helpful wrappers for them.  It compresses about 3x faster
 * 
 * Smaz java is at https://github.com/RyanAD/jsmaz/blob/master/src/main/java/com/github/icedrake/jsmaz/Smaz.java
 * 
 * based on the original C implementation https://github.com/antirez/smaz/
 *
 * @author icedrake
 */
public class Smaz {
 
    /* Compression CODEBOOK, used for compression */
	/*
	 *
	// this is the original codebook, from the C version of smaz as copied into the java version
	//
    private static final String SCODEBOOK[] = {
            "\002s,\266", "\003had\232\002leW", "\003on \216", "", "\001yS",
            "\002ma\255\002li\227", "\003or \260", "", "\002ll\230\003s t\277",
            "\004fromg\002mel", "", "\003its\332", "\001z\333", "\003ingF", "\001>\336",
            "\001 \000\003   (\002nc\344", "\002nd=\003 on\312",
            "\002ne\213\003hat\276\003re q", "", "\002ngT\003herz\004have\306\003s o\225",
            "", "\003ionk\003s a\254\002ly\352", "\003hisL\003 inN\003 be\252", "",
            "\003 fo\325\003 of \003 ha\311", "", "\002of\005",
            "\003 co\241\002no\267\003 ma\370", "", "", "\003 cl\356\003enta\003 an7",
            "\002ns\300\001\"e", "\003n t\217\002ntP\003s, \205",
            "\002pe\320\003 we\351\002om\223", "\002on\037", "", "\002y G", "\003 wa\271",
            "\003 re\321\002or*", "", "\002=\"\251\002ot\337", "\003forD\002ou[",
            "\003 toR", "\003 th\r", "\003 it\366",
            "\003but\261\002ra\202\003 wi\363\002</\361", "\003 wh\237", "\002  4",
            "\003nd ?", "\002re!", "", "\003ng c", "",
            "\003ly \307\003ass\323\001a\004\002rir", "", "", "", "\002se_", "\003of \"",
            "\003div\364\002ros\003ere\240", "", "\002ta\310\001bZ\002si\324", "",
            "\003and\u0007\002rs\335", "\002rt\362", "\002teE", "\003ati\316", "\002so\263",
            "\002th\021", "\002tiJ\001c\034\003allp", "\003ate\345", "\002ss\246",
            "\002stM", "", "\002><\346", "\002to\024", "\003arew", "\001d\030",
            "\002tr\303", "", "\001\n1\003 a \222", "\003f tv\002veo", "\002un\340", "",
            "\003e o\242", "\002a \243\002wa\326\001e\002", "\002ur\226\003e a\274",
            "\002us\244\003\n\r\n\247", "\002ut\304\003e c\373", "\002we\221", "", "",
            "\002wh\302", "\001f,", "", "", "", "\003d t\206", "", "", "\003th \343",
            "\001g;", "", "", "\001\r9\003e s\265", "\003e t\234", "", "\003to Y",
            "\003e\r\n\236", "\002d \036\001h\022", "", "\001,Q", "\002 a\031", "\002 b^",
            "\002\r\n\025\002 cI", "\002 d\245", "\002 e\253", "\002 fh\001i\b\002e \013",
            "", "\002 hU\001-\314", "\002 i8", "", "", "\002 l\315", "\002 m{",
            "\002f :\002 n\354", "\002 o\035", "\002 p}\001.n\003\r\n\r\250", "",
            "\002 r\275", "\002 s>", "\002 t\016", "", "\002g \235\005which+\003whi\367",
            "\002 w5", "\001/\305", "\003as \214", "\003at \207", "", "\003who\331", "",
            "\001l\026\002h \212", "", "\002, $", "", "\004withV", "", "", "", "\001m-", "",
            "", "\002ac\357", "\002ad\350", "\003TheH", "", "", "\004this\233\001n\t",
            "", "\002. y", "", "\002alX\003e, \365", "\003tio\215\002be\\",
            "\002an\032\003ver\347", "", "\004that0\003tha\313\001o\006", "\003was2",
            "\002arO", "\002as.", "\002at'\003the\001\004they\200\005there\322\005theird",
            "\002ce\210", "\004were]", "", "\002ch\231\002l \264\001p<", "", "",
            "\003one\256", "", "\003he \023\002dej", "\003ter\270", "\002cou", "",
            "\002by\177\002di\201\002eax", "", "\002ec\327", "\002edB", "\002ee\353", "",
            "", "\001r\f\002n )", "", "", "", "\002el\262", "", "\003in i\002en3", "",
            "\002o `\001s\n", "", "\002er\033", "\003is t\002es6", "", "\002ge\371",
            "\004.com\375", "\002fo\334\003our\330", "\003ch \301\001t\003", "\002hab", "",
            "\003men\374", "", "\002he\020", "", "", "\001u&", "\002hif", "",
            "\003not\204\002ic\203", "\003ed @\002id\355", "", "", "\002ho\273",
            "\002r K\001vm", "", "", "", "\003t t\257\002il\360", "\002im\342",
            "\003en \317\002in\017", "\002io\220", "\002s \027\001wA", "", "\003er |",
            "\003es ~\002is%", "\002it/", "", "\002iv\272", "",
            "\002t #\u0007http://C\001x\372", "\002la\211", "\001<\341", "\003, a\224"
    };
    static final byte[][] CODEBOOK2 = new byte[SCODEBOOK.length][];
    static {
    	//
    	// convert the original codebook to use arrays of bytes
    	// also create a string to print and put in the sources
    	//
    	{	StringBuffer out = new StringBuffer();
    		out.append("byte [][]CODEBOOK = new byte[][] {\n");
    		for(int i=0;i<SCODEBOOK.length;i++)
    		{ String from = SCODEBOOK[i];
    		  out.append("{");
    	      byte to[] = new byte[from.length()];
    	      for(int j=0;j<to.length;j++) 
    	      	{ to[j] = (byte)from.charAt(j);
    	      	  out.append(to[j]);
    	      	  out.append(",");
    	      	}
    	      out.append("},");
    	      if(i%10==5) {out.append("\n");}
    		  CODEBOOK2[i] = to;
			} 
    		out.append("};\n");
    	System.out.println(out.toString());
    	}
    }
    */
    static byte [][]CODEBOOK = new byte[][] {
    	{2,115,44,-74,},{3,104,97,100,-102,2,108,101,87,},{3,111,110,32,-114,},{},{1,121,83,},{2,109,97,-83,2,108,105,-105,},
    	{3,111,114,32,-80,},{},{2,108,108,-104,3,115,32,116,-65,},{4,102,114,111,109,103,2,109,101,108,},{},{3,105,116,115,-38,},{1,122,-37,},{3,105,110,103,70,},{1,62,-34,},{1,32,0,3,32,32,32,40,2,110,99,-28,},
    	{2,110,100,61,3,32,111,110,-54,},{2,110,101,-117,3,104,97,116,-66,3,114,101,32,113,},{},{2,110,103,84,3,104,101,114,122,4,104,97,118,101,-58,3,115,32,111,-107,},{},{3,105,111,110,107,3,115,32,97,-84,2,108,121,-22,},{3,104,105,115,76,3,32,105,110,78,3,32,98,101,-86,},{},{3,32,102,111,-43,3,32,111,102,32,3,32,104,97,-55,},{},
    	{2,111,102,5,},{3,32,99,111,-95,2,110,111,-73,3,32,109,97,-8,},{},{},{3,32,99,108,-18,3,101,110,116,97,3,32,97,110,55,},{2,110,115,-64,1,34,101,},{3,110,32,116,-113,2,110,116,80,3,115,44,32,-123,},{2,112,101,-48,3,32,119,101,-23,2,111,109,-109,},{2,111,110,31,},{},
    	{2,121,32,71,},{3,32,119,97,-71,},{3,32,114,101,-47,2,111,114,42,},{},{2,61,34,-87,2,111,116,-33,},{3,102,111,114,68,2,111,117,91,},{3,32,116,111,82,},{3,32,116,104,13,},{3,32,105,116,-10,},{3,98,117,116,-79,2,114,97,-126,3,32,119,105,-13,2,60,47,-15,},
    	{3,32,119,104,-97,},{2,32,32,52,},{3,110,100,32,63,},{2,114,101,33,},{},{3,110,103,32,99,},{},{3,108,121,32,-57,3,97,115,115,-45,1,97,4,2,114,105,114,},{},{},
    	{},{2,115,101,95,},{3,111,102,32,34,},{3,100,105,118,-12,2,114,111,115,3,101,114,101,-96,},{},{2,116,97,-56,1,98,90,2,115,105,-44,},{},{3,97,110,100,7,2,114,115,-35,},{2,114,116,-14,},{2,116,101,69,},
    	{3,97,116,105,-50,},{2,115,111,-77,},{2,116,104,17,},{2,116,105,74,1,99,28,3,97,108,108,112,},{3,97,116,101,-27,},{2,115,115,-90,},{2,115,116,77,},{},{2,62,60,-26,},{2,116,111,20,},
    	{3,97,114,101,119,},{1,100,24,},{2,116,114,-61,},{},{1,10,49,3,32,97,32,-110,},{3,102,32,116,118,2,118,101,111,},{2,117,110,-32,},{},{3,101,32,111,-94,},{2,97,32,-93,2,119,97,-42,1,101,2,},
    	{2,117,114,-106,3,101,32,97,-68,},{2,117,115,-92,3,10,13,10,-89,},{2,117,116,-60,3,101,32,99,-5,},{2,119,101,-111,},{},{},{2,119,104,-62,},{1,102,44,},{},{},
    	{},{3,100,32,116,-122,},{},{},{3,116,104,32,-29,},{1,103,59,},{},{},{1,13,57,3,101,32,115,-75,},{3,101,32,116,-100,},
    	{},{3,116,111,32,89,},{3,101,13,10,-98,},{2,100,32,30,1,104,18,},{},{1,44,81,},{2,32,97,25,},{2,32,98,94,},{2,13,10,21,2,32,99,73,},{2,32,100,-91,},
    	{2,32,101,-85,},{2,32,102,104,1,105,8,2,101,32,11,},{},{2,32,104,85,1,45,-52,},{2,32,105,56,},{},{},{2,32,108,-51,},{2,32,109,123,},{2,102,32,58,2,32,110,-20,},
    	{2,32,111,29,},{2,32,112,125,1,46,110,3,13,10,13,-88,},{},{2,32,114,-67,},{2,32,115,62,},{2,32,116,14,},{},{2,103,32,-99,5,119,104,105,99,104,43,3,119,104,105,-9,},{2,32,119,53,},{1,47,-59,},
    	{3,97,115,32,-116,},{3,97,116,32,-121,},{},{3,119,104,111,-39,},{},{1,108,22,2,104,32,-118,},{},{2,44,32,36,},{},{4,119,105,116,104,86,},
    	{},{},{},{1,109,45,},{},{},{2,97,99,-17,},{2,97,100,-24,},{3,84,104,101,72,},{},
    	{},{4,116,104,105,115,-101,1,110,9,},{},{2,46,32,121,},{},{2,97,108,88,3,101,44,32,-11,},{3,116,105,111,-115,2,98,101,92,},{2,97,110,26,3,118,101,114,-25,},{},{4,116,104,97,116,48,3,116,104,97,-53,1,111,6,},
    	{3,119,97,115,50,},{2,97,114,79,},{2,97,115,46,},{2,97,116,39,3,116,104,101,1,4,116,104,101,121,-128,5,116,104,101,114,101,-46,5,116,104,101,105,114,100,},{2,99,101,-120,},{4,119,101,114,101,93,},{},{2,99,104,-103,2,108,32,-76,1,112,60,},{},{},
    	{3,111,110,101,-82,},{},{3,104,101,32,19,2,100,101,106,},{3,116,101,114,-72,},{2,99,111,117,},{},{2,98,121,127,2,100,105,-127,2,101,97,120,},{},{2,101,99,-41,},{2,101,100,66,},
    	{2,101,101,-21,},{},{},{1,114,12,2,110,32,41,},{},{},{},{2,101,108,-78,},{},{3,105,110,32,105,2,101,110,51,},
    	{},{2,111,32,96,1,115,10,},{},{2,101,114,27,},{3,105,115,32,116,2,101,115,54,},{},{2,103,101,-7,},{4,46,99,111,109,-3,},{2,102,111,-36,3,111,117,114,-40,},{3,99,104,32,-63,1,116,3,},
    	{2,104,97,98,},{},{3,109,101,110,-4,},{},{2,104,101,16,},{},{},{1,117,38,},{2,104,105,102,},{},
    	{3,110,111,116,-124,2,105,99,-125,},{3,101,100,32,64,2,105,100,-19,},{},{},{2,104,111,-69,},{2,114,32,75,1,118,109,},{},{},{},{3,116,32,116,-81,2,105,108,-16,},
    	{2,105,109,-30,},{3,101,110,32,-49,2,105,110,15,},{2,105,111,-112,},{2,115,32,23,1,119,65,},{},{3,101,114,32,124,},{3,101,115,32,126,2,105,115,37,},{2,105,116,47,},{},{2,105,118,-70,},
    	{},{2,116,32,35,7,104,116,116,112,58,47,47,67,1,120,-6,},{2,108,97,-119,},{1,60,-31,},{3,44,32,97,-108,},};

    /* Reverse compression CODEBOOK, used for decompression */
    private static final String REVERSE_CODEBOOK[] = {
            " ", "the", "e", "t", "a", "of", "o", "and", "i", "n", "s", "e ", "r", " th",
            " t", "in", "he", "th", "h", "he ", "to", "\r\n", "l", "s ", "d", " a", "an",
            "er", "c", " o", "d ", "on", " of", "re", "of ", "t ", ", ", "is", "u", "at",
            "   ", "n ", "or", "which", "f", "m", "as", "it", "that", "\n", "was", "en",
            "  ", " w", "es", " an", " i", "\r", "f ", "g", "p", "nd", " s", "nd ", "ed ",
            "w", "ed", "http://", "for", "te", "ing", "y ", "The", " c", "ti", "r ", "his",
            "st", " in", "ar", "nt", ",", " to", "y", "ng", " h", "with", "le", "al", "to ",
            "b", "ou", "be", "were", " b", "se", "o ", "ent", "ha", "ng ", "their", "\"",
            "hi", "from", " f", "in ", "de", "ion", "me", "v", ".", "ve", "all", "re ",
            "ri", "ro", "is ", "co", "f t", "are", "ea", ". ", "her", " m", "er ", " p",
            "es ", "by", "they", "di", "ra", "ic", "not", "s, ", "d t", "at ", "ce", "la",
            "h ", "ne", "as ", "tio", "on ", "n t", "io", "we", " a ", "om", ", a", "s o",
            "ur", "li", "ll", "ch", "had", "this", "e t", "g ", "e\r\n", " wh", "ere",
            " co", "e o", "a ", "us", " d", "ss", "\n\r\n", "\r\n\r", "=\"", " be", " e",
            "s a", "ma", "one", "t t", "or ", "but", "el", "so", "l ", "e s", "s,", "no",
            "ter", " wa", "iv", "ho", "e a", " r", "hat", "s t", "ns", "ch ", "wh", "tr",
            "ut", "/", "have", "ly ", "ta", " ha", " on", "tha", "-", " l", "ati", "en ",
            "pe", " re", "there", "ass", "si", " fo", "wa", "ec", "our", "who", "its", "z",
            "fo", "rs", ">", "ot", "un", "<", "im", "th ", "nc", "ate", "><", "ver", "ad",
            " we", "ly", "ee", " n", "id", " cl", "ac", "il", "</", "rt", " wi", "div",
            "e, ", " it", "whi", " ma", "ge", "x", "e c", "men", ".com"
    };
    private static boolean slotsEqual(byte[]slot,int from,int to,byte target[],int tfrom)
    {	int tlim = target.length;
    	while(from<to) { if((tfrom>=tlim) || (slot[from++]!=target[tfrom++])) { return(false); }}
    	return(true);
    }
    private static boolean slotsEqual(byte[]slot,int from,int to,ByteOutputStream target,int tfrom)
    {	int tlim = target.size();
    	while(from<to) { if((tfrom>=tlim) || (slot[from++]!=target.elementAt(tfrom++))) { return(false); }}
    	return(true);
    }

    public static byte[] compress(String inString) {
    	return compress(inString.getBytes());
    }
 /*
    public static byte[] compressOriginal(String inString) {
        confirmOnlyAscii(inString);

        StringBuilder verb = new StringBuilder();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        CharBuffer charBuffer = CharBuffer.wrap(inString);
        int inlen;

        // loop through input looking for matches in codebook
        while ((inlen = charBuffer.remaining()) > 0) {
            int h1, h2, h3;
            charBuffer.mark();
            h1 = h2 = charBuffer.get() << 3;
            if (inlen > 1) h2 += charBuffer.get();
            if (inlen > 2) {
                h3 = h2 ^ charBuffer.get();
            } else {
                h3 = 0;
            }
            charBuffer.reset();

            int j = 7;
            if (j > inlen) j = inlen;

            boolean found = false;


            // Try to lookup substrings into the codebook, starting from the
            // longer to the shorter substrings 
            for (; j > 0; j--) {
                CharBuffer slot;
                if (j == 1) {
                    slot = CharBuffer.wrap(SCODEBOOK[h1 % 241]);
                } else if (j == 2) {
                    slot = CharBuffer.wrap(SCODEBOOK[h2 % 241]);
                } else {
                    slot = CharBuffer.wrap(SCODEBOOK[h3 % 241]);
                }

                int slotLength = slot.length();
                int slotIndex = 0;
                int slotEndIndex = slotIndex + j + 1;
                while (slotLength > 0 && slotEndIndex <= slotLength) {
                    if (slot.get(slotIndex) == j && inlen >= j &&
                            slot.subSequence(slotIndex + 1, slotEndIndex).toString()
                                    .equals(charBuffer.subSequence(0, j).toString())) {
                        // Match found in codebook
                        // Add verbatim data if needed
                        if (verb.length() > 0) {
                            // output the verbatim data now
                        	String str = verb.toString();
                        	outputVerb(output, str);
                            verb.setLength(0);
                        }

                        // Add encoded data and ditch unnecessary part of input string
                        output.write(slot.get(slot.get(slotIndex) + 1 + slotIndex));
                        charBuffer.position(charBuffer.position() + j);
                        inlen -= j;
                        found = true;
                        break;
                    } else {
                        slotIndex++;
                        slotEndIndex = slotIndex + j + 1;
                    }
                }
            }

            // match not found, add to verbatim
            if (!found) {
                if (inlen > 0) {
                    inlen--;
                    String str = charBuffer.subSequence(0, 1).toString();
                    verb.append(str);
                }
                charBuffer.position(charBuffer.position() + 1);
            }

            // If the verbatim buffer is getting too long or we're at the end of the doc
            // throw the verbatim buffer to the output queue
            int verbLength = verb.length();
            if (verbLength == 256 || verbLength > 0 && inlen == 0) {
                outputVerb(output, verb.toString());
                verb.setLength(0);
            }

        }
        return output.toByteArray();
    }
    //
    // * Outputs the verbatim string to the output stream
    // *
    // * @param baos
    // * @param str
    // 
    static private void outputVerb(ByteArrayOutputStream baos, String str) {
   	 
        if (str.length() == 1) {
            baos.write(254);
            baos.write(str.toCharArray()[0]);
        } else {
            baos.write(255);
            baos.write(str.length());
            try {
                baos.write(str.getBytes());
            } catch (IOException e) {
                G.print("Error outputting verbatim data", e);
            }
        }
    }
    private static void confirmOnlyAscii(String input) {
        char[] chars = input.toCharArray();
        for (char c : chars) {
            if (c > 127) throw new IllegalArgumentException("Only ASCII can be smazed at this time");
        }
    }


*/
    private static ByteOutputStream verb = new ByteOutputStream();
    private static ByteOutputStream output = new ByteOutputStream();
    
    /**
     * Returns compressed byte array for the specified string
     *
     * @param inString
     * @return byte array
     */
    public static byte[] compress(byte[] inString) {
        confirmOnlyAscii(inString);

        verb.reset();
        output.reset();

        int limit = inString.length;
        int index = 0;
         // loop through input looking for matches in codebook
        while (index<limit) {
            int h1, h2, h3;
        
            {int tempIndex = index;
	            h1 = h2 = inString[tempIndex++] << 3;
	            if (tempIndex<limit) { h2 += inString[tempIndex++]; }
	            if (tempIndex<limit) {
	                h3 = h2 ^ inString[tempIndex++];
	            } else {
	                h3 = 0;
	            }
        	}
            int inlen = limit-index;
            int j = 7;
            if (j > inlen) j = inlen;

            boolean found = false;


            /* Try to lookup substrings into the codebook, starting from the
             * longer to the shorter substrings */
            for (; j > 0; j--) {
                byte[] slot;
                if (j == 1) {
                    slot = CODEBOOK[h1 % 241];
                } else if (j == 2) {
                    slot = CODEBOOK[h2 % 241];
                } else {
                    slot = CODEBOOK[h3 % 241];
                }

                int slotLength = slot.length;
                int slotIndex = 0;
                int slotEndIndex = slotIndex + j + 1;
                while (slotLength > 0 && slotEndIndex <= slotLength) {
                    if (slot[slotIndex] == j
                    		&& (inlen >= j) 
                    		&& slotsEqual(slot,slotIndex+1,slotEndIndex,inString,index))
                    {
                        // Match found in codebook
                        // Add verbatim data if needed
                        if (verb.size() > 0) {
                            // output the verbatim data now
                            outputVerb(output, verb);
                            verb.reset();
                        }

                        // Add encoded data and ditch unnecessary part of input string
                        int bb = slot[slot[slotIndex] + 1 + slotIndex];
                        output.write(bb);
                        index += j;
                        found = true;
                        break;
                    } else {
                        slotIndex++;
                        slotEndIndex = slotIndex + j + 1;
                    }
                }
            }

            // match not found, add to verbatim
            if (!found) {
                if (inlen > 0) {
                    verb.write(inString[index]);
                    index++;
                    inlen--;
                }
            }

            // If the verbatim buffer is getting too long or we're at the end of the doc
            // throw the verbatim buffer to the output queue
            int verbLength = verb.size();
            if (verbLength == 256 || verbLength > 0 && inlen == 0) {
                outputVerb(output, verb);
                verb.reset();
            }

        }
        return output.toByteArray();
    }

   /**
     * Returns compressed byte array for the specified string
     *
     * @param inString
     * @return byte array
     */
    public static byte[] compress(ByteOutputStream inString) {

    	verb.reset();
    	output.reset();

        int limit = inString.size();
        int index = 0;
         // loop through input looking for matches in codebook
        while (index<limit) {
            int h1, h2, h3;
        
            {int tempIndex = index;
	            h1 = h2 = inString.elementAt(tempIndex++) << 3;
	            if (tempIndex<limit) { h2 += inString.elementAt(tempIndex++); }
	            if (tempIndex<limit) {
	                h3 = h2 ^ inString.elementAt(tempIndex++);
	            } else {
	                h3 = 0;
	            }
        	}
            int inlen = limit-index;
            int j = 7;
            if (j > inlen) j = inlen;

            boolean found = false;


            /* Try to lookup substrings into the codebook, starting from the
             * longer to the shorter substrings */
            for (; j > 0; j--) {
                byte[] slot;
                if (j == 1) {
                    slot = CODEBOOK[h1 % 241];
                } else if (j == 2) {
                    slot = CODEBOOK[h2 % 241];
                } else {
                    slot = CODEBOOK[h3 % 241];
                }

                int slotLength = slot.length;
                int slotIndex = 0;
                int slotEndIndex = slotIndex + j + 1;
                while (slotLength > 0 && slotEndIndex <= slotLength) {
                    if (slot[slotIndex] == j
                    		&& (inlen >= j) 
                    		&& slotsEqual(slot,slotIndex+1,slotEndIndex,inString,index))
                    {
                        // Match found in codebook
                        // Add verbatim data if needed
                        if (verb.size() > 0) {
                            // output the verbatim data now
                            outputVerb(output, verb);
                            verb.reset();
                        }

                        // Add encoded data and ditch unnecessary part of input string
                        int bb = slot[slot[slotIndex] + 1 + slotIndex];
                        output.write(bb);
                        index += j;
                        found = true;
                        break;
                    } else {
                        slotIndex++;
                        slotEndIndex = slotIndex + j + 1;
                    }
                }
            }

            // match not found, add to verbatim
            if (!found) {
                if (inlen > 0) {
                    verb.write(inString.elementAt(index));
                    index++;
                    inlen--;
                }
            }

            // If the verbatim buffer is getting too long or we're at the end of the doc
            // throw the verbatim buffer to the output queue
            int verbLength = verb.size();
            if (verbLength == 256 || verbLength > 0 && inlen == 0) {
                outputVerb(output, verb);
                verb.reset();
            }

        }
        return output.toByteArray();
    }


    static private void outputVerb(ByteOutputStream baos, ByteOutputStream str) {
       if (str.size() == 1) {
            baos.write(254);
            baos.write(str.elementAt(0));
        } else {
            baos.write(255);
            baos.write(str.size());
                baos.write(str);
        }
    }

    private static void confirmOnlyAscii(byte input[]) {
        for (byte c : input) {
            if (c > 127 || c<=0) throw new IllegalArgumentException("Only ASCII can be smazed at this time");
        }
    }

    /**
     * Decompress byte array from compress back into String
     *
     * @param strBytes
     * @return decompressed String
     * @see Smaz#compress(String)
     */
    public static String decompress(byte[] strBytes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < strBytes.length; i++) {
            char b = (char) (0xFF & strBytes[i]);
            if (b == 254) {
                out.append((char) strBytes[++i]);
            } else if (b == 255) {
                byte length = strBytes[++i];
                for (int j = 1; j <= length; j++) {
                    out.append((char) strBytes[i + j]);
                }
                i += length;
            } else {
                int loc = (0xFF & b);
                out.append(REVERSE_CODEBOOK[loc]);
            }
        }
        return out.toString();
    }

}