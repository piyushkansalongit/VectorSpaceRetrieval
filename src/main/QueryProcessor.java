package src.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.javatuples.Pair;

public class QueryProcessor {
    private static Pattern patternQuery = Pattern.compile("(<title>[\\s]+Topic:)(.*?)(<)", Pattern.DOTALL);

    private static int numDocuments;
    private static HashMap<Integer, String> ID2DocID = new HashMap<>();
    private static HashMap<String, Integer> DocID2ID = new HashMap<>();
    private static ArrayList<HashMap<String, Pair<Integer, ArrayList<Pair<Integer, Byte>>>>> indexes = new ArrayList<>();
    private static float[] documentNormalizationScores;
    private static int[] documentTokenCounts;

    private static float __computeScore(int tf, int df) {
        return (float)(Math.log(1 + (float)QueryProcessor.numDocuments/df)*(1+Math.log(tf)));
    }
    
    private static HashMap<Integer, ArrayList<String>> __queryExpand1(String query){
        ArrayList<String> tokenizedQuery = Util.TokenizeText(query, false);
        HashMap<Integer, ArrayList<String>> expandedQuery = new HashMap<>();
        for(int i=0; i<4; i++){
            expandedQuery.put(i, new ArrayList<>());
        }

        int n = tokenizedQuery.size();
        for(int index=1; index<4; index++){
            ArrayList<String> temp = new ArrayList<>(tokenizedQuery);
            for(int i=n; i>=1; i--){
                ArrayList<String> ngram = Util.NGrams(temp, i);
                for(String token : ngram){
                    if(QueryProcessor.indexes.get(index).containsKey(token)){
                        expandedQuery.get(index).add(token);
                        temp = Util.RemoveToken(temp, token, false);
                    }
                }
            }
        }

        for(String token : Util.TokenizeText(query, true)){
            if(QueryProcessor.indexes.get(0).containsKey(token)){
                expandedQuery.get(0).add(token);
            }
        }
        
        return expandedQuery;
    }

    // private static HashMap<Integer, ArrayList<String>> __queryExpand2(ArrayList<String> query){
    //     HashMap<Integer, ArrayList<String>> expandedQuery = new HashMap<>();
    //     for(int i=0; i<4; i++){
    //         expandedQuery.put(i, new ArrayList<>());
    //     }

    //     int n = query.size();
    //     for(int i=n; i>=1; i--){
    //         for(int index=1; index<4; index++){
    //             ArrayList<String> ngram = Util.NGrams(query, i);
    //             for(String token : ngram){
    //                 if(QueryProcessor.indexes.get(index).containsKey(token)){
    //                     expandedQuery.get(index).add(token);
    //                     query = Util.RemoveToken(query, token);
    //                 }
    //             }
    //         }
    //     }
    //     for(int i=n; i>=1; i--){
    //         ArrayList<String> ngram = Util.NGrams(query, i);
    //         for(String token : ngram){
    //             if(QueryProcessor.indexes.get(0).containsKey(token)){
    //                 expandedQuery.get(0).add(token);
    //                 query = Util.RemoveToken(query, token);
    //             }
    //         }
    //     }
    //     System.out.println(expandedQuery);
    //     return expandedQuery;
    // }

    private static void __AddTopK(ArrayList<Float> scores, HashMap<String, Float> finalScores){
        // Get the indices
        ArrayList<Integer> indices = Util.Sort(scores);

        for(int i=QueryProcessor.numDocuments-1; i>=0; i--)
        {
            String docID = QueryProcessor.ID2DocID.get(indices.get(i)+1);
            float score = scores.get(indices.get(i));
            // System.out.println(String.format("%s:%f", docID, score));
            if(score==0.0) break;

            if(finalScores.containsKey(docID)){
                finalScores.put(docID, (finalScores.get(docID)+score));
            }
            else{
                finalScores.put(docID, score);
            }
        }
    }

