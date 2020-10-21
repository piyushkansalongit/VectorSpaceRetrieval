# Compile
javac -classpath .:./lib/* -d classes src/*/*.java

# Run the indexer
/usr/bin/time -v java -Xmx4096m -classpath .:classes:./lib/* src/main/InvertedIndex.java /home/ankur/Desktop/COL764/data/TaggedTrainingAP /home/ankur/Desktop/COL764/generated/indexfile

# Run the dictionary printer
java -classpath .:classes:./lib/* src/main/printdict.java /home/ankur/Desktop/COL764/generated/indexfile.dict > /home/ankur/Desktop/COL764/generated/dictionary

# Process the queries
java -Xmx4096m -classpath .:classes:./lib/* src/main/QueryProcessor.java \
--query /home/ankur/Desktop/COL764/data/topics.51-100.wildcard-tagged \
--cutoff 100 \
--output /home/ankur/Desktop/COL764/generated/resultfile \
--index /home/ankur/Desktop/COL764/generated/indexfile.idx \
--dict /home/ankur/Desktop/COL764/generated/indexfile.dict

# Get the results
./trec_eval-master/trec_eval -mndcg_cut.10 -M100 -mset_F /home/ankur/Desktop/COL764/data/qrels.filtered.51-100 /home/ankur/Desktop/COL764/generated/resultfile
python3 generate_stats.py --gt=/home/ankur/Desktop/COL764/data/qrels.filtered.51-100 --results=/home/ankur/Desktop/COL764/generated/resultfile
