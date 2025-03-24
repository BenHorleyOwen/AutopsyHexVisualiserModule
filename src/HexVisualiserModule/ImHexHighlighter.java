package HexVisualiserModule;

import java.util.*;
import java.util.regex.*;

/**
 * ImHexHighlighter - A Java utility to parse ImHex pattern files and highlight byte data
 * according to the structure definitions in the pattern file.
 */
public class ImHexHighlighter {
    private Map<String, StructureInfo> structures;
    private Map<String, String> structureColors;
    private List<String> colorPalette;
    private int currentColor;

    public ImHexHighlighter() {
        structures = new HashMap<>();
        structureColors = new HashMap<>();
        colorPalette = Arrays.asList(
            "#FF5733", // Orange-red
            "#33FF57", // Green
            "#3357FF", // Blue
            "#F3FF33", // Yellow
            "#FF33F3", // Pink
            "#33FFF3", // Cyan
            "#FF8C33", // Orange
            "#8C33FF", // Purple
            "#33FF8C", // Light green
            "#FF338C"  // Magenta
        );
        currentColor = 0;
        
    }

    /**
     * Inner class to represent a structure's information
     */
    private static class StructureInfo {
        List<FieldInfo> fields;
        int size;

        public StructureInfo() {
            fields = new ArrayList<>();
            size = 0;
        }
    }

    /**
     * Inner class to represent a field's information within a structure
     */
    private static class FieldInfo {
        String name;
        String type;
        int offset;
        int size;

        public FieldInfo(String name, String type, int offset, int size) {
            this.name = name;
            this.type = type;
            this.offset = offset;
            this.size = size;
        }
    }

    /**
     * Parse an ImHex pattern file and highlight bytes according to the patterns
     * 
     * @param hexString String of hexadecimal bytes (e.g. "01 02 03 04")
     * @param patternContent Content of the ImHex pattern file
     * @return HTML string containing the highlighted bytes
     */
    public String highlight(String hexString, String patternContent) {
        // Parse the bytes
        List<Integer> bytes = parseHexString(hexString);
        
        // Parse the pattern file
        parsePatternFile(patternContent);
        
        // Generate HTML with highlighting
        return generateHighlightedHTML(bytes);
    }

    /**
     * Parse a hex string into a list of byte values
     * 
     * @param hexString String of hexadecimal bytes
     * @return List of integer byte values
     */
    private List<Integer> parseHexString(String hexString) {
        // Remove all whitespace and split into pairs
        String cleanHex = hexString.replaceAll("\\s+", "");
        List<Integer> bytes = new ArrayList<>();
        
        for (int i = 0; i < cleanHex.length(); i += 2) {
            if (i + 1 < cleanHex.length()) {
                String byteStr = cleanHex.substring(i, i + 2);
                if (byteStr.matches("[0-9A-Fa-f]{2}")) {
                    bytes.add(Integer.parseInt(byteStr, 16));
                }
            }
        }
        
        return bytes;
    }