    private static void __scoreQuery(ArrayList<String> tokens, int index, HashMap<String, Float> finalScores){

        // System.out.println(tokens);

        // Compute the query scores
        HashMap<String, Byte> tokenCounts = Util.List2Set(tokens);
        ArrayList<Float> queryScores = new ArrayList<>(tokenCounts.size());
        float queryNormalizationScore = 0;
        for(int i=0; i<tokenCounts.size(); i++){
            String token = tokens.get(i);
            queryScores.add(QueryProcessor.__computeScore(tokenCounts.get(token), QueryProcessor.indexes.get(index).get(token).getValue0()));
            queryNormalizationScore += Math.pow(queryScores.get(i), 2);
        }
        queryNormalizationScore = (float)(Math.sqrt(queryNormalizationScore));
        
        //INFO
        // System.out.println(String.format("Calculated query scores as: %s and normalization score: %f", queryScores.toString(), queryNormalizationScore));

        // Start by collecting the unique set of relevant documents for the query
        float[] documentScores = new float[QueryProcessor.numDocuments];
        for(int i=0; i<tokenCounts.size(); i++){
            String token = tokens.get(i);
            Pair<Integer, ArrayList<Pair<Integer, Byte>>> tokenData = QueryProcessor.indexes.get(index).get(token);
            int documentFrequency = tokenData.getValue0();
            for(Pair<Integer, Byte> doc : tokenData.getValue1()){    
                // Documents which have multiple tokens matching with the query are important
                QueryProcessor.documentTokenCounts[doc.getValue0()-1]*=10;
                // Documents which have larger n-grams matching with the query are important
                documentScores[doc.getValue0()-1] += (queryScores.get(i) * QueryProcessor.__computeScore(doc.getValue1(), documentFrequency) * token.split(" ").length);

            }
        }

        // Normalize the scores
        ArrayList<Float> scores = new ArrayList<>();
        for(int i=0; i<QueryProcessor.numDocuments; i++){
            if(QueryProcessor.documentNormalizationScores[i]!=0.0 && documentScores[i] != 0.0){
                float score = documentScores[i] / (queryNormalizationScore * QueryProcessor.documentNormalizationScores[i]);
                scores.add(score);
            }
            else{
                scores.add(0.0f);
            }
        }

        // Add the scores for all the documents for this index to the final scores
        QueryProcessor.__AddTopK(scores, finalScores);
    }

    private static void __readMeta(GZIPInputStream metaFile) throws IOException {
        byte[] buf = new byte[4];
        while(true){
            // Read the docID length
            int len = metaFile.readNBytes(buf, 0, 4);
            if(len==0) break;
            int docIDSize = ByteBuffer.wrap(buf).getInt();

            // Read the docID
            byte[] docIDBuffer = new byte[docIDSize];
            metaFile.readNBytes(docIDBuffer, 0, docIDSize);
            String docID = new String(docIDBuffer, StandardCharsets.UTF_8);
            
            // Read the id of the docID string
            metaFile.readNBytes(buf, 0, 4);
            int ID = ByteBuffer.wrap(buf).getInt();
            QueryProcessor.ID2DocID.put(ID, docID);

        }
        QueryProcessor.numDocuments = QueryProcessor.ID2DocID.size();
        QueryProcessor.documentNormalizationScores = new float[QueryProcessor.numDocuments];
        QueryProcessor.documentTokenCounts = new int[QueryProcessor.numDocuments];
        for(Integer ID : QueryProcessor.ID2DocID.keySet()){
            QueryProcessor.DocID2ID.put(QueryProcessor.ID2DocID.get(ID), ID);
        }
    }

