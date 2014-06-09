import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class KSpecialClassifier implements IClassifier {

	private final int KMEANS_ITERATIONS = 10;
	private final int _k;
	private double distortion = 0;
	Map<String, Vector<int[]>> medians;

	public KSpecialClassifier(int k) {
		_k = k;
		medians = new HashMap<>(_k);
	}

	@Override
	public String classify(int[] histogram) {
		String minClass = "unknown";
		double min = Double.MAX_VALUE;

		//return the image class with the most VisualWords
		for (String className : medians.keySet()) {
			for (int i = 0; i < medians.get(className).size(); i++) {
				double current = calculateDistance(histogram, medians.get(className).get(i));
				if (current < min) {
					min = current;
					minClass = className;
				}
			}
		}
		return minClass;
	}

	@Override
	public void learn(Map<String, Vector<int[]>> dataSet) {

		// fill arrays into one data structure
		Vector<int[]> vectors = new Vector<>();
		for (String classname : dataSet.keySet()) {
			vectors.addAll(dataSet.get(classname));
		}

		// sets randomly the medians
		Random rand = new Random();
		Map<String, Map<Integer, Vector<int[]>>> medianPoints = new HashMap<>();
		for (String classname : dataSet.keySet()) {
			Vector<int[]> data = dataSet.get(classname);
			medians.put(classname, new Vector<int[]>());
			medianPoints.put(classname, new HashMap<Integer, Vector<int[]>>());
			for (int i = 0; i < _k; i++) {
				int[] vectorRand = data.get(rand.nextInt(data.size()));
				int[] vector = new int[vectorRand.length];
				System.arraycopy(vectorRand, 0, vector, 0, vectorRand.length);
				medians.get(classname).add(vector);
				medianPoints.get(classname).put(medians.get(classname).size() - 1, new Vector<int[]>());
			}
		}

		int count = 0;
		while (count++ < KMEANS_ITERATIONS) {
			distortion = 0;
			for (String className : medianPoints.keySet()) {
				Vector<int[]> data = dataSet.get(className);

				for (Integer p : medianPoints.get(className).keySet())
					medianPoints.get(className).get(p).clear();

				// Jedes Objekt demjenigen Cluster zuordnen, dessen Schwerpunkt er am nächsten liegt.
				for (int i = 0; i < data.size(); i++) {
					double minDist = Double.MAX_VALUE;
					int minPoint = -1;
					//for (int[] v : medians.get(className)) {
					for (int j = 0; j < medians.get(className).size(); j++) {
						double dist = calculateDistance(medians.get(className).get(j), data.get(i));
						if (dist < minDist) {
							minDist = dist;
							minPoint = j;
						}
					}
					distortion = minDist * minDist;
					medianPoints.get(className).get(minPoint).add(data.get(i));
				}

				//  Die Cluster-Schwerpunkte anhand aller Cluster-Objekte neu berechnen.
				for (Integer medP : medianPoints.get(className).keySet()) {
					List<int[]> medPoints = medianPoints.get(className).get(medP);
					int[] w = medians.get(className).get(medP);
					int[] newD = new int[w.length];

					if (medPoints.size() > 0) {
						//sum
						for (int[] p : medPoints) {
							for (int i = 0; i < newD.length; i++)
								newD[i] += p[i];
						}
						for (int i = 0; i < newD.length; i++)
							newD[i] /= medPoints.size();

						if (!newD.equals(w)) {
							w = newD;
						}
					} else {
						int[] f = data.get(rand.nextInt(data.size())); //points[rand.nextInt(points.length)];
						System.arraycopy(f, 0, w, 0, f.length);
					}
				}
			}
		}
	}

	private static double calculateDistance(int[] vector1, int[] vector2) {
		double distanceVal = 0;
		for (int i = 0; i < vector1.length; i++) {
			double v = Math.abs(vector1[i] - vector2[i]);
			distanceVal += v * v;
		}
		return Math.sqrt(distanceVal);
	}

}
