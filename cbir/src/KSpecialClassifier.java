import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

public class KSpecialClassifier implements IClassifier {

	private class ClassifyPoint implements Comparable<ClassifyPoint> {
		double distance;
		String className;

		@Override
		public int compareTo(ClassifyPoint o) {
			return Double.compare(distance, o.distance);
		}

	}

	private final int KMEANS_ITERATIONS = 10;
	private final int COMPARE_SIZE = 3;
	private final double COMPARE_MAX_DISTANCE = 0.4;

	private final int _k;
	public double distortion = 0;
	Map<String, Vector<int[]>> medians;

	public KSpecialClassifier(int k) {
		_k = k;
		medians = new HashMap<>(_k);
	}

	@Override
	public String classify(int[] histogram) {
		Map<String, Set<ClassifyPoint>> top = new HashMap<>();

		//return the image class with the most VisualWords
		for (String className : medians.keySet()) {
			TreeSet<ClassifyPoint> set = new TreeSet<>();

			top.put(className, set);
			for (int i = 0; i < medians.get(className).size(); i++) {
				double current = calculateDistance(histogram, medians.get(className).get(i));

				ClassifyPoint p = new ClassifyPoint();
				p.className = className;
				p.distance = current;
				set.add(p);
				if (set.size() > COMPARE_SIZE)
					set.pollLast();
			}
		}

		PriorityQueue<ClassifyPoint> pq = new PriorityQueue<>();
		for (String className : medians.keySet()) {
			pq.addAll(top.get(className));
		}

		ArrayList<ClassifyPoint> points = new ArrayList<>(COMPARE_SIZE);
		ClassifyPoint p0 = pq.poll();
		points.add(p0);
		for (int i = 1; i < COMPARE_SIZE; i++) {
			ClassifyPoint pi = pq.poll();
			if (p0.distance / pi.distance < COMPARE_MAX_DISTANCE)
				points.add(pi);
			else {
				break;
			}
		}

		int maxOccurence = 0;
		double maxDistance = Double.MAX_VALUE;
		String maxClassName = null;
		for (String className : medians.keySet()) {
			int occurences = 0;
			double accDistance = 0;
			for (int i = 0; i < points.size(); i++) {
				if (points.get(i).className.equals(className)) {
					occurences++;
					accDistance += points.get(i).distance;
				}
			}

			if (occurences >= maxOccurence) {
				if (occurences == maxOccurence) {
					if (maxDistance > accDistance) {
						maxOccurence = occurences;
						maxClassName = className;
						maxDistance = accDistance;
					}
				} else {
					maxOccurence = occurences;
					maxClassName = className;
					maxDistance = accDistance;
				}
			}
		}

		return maxClassName;
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
		double lastSavedDistortion = Double.MAX_VALUE;

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
					distortion += minDist * minDist;
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