    private static void BuildDictAndIndex(String indexFile, String dictFile) throws IOException {
        // Read the inverted index and the dictionary in memory
        GZIPInputStream postingsFile = new GZIPInputStream(new FileInputStream(new File(indexFile)));
        GZIPInputStream dictionaryFile = new GZIPInputStream(new FileInputStream(new File(dictFile)));
        byte[] buf = new byte[4];


        // Read the dictionary first
        Integer[] breakpoints = new Integer[4];
        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<Integer> dfs = new ArrayList<>();
        ArrayList<Integer> startpositions = new ArrayList<>();

        int len = dictionaryFile.readNBytes(buf, 0, 4);
        while(len!=0){
            int tokenSize = ByteBuffer.wrap(buf).getInt();
            if(tokenSize==0){
                for(int i=0; i<4; i++){
                    dictionaryFile.readNBytes(buf, 0, 4);
                    breakpoints[i] = ByteBuffer.wrap(buf).getInt();
                }
                QueryProcessor.__readMeta(dictionaryFile);
                break;
            }

            // Read the token
            byte[] tokenBuffer = new byte[tokenSize];
            dictionaryFile.readNBytes(tokenBuffer, 0, tokenSize);
            String token = new String(tokenBuffer, StandardCharsets.UTF_8);
            tokens.add(token);
            //INFO
            // System.out.println(String.format("Word: %s", token));

            // Read the document frequency
            dictionaryFile.readNBytes(buf, 0, 4);
            int documentFrequency = ByteBuffer.wrap(buf).getInt();
            dfs.add(documentFrequency);
            //INFO
            // System.out.println(String.format("DF: %d", documentFrequency));

            // Read the starting index of the posting list
            dictionaryFile.readNBytes(buf, 0, 4);
            int START = ByteBuffer.wrap(buf).getInt();
            startpositions.add(START);

            // Get the size of the next token from the dictionary
            len = dictionaryFile.readNBytes(buf, 0, 4);
        }
        for(int invidx_index=0; invidx_index<4; invidx_index++){
            int START_IDX;
            if(invidx_index==0) START_IDX = 0;
            else START_IDX = breakpoints[invidx_index-1];
            int END_IDX = breakpoints[invidx_index];
            HashMap<String, Pair<Integer, ArrayList<Pair<Integer, Byte>>>> index = new HashMap<>();
            for(int vocab_index=START_IDX; vocab_index<END_IDX; vocab_index++){
                
                String token = tokens.get(vocab_index);
                int documentFrequency = dfs.get(vocab_index);
                int START = startpositions.get(vocab_index);

                // Read the posting list
                ArrayList<Pair<Integer, Byte>> postingList = new ArrayList<>();
                for(int i=0; i<documentFrequency; i++){
                    byte[] ID = new byte[4];
                    postingsFile.readNBytes(ID, 1, 3);
                    int docID = ByteBuffer.wrap(ID).getInt();

                    byte[] frequency = new byte[1];
                    postingsFile.readNBytes(frequency, 0, 1);
                    
                    postingList.add(new Pair<Integer, Byte>(docID, frequency[0]));
                    
                    //INFO
                    // System.out.println(String.format("---%d----%s:%d", i+1, QueryProcessor.ID2DocID.get(docID), frequency[0]));
                }
                index.put(token, new Pair<Integer, ArrayList<Pair<Integer, Byte>>>(documentFrequency, postingList));
            }
            QueryProcessor.indexes.add(index);
        }
        postingsFile.close();
        dictionaryFile.close();
    }

    private static void CalculateNormalizationScores(){
        for(int i=0; i<4; i++){
            HashMap<String, Pair<Integer, ArrayList<Pair<Integer, Byte>>>> index = QueryProcessor.indexes.get(i);
            for(String token : index.keySet()){
                int documentFrequency = index.get(token).getValue0();
                ArrayList<Pair<Integer, Byte>> postingsList = index.get(token).getValue1();
                for(Pair<Integer, Byte> p : postingsList){
                    float score = QueryProcessor.__computeScore(p.getValue1(), documentFrequency);
                    QueryProcessor.documentNormalizationScores[p.getValue0()-1] += Math.pow(score, 2);
                }
            }
        }
        for(int i=0; i<QueryProcessor.numDocuments; i++){
            float score = QueryProcessor.documentNormalizationScores[i];
            QueryProcessor.documentNormalizationScores[i] = (float)(Math.sqrt(score));
        }
    }

