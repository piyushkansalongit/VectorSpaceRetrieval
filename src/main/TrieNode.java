package src.main;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {
    private Character key = null;
    private boolean wordEnd = false;
    public Map<Character, TrieNode> children;
    
    public TrieNode(){
        this.children = new HashMap<>();
    }

    public TrieNode(final Character key){
        this.children = new HashMap<>();
        this.key = key;
    }

    public void addPath(final String path){
        TrieNode temp = this;
        for(int i=0; i<path.length(); i++){
            final Character c = path.charAt(i);
            TrieNode child;
            if(!temp.children.containsKey(c)){
                child = new TrieNode(c);
                temp.children.put(c, child);
            }
            temp = temp.children.get(c);
        }
        temp.wordEnd = true;
    }

    public boolean hasPath(String path){
        TrieNode temp = this;
        for(int i=0; i<path.length(); i++){
            final Character c = path.charAt(i);
            if(!temp.children.containsKey(c))
                return false;
            temp = temp.children.get(c);
        }
        return temp.wordEnd;
    }

    public void printAllPaths(final StringBuilder buffer){
        for(final Character c : this.children.keySet()){
            buffer.append(c);
            this.children.get(c).printAllPaths(buffer);
            buffer.deleteCharAt(buffer.length()-1);
        }
        if(this.children.keySet().isEmpty()){
            System.out.println(buffer);
        }
    }


}
