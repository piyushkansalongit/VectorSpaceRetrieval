package src.main;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private static Pattern patternSpecialWords = Pattern.compile("[a-zA-Z]+['][a-zA-Z]+");
    private static Pattern patterAcronyms = Pattern.compile("\\b(?:[a-zA-Z]\\.){2,}");
    private static Pattern patternPunctuations = Pattern.compile("[,'-._:;#%=@?^`!~$&/\"|\\\\]");

    // Converts the contents of a file into a CharSequence
    public static CharSequence File2Seq(String filename) throws IOException {
        FileInputStream input = new FileInputStream(filename);
        FileChannel channel = input.getChannel();
        ByteBuffer bbuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int)channel.size());
        CharBuffer cbuf = Charset.forName("8859_1").newDecoder().decode(bbuf);
        input.close();
        return cbuf;
    }

    // Matches, extracts and removes matching patterns from a string
    private static String FindMatchingTokens(ArrayList<String> tokens, String text, Pattern pattern){
        Matcher m = pattern.matcher(text);
        while(m.find()){
            String token = m.group();
            tokens.add(token);
        }
        text = m.replaceAll("");
        return text;
    }
    
    public static ArrayList<String> TokenizeText(String text){
        ArrayList<String> tokens = new ArrayList<>();
 
        // tokens with apostrophe
        // text = Util.FindMatchingTokens(tokens, text, patternSpecialWords);
    
        // tokens that are acronyms
        text = Util.FindMatchingTokens(tokens, text, patterAcronyms);

        // Remove punctuations
        text = patternPunctuations.matcher(text).replaceAll(" ");
        
        for (String token : text.split("[\\s]+", 0)) {
            if(token.length() == 0)
                continue;
            tokens.add(token);
        }
        return tokens;
    }

    // Clean the text
    public static String SpecialCleaner(String text){
        return text.toLowerCase().replaceAll("([a-z]*)([\\d]+)([a-z]*)", "");
        // return text;
    }

    // Converts the list of tokens into a set of unique tokens with counts
    public static HashMap<String, Byte> List2Set(ArrayList<String> tokens){
        HashMap<String, Byte> wordCounts = new HashMap<>();
        for(String s : tokens) {
            if(wordCounts.containsKey(s)){
                wordCounts.put(s,(byte)(wordCounts.get(s)+1));
            }
            else{
                wordCounts.put(s, (byte)1);
            }
        }
        return wordCounts;
    }

    public static ArrayList<String> NGrams(ArrayList<String> words, int n) {
        n = Math.min(n, words.size());
        ArrayList<String> ngrams = new ArrayList<String>();
        for (int i = 0; i < words.size() - n + 1; i++)
            ngrams.add(Util.__concat(words, i, i+n));
        return ngrams;
    }

    public static String __concat(ArrayList<String> words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++)
            sb.append((i > start ? " " : "") + words.get(i));
        return sb.toString();
    }

    public static ArrayList<String> RemoveToken(ArrayList<String> query, String token){
        return  Util.TokenizeText(Util.__concat(query, 0, query.size()).replace(token, ""));
    }

    public static ArrayList<Integer> Sort(ArrayList<Float> scores){
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            indices.add(i);
        }
        // Sort the indices based on the scores
        Comparator<Integer> comparator = new Comparator<Integer>() {
            public int compare(Integer i, Integer j) {
                return Float.compare(scores.get(i), scores.get(j));
            }
        };
        Collections.sort(indices, comparator);
        return indices;
    }
}