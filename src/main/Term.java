package src.main;

import java.util.ArrayList;

import org.javatuples.Pair;

public class Term {

    public int documentFrequency;
    public int offset1;
    public ArrayList<Pair<Integer, Byte>> postingList;

    public Term(){
        this.documentFrequency = 0;
        this.offset1 = 0;
        this.postingList = new ArrayList<>();
    }

    public void update(int docID, int termFrequnecy){
        this.documentFrequency+=1;
        this.postingList.add(new Pair<Integer, Byte>(docID, (byte)termFrequnecy));
    }
   
}
