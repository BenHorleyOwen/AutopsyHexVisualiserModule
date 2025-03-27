/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

modified this to specifically return highlighted styled document based off of the MBR structure
 */
package HexVisualiserModule;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * Helper methods for converting data.
 */
public class DataConversion {

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray(); //NON-NLS
    //a style-context/attribute-set has to be made for each of the different parts of the mbr which would be highlighted, these sets will also be used for the key section
    

    /**
     * Return the hex-dump layout of the passed in byte array.
     *
     * @param array       Data to display
     * @param length      Amount of data in array to display
     * @param arrayOffset Offset of where data in array begins as part of a
     *                    bigger file (used for arrayOffset column)
     *
     * @return
     */
    public static StyledDocument byteArrayToHex(byte[] array, int length, long arrayOffset) throws BadLocationException {//to modify this to highlight the text
        if (array == null) {
            return null;
        } else {
            StyledDocument doc = new DefaultStyledDocument();
            Style defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setFontFamily(defaultStyle, "Monospaced");

            // Define styles for different MBR sections
            Style defaultTextStyle = doc.addStyle("DefaultText", defaultStyle);
            StyleConstants.setForeground(defaultTextStyle, Color.BLACK);

            Style bootloaderStyle = doc.addStyle("Bootloader", defaultStyle);
            StyleConstants.setBackground(bootloaderStyle, new Color(173, 216, 230)); // Light blue

            Style partitionTableStyle = doc.addStyle("PartitionTable", defaultStyle);
            StyleConstants.setBackground(partitionTableStyle, new Color(152, 251, 152)); // Pale green

            Style bootSignatureStyle = doc.addStyle("BootSignature", defaultStyle);
            StyleConstants.setBackground(bootSignatureStyle, new Color(255, 182, 193)); // Light pink
            
            // loop through the file in 16-byte increments 
            for (int curOffset = 0; curOffset < length; curOffset += 16) {
                // how many bytes are we displaying on this line
                int lineLen = 16;
                if (length - curOffset < 16) {
                    lineLen = length - curOffset;
                }

                // print the offset column
                  //NON-NLS

                // print the hex columns                
                for (int i = 0; i < 16; i++) {//this is the loop which goes through the bytes one at a time, this is where the Jpane needs to have the attributes set based off of a counter
                    if (i < lineLen) {
                        int v = array[curOffset + i] & 0xFF;// & 0xFF is a bitwise AND operation that converts the byte to an unsigned integer
                        Style currentStyle = getStyleForLocation(doc, curOffset + i);
                        //once curoffset + i aligns with ta specific value the document style should be changed and then reset after the bit has been appended to the document
                        //create a variable which changes style based off of a switchcase for curoffset+i 
                        doc.insertString(doc.getLength(),String.valueOf(hexArray[v >>> 4]), currentStyle);
                        doc.insertString(doc.getLength(),String.valueOf(hexArray[v & 0x0F]), currentStyle);
                    } else {
                        doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    }

                    // controls the seperation of bytes, previous author had unused code in here to be stripped out
                    doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    if (i % 4 == 3) {
                        doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    }
                    if (i == 7) {
                        doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    }
                }

                doc.insertString(doc.getLength(),String.valueOf("  "),null);
                //this can be ignored for me purposes
                // print the ascii columns
                String ascii = new String(array, curOffset, lineLen, java.nio.charset.StandardCharsets.US_ASCII);
                for (int i = 0; i < 16; i++) {
                    char c = ' ';
                    if (i < ascii.length()) {
                        c = ascii.charAt(i);
                        int dec = (int) c;

                        if (dec < 32 || dec > 126) {
                            c = '.';
                        }
                    }
                    doc.insertString(doc.getLength(),String.valueOf(c),null);
                }

                doc.insertString(doc.getLength(),String.valueOf("\n"),null);
            }
            return doc;
        }
    }

    protected static String charArrayToByte(char[] array) {
        if (array == null) {
            return "";
        } else {
            String[] binary = new String[array.length];

            for (int i = 0; i < array.length; i++) {
                binary[i] = Integer.toBinaryString(array[i]);
            }
            return Arrays.toString(binary);
        }
    }
    
    private static Style getStyleForLocation(StyledDocument doc, int location) {
        // Typical MBR structure breakdown
        // 0-446 bytes: Bootloader code
        // 446-510 bytes: Partition table
        // 510-511 bytes: Boot signature
        
        if (location < 446) {
            return doc.getStyle("Bootloader");
        } else if (location >= 446 && location < 510) {
            return doc.getStyle("PartitionTable");
        } else if (location >= 510 && location < 512) {
            return doc.getStyle("BootSignature");
        }
        
        return doc.getStyle("DefaultText");
    }
}
