package com.yafred.asn1.runtime;

import java.util.ArrayList;


/**
 * Utility class for BERDumper
 */
public class BERTag {
	
	public static final class Class {
		String name;
	    public static final Class CONTEXT = new Class("CONTEXT");
	    public static final Class UNIVERSAL = new Class("UNIVERSAL");
	    public static final Class APPLICATION = new Class("APPLICATION");
	    public static final Class PRIVATE = new Class("PRIVATE");
	    
	    private Class(String name) {
	    	this.name = name;
	    }
	    
	    @Override
		public String toString() {
	    	return name;
	    }
	}
	
	public static final class Form {
		String name;
	    public static final Form PRIMITIVE = new Form("PRIMITIVE");
	    public static final Form CONSTRUCTED = new Form("CONSTRUCTED");
	    
	    private Form(String name) {
	    	this.name = name;
	    }
	    
	    @Override
		public String toString() {
	    	return name;
	    }	
	}
	
	
    Class tagClass;
    int tagNumber = -1;
    Form tagForm;

    public BERTag(Class tagClass, int tagNumber, Form tagForm) {
    	this.tagClass = tagClass;
        this.tagNumber = tagNumber;
    	this.tagForm = tagForm;
    }


    /**
     * Constructs a Tag from its encoded representation
     */
    public BERTag(byte[] tag) {
        // class
        int mask = tag[0] & 0xC0;

        if (mask == 0x00) {
            tagClass = Class.UNIVERSAL;
        } else if (mask == 0x40) {
        	tagClass = Class.APPLICATION;
        } else if (mask == 0x80) {
        	tagClass = Class.CONTEXT;
        } else {
        	tagClass = Class.PRIVATE;
        }

        // form
        mask = tag[0] & 0x20;

        if (mask == 0x20) {
            tagForm = Form.CONSTRUCTED;
        } else {
        	tagForm = Form.PRIMITIVE;
        }

        // number
        mask = tag[0] & 0x1F;

        if (mask == 0x1F) { // this is a long tag

            // assertions
            if (tag.length == 1) {
                throw new RuntimeException("Malformed tag: needs more bytes");
            }

            tagNumber = 0;

            for (int i = tag.length - 1, mult = 1; i > 0; i--, mult *= 128) {
            	tagNumber += ((tag[i] & 0x7f) * mult);
            }
        }
        else {
        	tagNumber = mask;
        }
    }



    public Byte[] getByteList() {
        if (tagNumber == -1) {
            throw new RuntimeException("Number not set");
        }

        ArrayList<Byte> result = new ArrayList<Byte>();

        int int1 = ((tagForm == Form.PRIMITIVE) ? 0x00 : 0x20);

        if (tagClass == Class.UNIVERSAL) {
            int1 |= 0;
        } else if (tagClass == Class.APPLICATION) {
            int1 |= 0x40;
        } else if (tagClass == Class.CONTEXT) {
            int1 |= 0x80;
        } else if (tagClass == Class.PRIVATE) {
            int1 |= 0xc0;
        }

        if (tagNumber < 31) {
            int1 += tagNumber;
            result.add(new Byte((byte) int1));
        } else {
            int1 += 31;
            result.add(new Byte((byte) int1));

            for (int highBits = tagNumber; highBits != 0; highBits = highBits >> 7) {
                int1 = (highBits & 0x7f) | 0x80;
                result.add(1, new Byte((byte) int1));
            }

            Byte last = result.get(result.size()-1);
            result.remove(last);
            result.add(new Byte((byte) (last.byteValue() & (byte) 0x7f)));
        }

        // make an array
        return result.toArray(new Byte[0]);
    }

    public byte[] getByteArray() {
        Byte[] tagBytes = getByteList();

        byte[] array = new byte[tagBytes.length];

        for (int i = 0; i < tagBytes.length; i++) {
            array[i] = tagBytes[i].byteValue();
        }

        return array;
    }

    public String toString() {
        return tagForm.toString() + "_" + tagClass.toString() + "_" + tagNumber;
    }
}
