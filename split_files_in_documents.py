import os
import re

def main():
    documentPattern = re.compile("<DOC>(.*?)</DOC>", re.DOTALL)
    idPattern = re.compile("<DOCNO>(.*?)</DOCNO>", re.DOTALL)
    readfolder = "./data/TaggedTrainingAP"
    writefolder = "./data/Documents"
    files = os.listdir(readfolder)
    for file in files:
        with open(os.path.join(readfolder, file), "r") as f:
            content = f.read()
            documents = re.findall(documentPattern, content)
            for document in documents:
                ID = re.findall(idPattern, document)
                ID = ID[0].strip()
                f = open(os.path.join(writefolder, ID), "w")
                f.write(document)
                f.close()

if __name__ == "__main__":
    main()