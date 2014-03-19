import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**********************************************************************
 * Übung 4
 * 
 * Implementieren Sie die Methode doAssociationRulesAnalysis().
 * 
 * Die Resultate sollten für die ersten 5'000 Queries, MIN_SUPPORT=0.0018,
 * MIN_CONFIDENCE=0.05 etwa folgendermassen aussehen:
 * 
 * SupportLHS Confidence LHS => RHS 6.4800% 53.7037% {american} => {idol}
 * 6.4800% 29.9383% {american} => {airlines} 6.4800% 6.7901% {american} =>
 * {express} 5.7400% 18.4669% {free} => {porn} 5.7400% 18.4669% {free} => {sex}
 * 5.7400% 14.6341% {free} => {games} 5.7400% 10.4530% {free} => {photos}
 * 5.7400% 10.1045% {free} => {photos, sex} 5.7400% 8.3624% {free} => {pics}
 * 5.7400% 7.6655% {free} => {cards} 5.7400% 6.6202% {free} => {credit} 5.7400%
 * 6.2718% {free} => {report} 5.7400% 6.2718% {free} => {downloads} 5.7400%
 * 6.2718% {free} => {credit, report} 5.7400% 6.2718% {free} => {music} 5.7400%
 * 5.2265% {free} => {e} 4.5400% 71.3656% {map} => {quest} 4.5400% 12.7753%
 * {map} => {of} 3.8800% 24.7423% {of} => {america} 3.8800% 24.2268% {of} =>
 * {bank}
 */
public class AssociationRules extends FrequentItemsets {
	public static List<AssociationRuleEntry> doAssociationRuleAnalysis(List<WordBasket> _queries, Map<WordBasket, Double> _frequentItemsets, double _minConfidence) {
		/*********************
		 * Insert your code here
		 * 
		 * Args: _queries: The list of queries done by users _frequentItemsets:
		 * The most frequent itemsets
		 * 
		 * Returns: This method should return a list of AssociationRuleEntry's.
		 */

		throw new RuntimeException("This method to be implemented.");
	}

	private static void powSet(WordBasket b, Map<WordBasket, Double> frequentItemSets) {
		int max = (int) (Math.pow(2, b.words.length) - 1);
		for (int bitset = 1; bitset < max; bitset++) {
			LinkedList<String> lhs = new LinkedList<>();
			LinkedList<String> rhs = new LinkedList<>();

			for (int j = 0; j < b.words.length; j++)
				if ((bitset & 1 << j) != 0)
					lhs.add(b.words[j]);
				else
					rhs.add(b.words[j]);
			WordBasket lhs_basket = new WordBasket(lhs);
			AssociationRuleEntry entry = new AssociationRuleEntry(lhs_basket, new WordBasket(rhs), frequentItemSets.get(b), frequentItemSets.get(lhs_basket));
		}

	}

	public static void main(String[] args) throws Exception {

		double minSupport = 0.0018;
		double minConfidence = 0.05;

		//read the first 5'000 queries
		List<WordBasket> queries = readQueries(5000);

		//perform analysis for baskets of 2-4 words
		Map<WordBasket, Double> frequentItemsets = new HashMap<WordBasket, Double>();
		frequentItemsets.putAll(doFrequentItemsetsAnalysis(queries, 4, minSupport));

		prettyPrint(frequentItemsets, 200);

		//perform association rule analysis
		long startTime = System.currentTimeMillis();
		List<AssociationRuleEntry> result = doAssociationRuleAnalysis(queries, frequentItemsets, minConfidence);
		long endTime = System.currentTimeMillis();

		//print top 20 association rules
		prettyPrint(result, 20);

		//print benchmark time
		System.out.println();
		System.out.println("Took " + (endTime - startTime) + "ms");
	}

	//prints "nicely" formatted results
	public static void prettyPrint(final List<AssociationRuleEntry> _results, int _limit) {
		//sort all results
		Collections.sort(_results, new Comparator<AssociationRuleEntry>() {
			@Override
			public int compare(AssociationRuleEntry _a, AssociationRuleEntry _b) {
				int lhsSupport = Double.compare(_b.supportLHS, _a.supportLHS);
				if (lhsSupport != 0)
					return lhsSupport;

				return Double.compare(_b.supportLHS_RHS, _a.supportLHS_RHS);
			}
		});

		//print results
		System.out.println("SupportLHS Confidence   LHS => RHS");
		int cnt = 0;
		for (AssociationRuleEntry r : _results) {
			String lhs = "";
			for (String word : r.lhs.words)
				lhs += ", " + word;

			String rhs = "";
			for (String word : r.rhs.words)
				rhs += ", " + word;

			System.out.format("%8.4f%%   %8.4f%%   {%s} => {%s}%n", r.supportLHS * 100, r.getConfidence() * 100, lhs.substring(2), rhs.substring(2));

			cnt++;
			if (cnt == _limit)
				break;
		}
	}
}