    /**
     * Parse an ImHex pattern file content
     * 
     * @param patternContent Content of the ImHex pattern file
     */
    private void parsePatternFile(String patternContent) {
        structures.clear();
        structureColors.clear();
        currentColor = 0;
        
        // Regular expressions for pattern parsing
        Pattern structPattern = Pattern.compile("struct\\s+(\\w+)\\s*\\{([^}]+)\\}");
        Pattern fieldPattern = Pattern.compile("\\s*(\\w+(?:\\[[^\\]]+\\])?)\\s+(\\w+)\\s*;");
        Pattern bitfieldPattern = Pattern.compile("bitfield\\s*\\{([^}]+)\\}\\s*(\\w+)\\s*;");
        Pattern bitPattern = Pattern.compile("\\s*(\\w+)\\s*:\\s*(\\d+)\\s*;");
        
        // Extract structures
        Matcher structMatcher = structPattern.matcher(patternContent);
        while (structMatcher.find()) {
            String structName = structMatcher.group(1);
            String structBody = structMatcher.group(2);
            
            if (!structures.containsKey(structName)) {
                structures.put(structName, new StructureInfo());
                
                // Assign a color to this structure
                structureColors.put(structName, colorPalette.get(currentColor % colorPalette.size()));
                currentColor++;
            }
            
            StructureInfo structInfo = structures.get(structName);
            
            // Find regular fields
            Matcher fieldMatcher = fieldPattern.matcher(structBody);
            while (fieldMatcher.find()) {
                String type = fieldMatcher.group(1);
                String name = fieldMatcher.group(2);
                
                // Determine field size (simplistic approach - can be expanded)
                int fieldSize = 1; // Default to 1 byte
                
                if (type.startsWith("u8") || type.startsWith("s8") || type.startsWith("char")) {
                    fieldSize = 1;
                } else if (type.startsWith("u16") || type.startsWith("s16")) {
                    fieldSize = 2;
                } else if (type.startsWith("u32") || type.startsWith("s32") || type.startsWith("float")) {
                    fieldSize = 4;
                } else if (type.startsWith("u64") || type.startsWith("s64") || type.startsWith("double")) {
                    fieldSize = 8;
                } else if (type.contains("[")) {
                    // Handle arrays
                    Matcher arrayMatcher = Pattern.compile("\\[(\\d+)\\]").matcher(type);
                    if (arrayMatcher.find()) {
                        int arraySize = Integer.parseInt(arrayMatcher.group(1));
                        String baseType = type.split("\\[")[0].trim();
                        
                        int baseSize = 1;
                        if (baseType.startsWith("u8") || baseType.startsWith("s8") || baseType.startsWith("char")) {
                            baseSize = 1;
                        } else if (baseType.startsWith("u16") || baseType.startsWith("s16")) {
                            baseSize = 2;
                        } else if (baseType.startsWith("u32") || baseType.startsWith("s32") || baseType.startsWith("float")) {
                            baseSize = 4;
                        } else if (baseType.startsWith("u64") || baseType.startsWith("s64") || baseType.startsWith("double")) {
                            baseSize = 8;
                        }
                        
                        fieldSize = baseSize * arraySize;
                    }
                } else if (structures.containsKey(type)) {
                    // Reference to another structure
                    fieldSize = structures.get(type).size;
                }
                
                structInfo.fields.add(new FieldInfo(name, type, structInfo.size, fieldSize));
                structInfo.size += fieldSize;
            }
            
            // Handle bitfields (basic support)
            Matcher bitfieldMatcher = bitfieldPattern.matcher(structBody);
            while (bitfieldMatcher.find()) {
                String bitfieldBody = bitfieldMatcher.group(1);
                String bitfieldName = bitfieldMatcher.group(2);
                
                int totalBits = 0;
                
                // Count total bits in the bitfield
                Matcher bitMatcher = bitPattern.matcher(bitfieldBody);
                while (bitMatcher.find()) {
                    int bitSize = Integer.parseInt(bitMatcher.group(2));
                    totalBits += bitSize;
                }
                
                // Calculate bytes needed
                int bitfieldSize = (int) Math.ceil(totalBits / 8.0);
                
                structInfo.fields.add(new FieldInfo(bitfieldName, "bitfield", structInfo.size, bitfieldSize));
                structInfo.size += bitfieldSize;
            }
        }
    }

