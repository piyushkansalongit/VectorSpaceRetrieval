package src.main;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    private static Pattern patternAcronyms = Pattern.compile("\\b(?:[a-zA-Z]\\.){2,}");
    private static Pattern patternPunctuationText = Pattern.compile("[,'._:;#%*=@?^`!~$&/\"|\\\\\\(\\)\\{\\}-]");
    private static Pattern patternPunctuationQuery = Pattern.compile("[,._:;#%=@?^`!~$&/\"|\\\\\\(\\)\\{\\}-]");
    public static String __concat(ArrayList<String> words, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++)
            sb.append((i > start ? " " : "") + words.get(i));
        return sb.toString();
    }

    // Matches, extracts and removes matching patterns from a string
    public static String FindAcronyms(ArrayList<String> tokens, String text){
        Matcher m = Util.patternAcronyms.matcher(text);
        while(m.find()){
            String token = m.group();
            tokens.add(token);
        }
        text = m.replaceAll("");
        return text;
    }

    public static String FindMatchingEntities(ArrayList<String> tokens, String text, Pattern pattern){
        Matcher m = pattern.matcher(text);
        while(m.find()){
            String token = m.group();
            tokens.add(token.replaceAll("[l|o|p|n]:", "").strip());
        }
        text = m.replaceAll("");
        return text;
    }

    public static String RemovePunctuationFromText(String text){
        return Util.patternPunctuationText.matcher(text).replaceAll(" ");
    }

    public static String RemovePunctuationFromQuery(String text){
        return Util.patternPunctuationQuery.matcher(text).replaceAll(" ");
    }


    // Converts the contents of a file into a CharSequence
    public static CharSequence File2Seq(String filename) throws IOException {
        FileInputStream input = new FileInputStream(filename);
        FileChannel channel = input.getChannel();
        ByteBuffer bbuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int)channel.size());
        CharBuffer cbuf = Charset.forName("8859_1").newDecoder().decode(bbuf);
        input.close();
        return cbuf;
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

    public static ArrayList<String> DeleteNGram(String token, ArrayList<String> tokens){
        String remaining = Util.__concat(tokens, 0, tokens.size()).replace(token, "");
        return new ArrayList<String>(Arrays.asList(remaining.split("[\\s]+", 0)));
    }

    public static ArrayList<Integer> Sort(final ArrayList<Float> scores){
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

    public static boolean IsWildCard(String s){
        return s.charAt(s.length()-1)=='*';
    }

    public static ArrayList<String> FindWildCardTokens(String s, ArrayList<String> vocabulary){
        ArrayList<String> matches = new ArrayList<>();
        for(String token : vocabulary){
            if(token.length()>=s.length()-1){
                if(token.substring(0, s.length()-1).equals(s.substring(0, s.length()-1))){
                    matches.add(token);
                }
            }
        }
        return matches;
    }
}