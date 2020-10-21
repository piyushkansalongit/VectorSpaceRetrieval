package src.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class printdict {

    private static String __getHifen(int n){
        StringBuilder s = new StringBuilder("");
        for(int i=0; i<n; i++){
            s.append("-");
        }
        return s.toString();
    }

    private static void BuildDict(String dictFile) throws IOException {
        // Read the inverted index and the dictionary in memory
        GZIPInputStream dictionaryFile = new GZIPInputStream(new FileInputStream(new File(dictFile)));
        byte[] buf = new byte[4];
    
        // Read the dictionary first
        Integer[] breakpoints = new Integer[4];
    
        int len = dictionaryFile.readNBytes(buf, 0, 4);

        while(len!=0){
            int tokenSize = ByteBuffer.wrap(buf).getInt();
            if(tokenSize==0){
                for(int i=0; i<4; i++){
                    dictionaryFile.readNBytes(buf, 0, 4);
                    breakpoints[i] = ByteBuffer.wrap(buf).getInt();
                }
                break;
            }
    
            // Read the token
            byte[] tokenBuffer = new byte[tokenSize];
            dictionaryFile.readNBytes(tokenBuffer, 0, tokenSize);
            String token = new String(tokenBuffer, StandardCharsets.UTF_8);
    
            // Read the document frequency
            dictionaryFile.readNBytes(buf, 0, 4);
            int documentFrequency = ByteBuffer.wrap(buf).getInt();
    
            // Read the starting index of the posting list
            dictionaryFile.readNBytes(buf, 0, 4);
            int START = ByteBuffer.wrap(buf).getInt();
    
            // Get the size of the next token from the dictionary
            len = dictionaryFile.readNBytes(buf, 0, 4);

            System.out.format("%s%s: %d%s : %d\n", token, printdict.__getHifen(100-token.length()), documentFrequency, printdict.__getHifen(10-Integer.toString(documentFrequency).length()),  START);
        }

        dictionaryFile.close();
    }
    public static void main(String[] args) throws IOException {
        printdict.BuildDict(args[0]);
    }
}