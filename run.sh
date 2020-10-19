javac -classpath .:./lib/* -d classes src/*/*.java
# /usr/bin/time -v java -Xmx4096m -classpath .:classes:./lib/* src/main/InvertedIndex.java data/TaggedTrainingAP generated/indexfile
# /usr/bin/time -v python3 view_dict_and_index.py --vocabulary=True --index=False
java -Xmx4096m -classpath .:classes:./lib/* src/main/QueryProcessor.java --query data/topics.51-100 --cutoff 100 --output generated/resultFile --index generated/indexfile.idx --dict generated/indexfile.dict
./trec_eval-master/trec_eval -mndcg_cut.10 -M100 -mset_F data/qrels.filtered.51-100 generated/resultFile
python3 generate_stats.py