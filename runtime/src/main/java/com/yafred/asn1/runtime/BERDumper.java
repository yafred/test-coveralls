/*
 * online: http://lapo.it/asn1js/
 */

package com.yafred.asn1.runtime;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.ArrayList;


public class BERDumper {
    static private final String INDENT = " ";

    /**
    *
    * @param hexaText
    * @return
    */
    final static String hexTable = "0123456789abcdef";
    PrintWriter writer = null;

    public BERDumper(Writer out) {
        writer = new PrintWriter(new BufferedWriter(out));
    }

    public void dump(InputStream in) throws IOException {
        dump(in, "");
        writer.flush();
    }

    private void dump(InputStream in, String indent) throws IOException {
        try {
            BERReader reader = new BERReader(in);

            while (true) {
                // dump tag
                reader.readTag();
                byte[] tag = reader.getTag();
                dumpTag(tag, indent);

                // dump length
                reader.readLength();
                dumpLength(reader.getLengthValue(), indent);

                // dump value
                byte[] value = reader.readOctetString(reader.getLengthValue());

                if ((tag[0] & 0x20) == 0x20) {
                    // constructed value
                    dump(new ByteArrayInputStream(value), indent + INDENT);
                } else {
                    dumpValue(value, indent);
                }
            }
        } catch (EOFException e) {
            // normal
        } catch (IOException ioExc) {
            writer.flush();
            throw ioExc;
        }

        if (indent.length() != 0) {
            indent = indent.substring(INDENT.length());
        }
    }

    private void dumpTag(byte[] tag, String indent) {
        writer.println(indent + "T: " + bytesToString(tag) + " (" +
            new BERTag(tag).toString() + ")");
    }

    private void dumpLength(int length, String indent) {
        writer.println(indent + "L: " + length);
    }

    private void dumpValue(byte[] value, String indent) {
        writer.println(indent + "V: " + bytesToString(value));
    }

    public static String bytesToString(byte[] buffer) {
        String text = "";

        for (int i = 0; i < buffer.length; i++) {
            String byteText = Integer.toHexString((int) buffer[i]);

            switch (byteText.length()) {
            case 1:
                byteText = "0" + byteText;

                break;

            case 2:
                break;

            default:
                byteText = byteText.substring(byteText.length() - 2,
                        byteText.length());

                break;
            }

            if (i == 0) {
                text = byteText;
            } else {
                text += (" " + byteText);
            }
        }

        return text;
    }

    static public byte[] bytesFromString(String hexaText) {
        boolean isNextHigh = true;
        hexaText = hexaText.toLowerCase();

        int[] byteAsChars = null;
        ArrayList<int[]> byteList = new ArrayList<int[]>();

        for (int i = 0; i < hexaText.length(); i++) {
            char c = hexaText.charAt(i);
            int index = hexTable.indexOf(c);

            if (index != -1) {
                if (isNextHigh) {
                    byteAsChars = new int[2];
                    byteAsChars[0] = index;
                    isNextHigh = false;
                } else {
                    byteAsChars[1] = index;
                    byteList.add(byteAsChars);
                    isNextHigh = true;
                }
            }
        }

        byte[] ret = new byte[byteList.size()];
        
        int i=0;
        for (int[]byteListItem : byteList) {
             ret[i] = (byte) ((16 * byteListItem[0]) + byteListItem[1]);
             i++;
        }

        return ret;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("src file needed");
            System.exit(1);
        }
        
        if(!new File(args[0]).exists()) {
        	// not a file, consider it is a hex string
        	String hexaText = args[0].replace('A', 'a');
        	hexaText = hexaText.replace('B', 'b');
        	hexaText = hexaText.replace('C', 'c');
        	hexaText = hexaText.replace('D', 'd');
        	hexaText = hexaText.replace('E', 'e');
        	hexaText = hexaText.replace('F', 'f');
           	InputStream input = new ByteArrayInputStream(bytesFromString(hexaText));
           	try {
				new BERDumper(new OutputStreamWriter(System.out)).dump(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        else {
	        try {
	            InputStream input = new FileInputStream(args[0]);
	            new BERDumper(new OutputStreamWriter(System.out)).dump(input);
	        } catch (EOFException eof) {
	        } catch (Exception exc) {
	            System.err.println(exc.getMessage());
	        }
        }
    }
}
