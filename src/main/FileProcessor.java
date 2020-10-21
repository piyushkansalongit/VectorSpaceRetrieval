package src.main;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import src.external.Stemmer;

public class FileProcessor {
    private static Stemmer stemmer = new Stemmer();
    private static StopWords stopWords = new StopWords();

    public static HashMap<Integer, String> docIdx2ID = new HashMap<>();
    private static Integer documentCounter = 0;
    public static int getDocumentIdx(String docID){
        FileProcessor.documentCounter += 1;
        FileProcessor.docIdx2ID.put(FileProcessor.documentCounter, docID);
        return FileProcessor.documentCounter;
    }

    private String dirPath;
    private String[] filenames;
    private HashMap<Integer, ArrayList<HashMap<String, Byte>>> documentObjects;

    public FileProcessor(String dirPath, String[] filenames, HashMap<Integer, ArrayList<HashMap<String, Byte>>> documentObjects) {
        this.dirPath = dirPath;
        this.filenames = filenames;
        this.documentObjects = documentObjects;
    }

    private Pattern patternDoc = Pattern.compile("<DOC>(.*?)</DOC>", Pattern.DOTALL);
    private Pattern patternText = Pattern.compile("(<TEXT>)(.*?)(</TEXT>)", Pattern.DOTALL);
    private Pattern patternLocations = Pattern.compile("((<location>)(.*?)(</location>)\\s*)+");
    private Pattern patternOrganizations = Pattern.compile("((<organization>)(.*?)(</organization>)\\s*)+");
    private Pattern patternPersons = Pattern.compile("((<person>)(.*?)(</person>)\\s*)+");
    private Pattern patternID = Pattern.compile("(<DOCNO>)(.*?)(</DOCNO>)", Pattern.DOTALL);

    private static ArrayList<String> __tokenizeText(String text){
        ArrayList<String> tokens = new ArrayList<>();
    
        // tokens that are acronyms
        text = Util.FindAcronyms(tokens, text);

        // Remove punctuations
        text = Util.RemovePunctuationFromText(text);

        for (String token : text.split("[\\s]+", 0)) {
            if(token.length() == 0)
                continue;
            if(!FileProcessor.stopWords.isStopWord(token)){
                String lemma = FileProcessor.stemmer.stem(token);
                tokens.add(lemma);
            }
        }
        return tokens;
    }

    // Matches, extracts and removes NER Entities from a string
    private String FindMatchingEntities(String text, Pattern pattern, ArrayList<String> tokens){
        Matcher m = pattern.matcher(text);
        while(m.find()){
            String token = m.group();
            token = token.replaceAll("<.*?>", "");
            token = token.strip().replaceAll("\\s+", " ");
            if(token.length() > 0)
                tokens.add(token);
        }
        text = m.replaceAll("");
        return text;
    }

    // Extract the <TEXT> blocks from a <DOC> block
    private String ExtractDocumentText(String document){
        Matcher m = patternText.matcher(document);
        StringBuilder documentTextBuilder = new StringBuilder(); 
        while(m.find()){
            // Iterate through each <TEXT> section in the DOC
            documentTextBuilder.append(m.group(2));
        }
        return documentTextBuilder.toString();
    }

    // Extracts the <DOCID> block from the <DOC> block
    private int ExtractDocumentID(String document){
        Matcher m = patternID.matcher(document);
        String ret = "";
        while(m.find()){
            ret = m.group(2).strip();
            break;
        }
        int idx = FileProcessor.getDocumentIdx(ret);
        return idx;
    }

    // Tokenizes a document
    private void TokenizeDoc(String text, ArrayList<HashMap<String, Byte>> tokens){
        
        text = text.toLowerCase().replaceAll("([a-z]*)([\\d]+)([a-z]*)", "");

        // Extract locations, persons, and organisations as separate token lists
        ArrayList<String> location_tokens = new ArrayList<>();
        text = FindMatchingEntities(text, patternLocations, location_tokens);
        // System.out.println(location_tokens);

        ArrayList<String> organization_tokens = new ArrayList<>();
        text = FindMatchingEntities(text, patternOrganizations, organization_tokens);
        // System.out.println(organization_tokens);

        ArrayList<String> person_tokens = new ArrayList<>();
        text = FindMatchingEntities(text, patternPersons, person_tokens);
        // System.out.println(person_tokens);

        // ArrayList<String> standard_tokens = FileProcessor.__tokenizeText(text);
        ArrayList<String> standard_tokens = FileProcessor.__tokenizeText(text);
        // System.out.println(standard_tokens);

        tokens.add(Util.List2Set(standard_tokens));
        tokens.add(Util.List2Set(location_tokens));
        tokens.add(Util.List2Set(organization_tokens));
        tokens.add(Util.List2Set(person_tokens));
    }

    public void run() {
        try{
            for (String filename : this.filenames) {
                // Iterate through the files in the directory
                String filePath = Paths.get(this.dirPath, filename).toString();
                Matcher matcherDoc = patternDoc.matcher(Util.File2Seq(filePath));
                while(matcherDoc.find()){
                    // Iterate through each <DOC> section in the file
                    String document = matcherDoc.group();
                    int documentID = ExtractDocumentID(document);
                    String documentText = ExtractDocumentText(document);
                    ArrayList<HashMap<String, Byte>> tokens = new ArrayList<>();
                    this.TokenizeDoc(documentText, tokens);
                    this.documentObjects.put(documentID, tokens);
                }
            }
        }
        catch(IOException e){
            System.err.println(e);
        }
    }
}