    private static void ProcessQueries(String filename, int K, String output) throws IOException {
        // Read the queries and tokenize them
        Matcher m = QueryProcessor.patternQuery.matcher(Util.File2Seq(filename));
        FileOutputStream fsOutput = new FileOutputStream(new File(output));

        ArrayList<HashMap<Integer, ArrayList<String>>> queries = new ArrayList<>();
        while(m.find()){
            String query = m.group(2).strip().replaceAll("[\\s]+", " ").toLowerCase();
            queries.add(QueryProcessor.__queryExpand1(query));

            System.out.format("Query: %d -- %s\n", queries.size(), queries.get(queries.size()-1));
        }

        for(int i=0; i<queries.size(); i++){
            //INFO
            // System.out.println(String.format("Processing query: %d", i));

            // Each query can be of multiple forms
            // ----- Airbus Subsidies    := Regular Query
            // ----- Air* Subsidies      := Query using wildcard
            // ----- O:Airbus Subsidies  := Query using named entities (L, O, P, N)

            // Assume you have a regular query with n tokens

            // Reset the token count array
            for(int j=0; j<QueryProcessor.numDocuments; j++){
                QueryProcessor.documentTokenCounts[j] = 1;
            }

            HashMap<String, Float> scores = new HashMap<>();
            for(int j=0; j<4; j++){
                QueryProcessor.__scoreQuery(queries.get(i).get(j), j, scores);
            }

            // Scale the scores
            for(String s : scores.keySet()){
                Integer ID = QueryProcessor.DocID2ID.get(s);
                scores.put(s, scores.get(s)*QueryProcessor.documentTokenCounts[ID-1]);
            }


            ArrayList<Pair<String, Float>> sortedScores = new ArrayList<>();
            for(String s : scores.keySet()){
                sortedScores.add(new Pair<String, Float>(s, scores.get(s)));
            }
            
            Comparator<Pair<String, Float>> comparator = new Comparator<Pair<String, Float>>() {
                public int compare(Pair<String, Float> a, Pair<String, Float> b) {
                    return Float.compare(b.getValue1(), a.getValue1());
                }
            };
            Collections.sort(sortedScores, comparator);
            for(int j=0; j<Math.min(K, sortedScores.size()); j++){
                Pair<String, Float> p = sortedScores.get(j);
                fsOutput.write(String.format("%s 0 %s 0 %f 0\n", 51+i, p.getValue0(), p.getValue1()).getBytes());
            }
        }

        fsOutput.close();
    }

    public static void main(String[] arguments) throws IOException {

        ArrayList<String> args = new ArrayList<>();
        for(int i=0; i<arguments.length; i+=2){
            if(i==0){
                assert(arguments[i].equals("--query"));
            }
            else if(i==2){
                assert(arguments[i].equals("--cutoff"));
            }
            else if(i==4){
                assert(arguments[i].equals("--output"));
            }
            else if(i==6){
                assert(arguments[i].equals("--index"));
            }
            else if(i==8){
                assert(arguments[i].equals("--dict"));
            }
            args.add(arguments[i+1]);
        }

        // Read the dictionaries and the index files
        System.out.println("Reading index and dictionary files...");
        QueryProcessor.BuildDictAndIndex(args.get(3), args.get(4));

        // Calculate the normalization scores for all the documents
        System.out.println("Calculating the normalization scores for all the documents...");
        QueryProcessor.CalculateNormalizationScores();
        
        // Read and respond to the queries
        System.out.println("Processing queries...");
        QueryProcessor.ProcessQueries(args.get(0), Integer.parseInt(args.get(1)), args.get(2));
        
    }
}