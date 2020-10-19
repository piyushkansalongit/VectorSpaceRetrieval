import argparse

def main():
    parser = argparse.ArgumentParser(description="Generating statistics on IR result file wrt to grount truth")
    parser.add_argument('--results', type=str, default = './generated/resultFile', help='path to the results file')
    parser.add_argument('--gt', type=str, default='./data/qrels.filtered.51-100', help='path to the ground truth file')
    args = parser.parse_args()

    results = {}
    with open(args.results, "r") as f:
        lines = f.readlines()
        for line in lines:
            line = line.split(" ")
            try:
                results[line[0]].append((line[2],line[4]))
            except:
                results[line[0]] = [(line[2],line[4])]
    
    gt = {}
    with open(args.gt, "r") as f:
        lines = f.readlines()
        for line in lines:
            line = line.split(" ")
            if int(line[3]) == 1:
                try:
                    gt[line[0]].append(line[2])
                except:
                    gt[line[0]] = [line[2]]
    

    for queryID in results.keys():
        print("Query ID = %s" % queryID)
        print("---Number of search results: %d" % len(results[queryID]))
        try:
            print("---Maximum number of positive results: %d" % len(gt[queryID]))
            positions = []
            for pos, (docID, score) in enumerate(results[queryID]):
                if docID in gt[queryID]:
                    positions.append(pos)
            print("---Number of positive matches: %d" % len(positions))
            print("---Positions of positive matches: %s" % positions)
        except:
            print("---Maximum number of positive results: %d" % 0)

if __name__ == "__main__":
    main()