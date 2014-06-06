import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


public class SchwambiClassifier implements IClassifier {
	
	Map<String, int[]> medians = new HashMap<>();
	
	@Override
	public String classify(int[] histogram) {
		String minClass = "unknown";
		double min = Double.MAX_VALUE;
				
		//return the image class with the most VisualWords
		for(String className : medians.keySet())
		{
			double current = calculateDistance(histogram, medians.get(className));
			if(current < min) {
				min = current;
				minClass = className;
			}
		}
				
		return minClass;
	}

	@Override
	public void learn(Map<String, Vector<int[]>> dataSet) {
		
		for (String s : dataSet.keySet()) {
			Vector<int[]> elements = dataSet.get(s);
			int[] med = new int[elements.firstElement().length];
			for (int[] h : elements) {
				for (int i = 0; i < med.length; i++) med[i] += h[i];
			}
			for (int i = 0; i < med.length; i++) med[i] /= elements.size();
			medians.put(s,  med);
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
