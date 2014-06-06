import java.io.BufferedReader;
import java.io.FileReader;

public class Reader {

	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(CbirWithSift.logFile1));
		String line = null;
		double stuff = 0;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			stuff += Double.parseDouble(line);
			count++;
		}
		System.out.println(stuff / count);
	}
}
