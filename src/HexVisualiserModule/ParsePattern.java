package HexVisualiserModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author benho
 */
public class ParsePattern {
    ArrayList<PatternObject> PatternObjects = new ArrayList<PatternObject>();//holds the list of objects 
    
    public ParsePattern(File pattern){//constructor 
        
        //as patern is parsed through objects should be created and added, these objects can then be used to populate other windows
        //create a loop which adds information to a pattern object, once loop is completed the object is fully made and added to Arraylist
        PatternObject current = new PatternObject();//fresh object within loop
        String regex = "[,\\.\\s]";//splits on commas, fullstops and whitespace \\ is doubled
        String[] commentPrefix = {"#", "/*", "//"};
        boolean commentCheck = false;
        
        try{
            Scanner parser = new Scanner(pattern);
            while (parser.hasNextLine()) {
                String line = parser.nextLine();
                for (String a : commentPrefix){
                    if(line.trim().startsWith(a)){
                        commentCheck=true;
                    }
                }
                if (line.trim().isEmpty() || commentCheck) { //filters out unnecesscary lines
                    continue;
                }
                String[] fileContent = line.trim().split("\\s+");//splits the given line into its key words, to process 
                for (String s : fileContent) {
                    System.out.println(s);
                }
                //use pattern language to decide if a new object needs to be added or if a previous one needs editing
            }
            //use if statements to tell what each line should do?
            PatternObjects.add(current);
        }
        catch(Exception e){}   
    }
}

class PatternObject {//these objects contain the information to populate displays following the pattern file
    String name;
    String start;
    String end;
    int size;
    String type;
    String value;
}
