/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package relevance;

import RankedRetrieval.DefaultRanking;
import RankedRetrieval.OkapiBM25Ranking;
import RankedRetrieval.RankedRetrievals;
import RankedRetrieval.RankingStrategy;
import RankedRetrieval.Tf_IdfRanking;
import RankedRetrieval.WackyRanking;
import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.Indexes;
import cecs429.index.Posting;
import cecs429.index.PostingAccumulator;
import cecs429.text.AdvancedTokenProcessor;
import csulb.DirectorySearch;
import disk.DiskIndexWriter;
import disk.DiskInvertedIndex;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dhrum
 */
public class Relevance {
    
    private static Indexes indexes;

    String query;
    private static String mPath;
    private String input;
    private List<String> listKeys;
    private static DocumentCorpus corpus;
    private List<Posting> result_docs = new ArrayList();
    private List<PostingAccumulator> Ranking_results = new ArrayList<>();
    private static AdvancedTokenProcessor processor = new AdvancedTokenProcessor();
    private static RankingStrategy ranking_strategy;
    private static String query_path;
    private static String qrel_path;
    

    static void QueryIndex()
    {
        mPath = "C:\\Docs\\Study\\SET\\Data\\relevance_cranfield";
        corpus = DirectoryCorpus.loadJsonDirectory(Paths.get(mPath).toAbsolutePath(), ".json");
        System.out.println(corpus.getCorpusSize());
        DiskInvertedIndex DII = new DiskInvertedIndex(mPath + "\\index\\");
        DiskInvertedIndex DII_biword = new DiskInvertedIndex(mPath + "\\index\\biword\\");
        indexes = new Indexes(DII, DII_biword);
        String query;
        //DiskInvertedIndex DII = new DiskInvertedIndex(mPath + "\\index\\");
        

    }
    
    private static List<PostingAccumulator> mMethod(String query) throws IOException {
        String result = "";
        RankedRetrievals r = new RankedRetrievals(query, mPath, corpus.getCorpusSize());
        List<PostingAccumulator> Ranking_results = new ArrayList<>();
    try {
        Ranking_results = r.getPostings(indexes, processor, ranking_strategy);
    } catch (IOException ex) {
        Logger.getLogger(DirectorySearch.class.getName()).log(Level.SEVERE, null, ex);
    }
    int j = 0;
    for (PostingAccumulator p : Ranking_results) {
        j++;
        Posting posting = p.getPosting();
        //System.out.println(posting.getDocumentId());
        String s = corpus.getDocument(posting.getDocumentId()).getTitle() + " Accum value - " + p.getAccumulator();
        result += s +"\n";
//        System.out.println(j + ")" + corpus.getDocument(posting.getDocumentId()).getTitle() + " Accum value - " + p.getAccumulator());
    }
    
        return Ranking_results;
    }
    
    public static void findMAP() throws IOException
    {
      query_path = "C:\\Docs\\Study\\SET\\Data\\relevance_cranfield\\relevance\\queries";
      qrel_path = "C:\\Docs\\Study\\SET\\Data\\relevance_cranfield\\relevance\\qrel";
      Scanner sc1 = new Scanner(new File(query_path));
      Scanner sc2 = new Scanner(new File(qrel_path));
      String query = "";
      String rel = "";
      List<PostingAccumulator> results = new ArrayList<>();
      double MAP = 0;
      double MRT = 0;
      double number_queries=0;
      while(sc1.hasNextLine())
      {
          number_queries++;
          String doc_list = "";
          query = sc1.nextLine();
          rel = sc2.nextLine();
          Set<Integer> rel_list = new HashSet<>();
          for(String s : rel.split(" "))
          {
              rel_list.add(Integer.parseInt(s));
          }
          long startTime = System.currentTimeMillis();;
          results = mMethod(query);
          long endTime = System.currentTimeMillis();;
          long queryTime = (endTime - startTime);
          double AP = 0;
          double numerator =0;
          double denominator = 0;
          for(PostingAccumulator p : results)
          {
             denominator++;
             Posting posting = p.getPosting();
             int doc_id = posting.getDocumentId();
             Document d = corpus.getDocument(doc_id);
             
             Path doc_path = d.getFilePath();
             String doc_name = doc_path.getFileName().toString();
            // System.out.println(doc_name);
             doc_name = doc_name.replaceFirst("[.][^.]+$", "");
             int doc_no = Integer.parseInt(doc_name);
             if(rel_list.contains(doc_no))
             {
                 numerator++;
                 doc_list += doc_no + " ";
                 double Pik = numerator/denominator;
                 AP += Pik;
             }
          }
          AP = AP/(double)rel_list.size();
//        System.out.println(AP);
          MAP += AP;
          MRT += queryTime;
       }
      // x --- 256 ...... 1sec --?
      double throughput = 256/(MRT/1000);
      MAP = MAP/number_queries;
      MRT = MRT/number_queries;
      System.out.println("MAP = " + MAP);
      System.out.println("Throughput = " + throughput);
      System.out.println("MRT = " + MRT + " miliseconds");
    }
    
    public static void main(String args[]) throws IOException {
      QueryIndex();
      System.out.println("Enter the corpus path : ");
      Scanner sc = new Scanner(System.in);
      mPath = sc.nextLine();
      DiskInvertedIndex DII = new DiskInvertedIndex(mPath + "\\index\\");
      
      query_path = mPath + "\\relevance\\queries";
      qrel_path = mPath + "\\relevance\\qrel";
      
      ranking_strategy = new DefaultRanking(DII);
      System.out.println("Default Ranking");
      findMAP();
      System.out.println();
      ranking_strategy = new OkapiBM25Ranking(DII);
      System.out.println("OkapiBM25 Ranking");
      findMAP();
      System.out.println();
      ranking_strategy = new Tf_IdfRanking(DII);
      System.out.println("Tf_Idf Ranking");
      findMAP();
      System.out.println();
      ranking_strategy = new WackyRanking(DII);
      System.out.println("Wacky Ranking");
      findMAP();
      
      
     
      
      
    
    
     
            
    }
}
