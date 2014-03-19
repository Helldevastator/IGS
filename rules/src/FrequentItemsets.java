import java.io.*;
import java.util.*;
import java.util.Map.*;

/**********************************************************************
 * Übung 3
 * 
 * Implementieren Sie die Methode doFrequentItemsetsAnalysis().
 * 
 * Die Resultate sollten (für die ersten 5'000 Queries) etwa folgendermassen aussehen:
 *   1.1000%   new, times, york
 *   0.9400%   america, bank, of
 *   0.5800%   free, photos, sex
 *   0.5400%   com, myspace, www
 *   0.5000%   blue, book, kelly
 *   0.3600%   little, river, weather
 *   0.3600%   credit, free, report
 *   0.3400%   little, river, sc
 *   0.3400%   river, sc, weather
 *   0.3400%   little, sc, weather
 *   0.3200%   from, home, work
 *   0.2800%   cards, e, free
 *   0.2800%   city, new, york
 *   0.2400%   r, toys, us
 *   0.2200%   new, state, york
 *   0.2200%   free, games, online
 *   0.2200%   for, homes, sale
 *   0.2000%   check, computer, up
 *   0.1800%   downloads, free, music
 *   0.1800%   hotels, las, vegas
 */
public class FrequentItemsets
{
	//the maximum number of queries to read as transaction set
    static int maxQueries = 5000;
    
    //the maximum set size we are interested it 
	static int maxItemsetSize = 3;
	
	//the minimum support for a frequent item set
	static double minSupport = 0.0018;
		
  public static Map<WordBasket,Double> doFrequentItemsetsAnalysis(List<WordBasket> _queries,int _numWords, double _minSupport)
  {
    /*********************
     * Insert your code here
     * 
     * Args:
     *   _queries: The list of queries done by users
     *   _numWords: The number of words per basked the caller is interested in
	 *	 _minSupport: the minimal support 
     *
     * Returns:
     *   This method should return a map where the key specifies a group of
     *   words (WordBasket) and the value their respective support (Double).
     */
    throw new RuntimeException("This method to be implemented.");
  }
  
  
  public static void main(String[] args) throws Exception
  {
	List<WordBasket> queries=readQueries(maxQueries);
    
    //perform analysis for baskets of 3 words
    long startTime=System.currentTimeMillis();
 
	Map<WordBasket,Double> result=doFrequentItemsetsAnalysis(queries,maxItemsetSize,minSupport);
    long endTime=System.currentTimeMillis();
    
    //print top 20 word baskets
    prettyPrint(result,20);
    
    //print benchmark time
    System.out.println();
    System.out.println("Took "+(endTime-startTime)+"ms");
  }
  
  
  //prints "nicely" formatted results
  public static void prettyPrint(final Map<WordBasket,Double> _results,int _limit)
  {
    //sort all results
    LinkedList<WordBasket> sortedKeys=new LinkedList<WordBasket>(_results.keySet());
    Collections.sort(sortedKeys,new Comparator<WordBasket>()
      {
        public int compare(WordBasket _a,WordBasket _b)
        {
          return _results.get(_b).compareTo(_results.get(_a));
        }
      });
    
    LinkedHashMap<WordBasket,Double> sortedResults=new LinkedHashMap<WordBasket,Double>(1024);
    for(WordBasket words:sortedKeys)
      sortedResults.put(words,_results.get(words));
    
    //print top _limit results
    int cnt=0;
    for(Entry<WordBasket,Double> e:sortedResults.entrySet())
    {
      String words="";
      for(String word:e.getKey().words)
        words+=", "+word;
      System.out.format("%8.4f%%   %s%n",e.getValue()*100,words.substring(2));
      
      cnt++;
      if(cnt==_limit)
        break;
    }
  }
  
  //reads and parses queries.txt
  public static List<WordBasket> readQueries(int _limit) throws Exception
  {
    LinkedList<WordBasket> queries=new LinkedList<WordBasket>();
    
    BufferedReader r=new BufferedReader(new FileReader("queries.txt"));
    String query;
    int cnt=0;
    while((query=r.readLine())!=null)
    {
      String[] words=query.split(" ");
      queries.add(new WordBasket(words));
      cnt++;
      if(cnt==_limit)
        break;
    }
    r.close();
    return queries;
  }
}
