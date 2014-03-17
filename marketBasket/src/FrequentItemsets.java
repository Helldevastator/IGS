import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**********************************************************************
 * Übung 4
 * 
 * Implementieren Sie die Methode doFrequentItemsetsAnalysis().
 * 
 * Die Resultate sollten (für die ersten 5'000 Queries) etwa folgendermassen
 * aussehen: 1.1000% new, times, york 0.9400% america, bank, of 0.5800% free,
 * photos, sex 0.5400% com, myspace, www 0.5000% blue, book, kelly 0.3600%
 * little, river, weather 0.3600% credit, free, report 0.3400% little, river, sc
 * 0.3400% river, sc, weather 0.3400% little, sc, weather 0.3200% from, home,
 * work 0.2800% cards, e, free 0.2800% city, new, york 0.2400% r, toys, us
 * 0.2200% new, state, york 0.2200% free, games, online 0.2200% for, homes, sale
 * 0.2000% check, computer, up 0.1800% downloads, free, music 0.1800% hotels,
 * las, vegas
 */
public class FrequentItemsets {
	static int numQuery = 10000;
	static int numWords = 3;
	static double minSupport = 0.0018;

	public static Map<WordBasket, Double> doFrequentItemsetsAnalysis(List<WordBasket> _queries, int _numWords, double _minSupport) {
		/*********************
		 * Insert your code here
		 * 
		 * Args: _queries: The list of queries done by users _numWords: The
		 * number of words per basked the caller is interested in _minSuport:
		 * the minimal suport
		 * 
		 * Returns: This method should return a map where the key specifies a
		 * group of words (WordBasket) and the value their respective support
		 * (Double).
		 */
		//case zero
		HashMap<String, Item> allWords = new HashMap<>(numQuery >>> 1);

		//Calculate first level
		for (WordBasket query : _queries)
			for (String w : query.words)
				if (allWords.containsKey(w)) {
					Item i = allWords.get(w);
					i.occurences++;
				} else {
					Item i = new Item();
					i.basket = new WordBasket(new String[] { w });
					i.occurences = 1;
					allWords.put(w, i);
				}

		ArrayList<Item> items = new ArrayList<Item>(allWords.values());
		calcAndRemoveSupports(items, _minSupport);
		Collections.sort(items);

		String[] level1 = new String[items.size()];
		for (int i = 0; i < level1.length; i++)
			level1[i] = items.get(i).basket.words[0];

		for (int i = 1; i < _numWords; i++) {
			//long start, end;
			//start = System.currentTimeMillis();
			items = createNextLevel(items, level1);
			//end = System.currentTimeMillis();
			//System.out.println("create: " + (end - start) + " size:" + items.size());

			//start = System.currentTimeMillis();
			countOccurences(_queries, items);
			//end = System.currentTimeMillis();
			//System.out.println("count: " + (end - start));

			//start = System.currentTimeMillis();
			calcAndRemoveSupports(items, minSupport);
			//end = System.currentTimeMillis();
			//System.out.println("remove: " + (end - start));

			if (items.size() <= 0)
				break;
		}

		Map<WordBasket, Double> results = new HashMap<>(items.size());
		for (Item i : items)
			results.put(i.basket, i.support);

		return results;
	}

	private static ArrayList<Item> createNextLevel(List<Item> current, String[] level1) {
		HashSet<Item> ret = new HashSet<>(current.size());
		for (int k = 0; k < current.size(); k++) {
			Item cur = current.get(k);

			//stupidly create all possible combinations, could be better!!
			for (int i = 0; i < level1.length; i++)
				if (!cur.basket.contains(level1[i])) {
					String[] n = Arrays.copyOf(cur.basket.words, cur.basket.words.length + 1);
					n[cur.basket.words.length] = level1[i];

					Item next = new Item();
					next.basket = new WordBasket(n);
					next.occurences = 0;

					//if(!ret.contains(next))
					ret.add(next);
				}
		}

		return new ArrayList<Item>(ret);
	}

	private static void countOccurences(List<WordBasket> queries, List<Item> items) {
		for (WordBasket b : queries)
			for (int j = 0; j < items.size(); j++) {
				Item i = items.get(j);
				//n log n, could be less?
				if (b.containsAll(i.basket))
					i.occurences++;
			}
	}

	private static void calcAndRemoveSupports(List<Item> items, double minSupport) {
		//calc and remove low support
		for (int i = items.size() - 1; i >= 0; i--) {
			Item item = items.get(i);
			item.support = (double) item.occurences / numQuery;
			if (item.support < minSupport)
				items.remove(i);
		}
	}

	public static void main(String[] args) throws Exception {
		List<WordBasket> queries = readQueries(numQuery);

		//perform analysis for baskets of 3 words
		long startTime = System.currentTimeMillis();

		Map<WordBasket, Double> result = doFrequentItemsetsAnalysis(queries, numWords, minSupport);
		long endTime = System.currentTimeMillis();

		//print top 20 word baskets
		prettyPrint(result, 20);

		//print benchmark time
		System.out.println();
		System.out.println("Took " + (endTime - startTime) + "ms");
	}

	//prints "nicely" formatted results
	public static void prettyPrint(final Map<WordBasket, Double> _results, int _limit) {
		//sort all results
		LinkedList<WordBasket> sortedKeys = new LinkedList<WordBasket>(_results.keySet());
		Collections.sort(sortedKeys, new Comparator<WordBasket>() {
			@Override
			public int compare(WordBasket _a, WordBasket _b) {
				return _results.get(_b).compareTo(_results.get(_a));
			}
		});

		LinkedHashMap<WordBasket, Double> sortedResults = new LinkedHashMap<WordBasket, Double>(1024);
		for (WordBasket words : sortedKeys)
			sortedResults.put(words, _results.get(words));

		//print top _limit results
		int cnt = 0;
		for (Entry<WordBasket, Double> e : sortedResults.entrySet()) {
			String words = "";
			for (String word : e.getKey().words)
				words += ", " + word;
			System.out.format("%8.4f%%   %s%n", e.getValue() * 100, words.substring(2));

			cnt++;
			if (cnt == _limit)
				break;
		}
	}

	//reads and parses queries.txt
	public static List<WordBasket> readQueries(int _limit) throws Exception {
		LinkedList<WordBasket> queries = new LinkedList<WordBasket>();

		BufferedReader r = new BufferedReader(new FileReader("queries.txt"));
		String query;
		int cnt = 0;
		while ((query = r.readLine()) != null) {
			String[] words = query.split(" ");
			queries.add(new WordBasket(words));
			cnt++;
			if (cnt == _limit)
				break;
		}
		r.close();
		return queries;
	}
}
