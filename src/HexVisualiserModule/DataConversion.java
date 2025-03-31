/*
modified this from autopss original data conversion component to specifically return highlighted styled document based off of the MBR structure
 */
package HexVisualiserModule;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;


public class DataConversion {

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray(); 
    public static Object[] byteArrayToHex(byte[] array, int length, long arrayOffset) throws BadLocationException {
        if (array == null) {
            return null;
        } else {
            StyledDocument doc = new DefaultStyledDocument();
            Style defaultStyle = doc.getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setFontFamily(defaultStyle, "Monospaced");
            ArrayList<Style> stylelist = new ArrayList<Style>();

            Style defaultTextStyle = doc.addStyle("DefaultText", defaultStyle);
            StyleConstants.setForeground(defaultTextStyle, Color.BLACK);
            stylelist.add(defaultTextStyle);

            Style bootloaderStyle = doc.addStyle("Bootloader", defaultStyle);
            StyleConstants.setBackground(bootloaderStyle, new Color(173, 216, 230)); 
            stylelist.add(bootloaderStyle);

            Style partitionTableStyle = doc.addStyle("PartitionTable", defaultStyle);
            StyleConstants.setBackground(partitionTableStyle, new Color(152, 251, 152)); 
            stylelist.add(partitionTableStyle);

            Style bootSignatureStyle = doc.addStyle("BootSignature", defaultStyle);
            StyleConstants.setBackground(bootSignatureStyle, new Color(255, 182, 193));
            stylelist.add(bootSignatureStyle);
            
            StyledDocument styleDoc = new DefaultStyledDocument();
        
            for (Style style : stylelist) {
                String styleName = style.getName();
                styleDoc.insertString(styleDoc.getLength(), 
                    "Style: " + styleName + "\n", 
                    defaultStyle);
                styleDoc.insertString(styleDoc.getLength(), 
                    "Example Text Showing " + styleName + " Style\n\n", 
                    style);}
            
                // loop through the file in 16-byte increments 
            for (int curOffset = 0; curOffset < length; curOffset += 16) {
                int lineLen = 16;
                if (length - curOffset < 16) {
                    lineLen = length - curOffset;
                }

                doc.insertString(doc.getLength(),String.format("0x%08x: ", arrayOffset + curOffset), defaultTextStyle);
              
                for (int i = 0; i < 16; i++) {
                    if (i < lineLen) {
                        int v = array[curOffset + i] & 0xFF;
                        Style currentStyle = getStyleForLocation(doc, curOffset + i);
                        doc.insertString(doc.getLength(),String.valueOf(hexArray[v >>> 4]), currentStyle);
                        doc.insertString(doc.getLength(),String.valueOf(hexArray[v & 0x0F]), currentStyle);
                    } else {
                        doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    }

                    doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    if (i % 4 == 3) {
                        doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    }
                    if (i == 7) {
                        doc.insertString(doc.getLength(),String.valueOf("  "),null);
                    }
                }

                doc.insertString(doc.getLength(),String.valueOf("  "),null);

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
                    doc.insertString(doc.getLength(),String.valueOf(c),defaultTextStyle);
                }

                doc.insertString(doc.getLength(),String.valueOf("\n"),null);
            }
            
            return new Object[]{doc, styleDoc};
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