    /**
     * Generate HTML with highlighted bytes
     * 
     * @param bytes List of byte values
     * @return HTML string with highlighted bytes
     */
    private String generateHighlightedHTML(List<Integer> bytes) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"imhex-highlighted\">\n");
        
        // Find the root structure (main)
        String rootStructName = "main";
        
        if (structures.containsKey(rootStructName)) {
            // Highlight according to the main structure
            highlightStructure(html, bytes, 0, rootStructName);
        } else {
            // If no main structure is found, just display bytes
            for (int i = 0; i < bytes.size(); i++) {
                String byteStr = String.format("%02x", bytes.get(i));
                html.append("<span class=\"byte\">").append(byteStr).append("</span> ");
            }
        }
        
        html.append("</div>\n");
        
        // Add CSS styling
        html.append("<style>\n");
        html.append("  .imhex-highlighted {\n");
        html.append("    font-family: monospace;\n");
        html.append("    line-height: 1.5;\n");
        html.append("  }\n");
        html.append("  .byte {\n");
        html.append("    padding: 2px;\n");
        html.append("    margin: 1px;\n");
        html.append("  }\n");
        html.append("  .field {\n");
        html.append("    border-radius: 3px;\n");
        html.append("    font-weight: bold;\n");
        html.append("  }\n");
        html.append("  .tooltip {\n");
        html.append("    position: relative;\n");
        html.append("    display: inline-block;\n");
        html.append("  }\n");
        html.append("  .tooltip .tooltiptext {\n");
        html.append("    visibility: hidden;\n");
        html.append("    background-color: rgba(0,0,0,0.8);\n");
        html.append("    color: white;\n");
        html.append("    text-align: center;\n");
        html.append("    padding: 5px;\n");
        html.append("    border-radius: 6px;\n");
        html.append("    position: absolute;\n");
        html.append("    z-index: 1;\n");
        html.append("    bottom: 125%;\n");
        html.append("    left: 50%;\n");
        html.append("    margin-left: -60px;\n");
        html.append("    opacity: 0;\n");
        html.append("    transition: opacity 0.3s;\n");
        html.append("  }\n");
        html.append("  .tooltip:hover .tooltiptext {\n");
        html.append("    visibility: visible;\n");
        html.append("    opacity: 1;\n");
        html.append("  }\n");
        html.append("</style>");
        
        return html.toString();
    }

    /**
     * Recursively highlight a structure
     * 
     * @param html StringBuilder to accumulate HTML
     * @param bytes List of byte values
     * @param offset Starting offset in the byte array
     * @param structName Name of the structure to highlight
     */
    private void highlightStructure(StringBuilder html, List<Integer> bytes, int offset, String structName) {
        if (!structures.containsKey(structName)) {
            return;
        }
        
        StructureInfo struct = structures.get(structName);
        
        for (FieldInfo field : struct.fields) {
            int fieldOffset = offset + field.offset;
            int fieldEnd = fieldOffset + field.size;
            
            // Skip if field is out of bounds
            if (fieldOffset >= bytes.size()) {
                continue;
            }
            
            if (structures.containsKey(field.type)) {
                // If field is another structure, recurse
                highlightStructure(html, bytes, fieldOffset, field.type);
            } else {
                // Highlight regular field
                String color = structureColors.get(structName);
                String textColor = getContrastColor(color);
                
                for (int i = fieldOffset; i < fieldEnd && i < bytes.size(); i++) {
                    String byteStr = String.format("%02x", bytes.get(i));
                    html.append("<span class=\"tooltip\">");
                    html.append("<span class=\"byte field\" style=\"background-color: ")
                        .append(color)
                        .append("; color: ")
                        .append(textColor)
                        .append(";\">")
                        .append(byteStr)
                        .append("</span>");
                    html.append("<span class=\"tooltiptext\">")
                        .append(structName)
                        .append(".")
                        .append(field.name)
                        .append(" (")
                        .append(field.type)
                        .append(")</span>");
                    html.append("</span> ");
                }
            }
        }
    }
    
    /**
     * Get a contrasting color (black or white) based on background color
     * 
     * @param colorHex The background color in hex format
     * @return Black or white depending on what contrasts better
     */
    private String getContrastColor(String colorHex) {
        // Parse the RGB values from the hex color
        int r = Integer.parseInt(colorHex.substring(1, 3), 16);
        int g = Integer.parseInt(colorHex.substring(3, 5), 16);
        int b = Integer.parseInt(colorHex.substring(5, 7), 16);
        
        // Calculate perceived brightness using the formula from WCAG
        double brightness = (r * 0.299 + g * 0.587 + b * 0.114) / 255;
        return brightness > 0.5 ? "#000000" : "#FFFFFF";
    }

//    /**
//     * Simple test method
//     */
//    public static void main(String[] args) {
//        // Example usage
//        String hexString = "01 02 03 04 05 06 07 08 09 0A 0B 0C";
//        
//        String patternExample = 
//                "struct Header {\n" +
//                "    u32 magic;\n" + 
//                "    u16 version;\n" +
//                "    u16 flags;\n" +
//                "};\n\n" +
//                "struct main {\n" +
//                "    Header header;\n" +
//                "    u32 dataSize;\n" +
//                "};";
//        
//        ImHexHighlighter highlighter = new ImHexHighlighter();
//        String html = highlighter.highlight(hexString, patternExample);
//        
//        System.out.println(html);
//    }
}
