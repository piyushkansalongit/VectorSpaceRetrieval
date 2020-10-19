import argparse
import json
from tqdm import tqdm
import gzip
import os

def str2bool(v):
    if isinstance(v, bool):
       return v
    if v.lower() in ('yes', 'true', 't', 'y', '1'):
        return True
    elif v.lower() in ('no', 'false', 'f', 'n', '0'):
        return False
    else:
        raise argparse.ArgumentTypeError('Boolean value expected.')

def readInt(f, size=4):
    return int.from_bytes(f.read(size), byteorder='big', signed=False)

def readStr(f, size):
    return f.read(size).decode("utf-8")

def readPostingList(f, size):
    pList = []
    totalOccurrence = 0
    for i in range(0, size):
        ID = readInt(f, 3)
        termFrequency = readInt(f, 1)
        totalOccurrence += termFrequency
        pList.append([ID, termFrequency])
    return pList, totalOccurrence

def main():
    parser = argparse.ArgumentParser(description='Viewing the index files and dictionaries')
    parser.add_argument('--path', type=str, default="./generated",
                        help='path of the file where generated index and dictionaries are stored')
    parser.add_argument('--index', type=str2bool, default='False', help="Save all the data to a json file?")
    parser.add_argument('--vocabulary', type=str2bool, default='False', help='Save the vocab?')
    args = parser.parse_args()

    # # Start with reading the meta file
    # ID2DocID = {}
    # # Expected Format is: int:string:int with docID size: doc ID: ID
    # f=gzip.open(os.path.join(args.path, "meta"),'rb')
    # size = readInt(f)
    # while(size):
    #     # Read size numbe of bytes to get the document ID
    #     docID = readStr(f, size)
    #     # Read the numeric id assigned to docID
    #     ID = readInt(f)
    #     ID2DocID[ID] = docID
    #     size = readInt(f)

    # Read all the dictionaries and the index files
    view = {}
    tokens = {}
    for i in tqdm(range(4)):
        INVIDX = {}
        # Read the dictionary file
        path = os.path.join(args.path, "dictionary_%d" % i)
        dictFile = gzip.open(path,'rb')

        # Read the index file
        path = os.path.join(args.path, "posting_list_%d" % i)
        idxFile = gzip.open(path,'rb')

        size = readInt(dictFile)
        tokens[i] = []
        while(size):
            # String token
            token = readStr(dictFile, size)

            INVIDX[token] = {}

            # Document Frequency
            documentFrquency = readInt(dictFile)
            INVIDX[token]["DF"] = documentFrquency

            # Start index in the idx file and the size of the posting list
            startIndex = readInt(dictFile)

            INVIDX[token]["PostingList"], totalOccurrence = readPostingList(idxFile, documentFrquency)
            
            # Build the token list for saving vocabulary
            tokens[i].append((token, totalOccurrence))
            # tokens[i].append((token))
            #Update the size for next iteration
            size = readInt(dictFile)

        tokens[i].sort(key=lambda x: x[1])
        # tokens[i].sort()
        view[i] = INVIDX

    #Save the dict
    if(args.vocabulary):
        with open(os.path.join(args.path, 'vocabulary.json'), 'w', encoding='utf-8') as f:
            json.dump(tokens, f, ensure_ascii=False, indent=4)

    if(args.index):
        with open(os.path.join(args.path, 'view.json'), 'w', encoding='utf-8') as f:
            json.dump(view, f, ensure_ascii=False, indent=4)

if __name__ == "__main__":
    main()