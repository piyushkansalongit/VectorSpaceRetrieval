package src.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.javatuples.Pair;
import java.util.zip.GZIPOutputStream;

public class InvertedIndex {
    private static ArrayList<HashMap<String, Term>> indexTerms = new ArrayList<>();

    public static ArrayList<String> BuildVocabulary(ArrayList<HashMap<String, Byte>> tokens){

        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            indices.add(i);
        }
        Comparator<Integer> comparator = new Comparator<Integer>() {
            public int compare(Integer i, Integer j) {
                return Integer.compare(tokens.get(i).size(), tokens.get(j).size());
            }
        };
        Collections.sort(indices, comparator);
        
        Set<String> vocabulary = new HashSet<>();
        for(int i=0; i<tokens.size(); i++){
            HashMap<String, Byte> tokenPairs  = tokens.get(indices.get(i));
            vocabulary.addAll(tokenPairs.keySet());
        }
        ArrayList<String> ret = new ArrayList<String>(vocabulary);
        return ret;
    }

    public static void WriteDictAndIndex(String path){
        try{
            GZIPOutputStream postingListOutput = new GZIPOutputStream(new FileOutputStream(String.format("%s.idx", path)));
            GZIPOutputStream dictionaryOutput = new GZIPOutputStream(new FileOutputStream(String.format("%s.dict", path)));
            Integer START=0, SIZE=0, VOCAB=0;
            Integer[] breakPoints = new Integer[4];
            for(int index=0; index<4; index++){
                HashMap<String, Term> terms = InvertedIndex.indexTerms.get(index);
                for(String s : terms.keySet()){
                    ArrayList<Pair<Integer, Byte>> postingList = terms.get(s).postingList;
                    SIZE = postingList.size()*4;
    
                    // Write one posting list to the file system
                    ByteBuffer list_buffer = ByteBuffer.allocate(SIZE);
                    int totalCount = 0;
                    for(Pair<Integer, Byte> p : postingList) {
                        // Write the docID (3 bytes)
                        byte[] docID = ByteBuffer.allocate(4).putInt(p.getValue0()).array();
                        for (int i=1; i<4; i++) {
                            list_buffer.put(docID[i]);
                        }
    
                        // Write the term frequency (1 byte)
                        byte termFrequency = p.getValue1();
                        list_buffer.put(termFrequency);

                        // Counter for the total occurrences of words
                        totalCount += (int)termFrequency;
                    }
                    if(index == 0 && totalCount == 1)
                        continue;
                    postingListOutput.write(list_buffer.array());
    
                    // Write one dictionary entry to the file system
                    byte[] token = s.getBytes();
                    ByteBuffer dict_buffer = ByteBuffer.allocate(12+token.length);
                    dict_buffer.putInt(token.length);
                    dict_buffer.put(token);
                    dict_buffer.putInt(terms.get(s).documentFrequency);
                    dict_buffer.putInt(START);
                    dictionaryOutput.write(dict_buffer.array());
    
                    START += SIZE;
                    VOCAB += 1;
                }
                breakPoints[index] = VOCAB;
            }
            postingListOutput.close();

            //INFO
            System.out.println(String.format("Vocabulary Size: %d", VOCAB));
            
            // Write the dictionary divisions to the dictionary file
            ByteBuffer dict_buffer = ByteBuffer.allocate(20);
            dict_buffer.putInt(0);
            for(int i=0; i<4; i++){
                dict_buffer.putInt(breakPoints[i]);
            }
            dictionaryOutput.write(dict_buffer.array());

            // Write the meta to the dictionary file
            for(Integer id : FileProcessor.docIdx2ID.keySet()){
                byte[] docID = FileProcessor.docIdx2ID.get(id).getBytes();
                ByteBuffer buffer = ByteBuffer.allocate(docID.length+8);
                buffer.putInt(docID.length);
                buffer.put(docID);
                buffer.putInt(id);
                dictionaryOutput.write(buffer.array());
            }

            dictionaryOutput.close();
        }catch(IOException e){
            System.err.println(e);
        }
    }


    public static void BuildDictAndIndex(HashMap<Integer, ArrayList<HashMap<String, Byte>>> documentObjects, int index, StopWords stopWords){
        ArrayList<HashMap<String, Byte>> tokens = new ArrayList<>();
        HashMap<String, Term> terms = new HashMap<>();
        // Find the vocabulary
        ArrayList<String> vocabulary;
        for(Integer docID : documentObjects.keySet()) {
            tokens.add(documentObjects.get(docID).get(index));
        }
        vocabulary = BuildVocabulary(tokens);

        for(String token : vocabulary){
            if(!stopWords.isStopWord(token)){
                terms.put(token, new Term());
            }
        }

        for(int i=1; i<=documentObjects.keySet().size(); i++){
            HashMap<String, Byte> documentObject = documentObjects.get(i).get(index);
            for(String token: documentObject.keySet()){
                if(!stopWords.isStopWord(token)){
                    terms.get(token).update(i, documentObject.get(token));
                }
            }
        }
        
        // Write the dictionary and inverted index to file system
        InvertedIndex.indexTerms.add(terms);
    }
    
    public static void main(String[] args) {
        File f = new File(args[0]);
        Path currentPath = Paths.get(System.getProperty("user.dir"));
        String[] filenames = f.list();
        HashMap<Integer, ArrayList<HashMap<String, Byte>>> documentObjects = new HashMap<>();

        //INFO
        System.out.println("Starting to read documents...");
        new FileProcessor(currentPath, f.getPath(), filenames, documentObjects).run();
        //INFO
        System.out.println(String.format("Number of Documents: %d", documentObjects.size()));

        //Build 4 different inverted indexes
        StopWords stopWords = new StopWords();
        for(int i=0; i<4; i++){
            // Write a dictionary of the format:
            //      word:df:offset1
            // Write posting lists using the format:
            //      <id:tf><id:tf>...<id:tf>
            BuildDictAndIndex(documentObjects, i, stopWords);
        }

        //INFO
        System.out.println("Saving the index to file system...");
        WriteDictAndIndex(args[1]);
    }
}