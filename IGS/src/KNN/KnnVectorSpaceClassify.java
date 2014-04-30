package KNN;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class KnnVectorSpaceClassify {

	//enable/disable stopwords/stemming filtering
	static final boolean filter = true;

	private final static boolean debug = false;

	/**
	 * 
	 * @param train
	 *            all "known" training documents
	 * @param test
	 *            all "unknown" test documents
	 * @param k
	 *            parameter for the knn majority decision
	 * 
	 * @return an overall error value as sum over all test case errors test case
	 *         error 0 : exact classification C == C1 0.5 : tie classification C
	 *         in {C1 .. Cn} 1 : false classification C != C1
	 * 
	 * 
	 */
	static double knnClassify(List<IGSDocument> train, List<IGSDocument> test, int k) {

		double error = 0;

		return error;
	}

	public static void main(String[] _args) throws Exception {

		//read all training documents
		List<IGSDocument> training = readDocuments("training1");

		//read all test documents
		List<IGSDocument> test = readDocuments("test");

		//classify each test document based on training set for different k 
		System.out.println("k\ttotal error ");
		for (int k = 1; k < 71; k++)
			System.out.println(k + "\t" + knnClassify(training, test, k));
	}

	/**
	 * read input documents and apply stemming
	 * 
	 * @param dirName
	 *            the directory for all documents
	 * @return a list of read documents from the directory
	 */
	static List<IGSDocument> readDocuments(String dirName) {
		List<IGSDocument> res = new LinkedList<IGSDocument>();
		File actual = new File(dirName);
		for (File f : actual.listFiles()) {
			IGSDocument doc = new IGSDocument(f, filter);
			res.add(doc);
			if (debug)
				System.out.println(doc);
		}

		return res;
	}
}
