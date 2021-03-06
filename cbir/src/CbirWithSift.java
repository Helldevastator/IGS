/**
 * Extract and display Scale Invariant Features after the method of David Lowe
 * \cite{Lowe04} in an image.
 * 
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author    = {David G. Lowe},
 *   title     = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal   = {International Journal of Computer Vision},
 *   year      = {2004},
 *   volume    = {60},
 *   number    = {2},
 *   pages     = {91--110},
 * }
 * </pre>
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import mpi.cbg.fly.Feature;
import mpi.cbg.fly.Filter;
import mpi.cbg.fly.FloatArray2D;
import mpi.cbg.fly.FloatArray2DSIFT;

@SuppressWarnings("serial")
public class CbirWithSift extends JFrame {
	public static final Object logLock = new Object();
	public static final String logFile1 = "tests.txt";

	Thread t;
	//helper variables for the repaint
	IgsImage cur_image;
	//the extracted visual words - model for the VisualWordHistogram 
	List<VisualWord> bagofwords = new Vector<VisualWord>();
	private double kDistortion;

	//how many visual words should be classified
	private static int K = 100;
	//the minimum count of members in a "visual-word" class
	private static int MIN_CLASS_SIZE = 10;
	private static int KMEANS_ITERATIONS = 10;
	private static int steps = 6;
	public static Type distance = Type.EUCLIDIAN;
	private static int KMEANS_RANDOM_TRIES = 1;
	private static double KMEANS_DISTORTION_SAVE = 810.0;
	private static double KMEANS_DISTORTION_ABORT = 730.0;

	private static final boolean CHOOSE_IMAGES_RANDOMLY = true;
	private static final String TRAINING_DIR = "Training";
	private static final String TEST_DIR = "Test";
	public static final String logFile = null;
	//how many images should be read from the input folders set to max for final run
	private static int readImages = 50;

	//number of SIFT iterations: more steps will produce more features 
	//default = 4

	//for testing: delay time for showing images in the GUI
	private static int wait = 0;

	public enum Type {
		EUCLIDIAN, MANHATTEN, WEIGHTED_EUCLIDIAN, CHEBYSHEV, MIN, IRIS, NON_NORM
	};

	private static double calculateDistance(float[] descriptors1, float[] descriptors2) {
		if (descriptors1 == null || descriptors2 == null)
			return -1;
		switch (distance) {
		case EUCLIDIAN:
			double distanceVal = 0;
			for (int i = 0; i < descriptors1.length; i++) {
				double v = Math.abs(descriptors1[i] - descriptors2[i]);
				distanceVal += v * v;
			}
			return Math.sqrt(distanceVal);
		case MANHATTEN:

		case WEIGHTED_EUCLIDIAN:

		case CHEBYSHEV:

		case MIN:

		case IRIS:

		case NON_NORM:

		default:
			throw new RuntimeException("This method to be implemented.");
		}

	}

	/**
	 * 
	 * IMPLEMENT THIS METHOD
	 * 
	 * 
	 * Classifies a SIFT-Feature Vector into a VisualWord Class by finding the
	 * nearest visual word in the bagofwords "space"
	 * 
	 * @param f
	 *            a SIFT feature
	 * @return the class ID (0..k) or null if quality is not good enough
	 */
	public Integer doClassifyVisualWord(Feature f) {
		double minDistance = Double.MAX_VALUE;
		Integer result = null;
		for (VisualWord w : bagofwords) {
			double val = calculateDistance(f.descriptor, w.descriptor);
			if (val < minDistance) {
				minDistance = val;
				result = w.classID;
			}
		}
		return result;
	}

	/**
	 * IMPLEMENT THIS METHOD
	 * 
	 * 
	 * a clustering implementation for SIFT-Features: (float[] :
	 * Feature.descriptor)
	 * 
	 * @param _points
	 *            a list of all found features in the training set
	 * @param K
	 *            how many classes (visual words)
	 * @param minCount
	 *            the minimum number of members in each class
	 * @return the centroides of the k-mean = visual words list
	 */
	public List<VisualWord> doClusteringVisualWords(final Feature[] points, int K, int minCount) {
		System.out.println("Start clustering with: " + points.length + " pkt to " + K + " classes");

		Random rand = new Random();
		List<VisualWord> medians = new ArrayList<>(K);
		Map<Integer, List<Feature>> medianPoints = new HashMap<>(2 * K);
		for (int i = 0; i < K; i++) {
			VisualWord w = new VisualWord();
			int r = rand.nextInt(points.length);
			Feature f = points[r];
			w.classID = i;
			w.descriptor = new float[f.descriptor.length];
			System.arraycopy(f.descriptor, 0, w.descriptor, 0, w.descriptor.length);
			medians.add(w);
			medianPoints.put(i, new ArrayList<Feature>());
		}

		int count = 0;
		double lastSavedDistortion = Double.MAX_VALUE;
		List<VisualWord> savedMedians = null;
		double distortion = Double.MAX_VALUE;
		while (count++ < KMEANS_ITERATIONS && KMEANS_DISTORTION_ABORT < distortion) {

			if (distortion < KMEANS_DISTORTION_SAVE && distortion < lastSavedDistortion) {
				lastSavedDistortion = distortion;
				savedMedians = new ArrayList<>(medians.size());
				//deep copy
				for (int i = 0; i < medians.size(); i++) {
					VisualWord w = new VisualWord();
					VisualWord tocp = medians.get(i);
					w.classID = tocp.classID;
					w.descriptor = new float[tocp.descriptor.length];
					System.arraycopy(tocp.descriptor, 0, w.descriptor, 0, w.descriptor.length);
					savedMedians.add(w);
				}
			}

			distortion = 0;
			for (Integer p : medianPoints.keySet())
				medianPoints.get(p).clear();

			// Jedes Objekt demjenigen Cluster zuordnen, dessen Schwerpunkt er am nächsten liegt.
			for (int i = 0; i < points.length; i++) {
				double minDist = Double.MAX_VALUE;
				int minPoint = 0;
				for (Integer p : medianPoints.keySet()) {

					double dist = calculateDistance(medians.get(p).descriptor, points[i].descriptor);
					if (dist < minDist) {
						minDist = dist;
						minPoint = p;
					}
				}
				distortion += minDist * minDist;
				medianPoints.get(minPoint).add(points[i]);
			}

			//  Die Cluster-Schwerpunkte anhand aller Cluster-Objekte neu berechnen.
			for (Integer medP : medianPoints.keySet()) {
				List<Feature> medPoints = medianPoints.get(medP);
				VisualWord w = medians.get(medP);
				float[] newD = new float[w.descriptor.length];
				if (medPoints.size() > minCount) {
					//sum
					for (Feature p : medPoints) {
						for (int i = 0; i < newD.length; i++)
							newD[i] += p.descriptor[i];
					}

					for (int i = 0; i < newD.length; i++)
						newD[i] = newD[i] / medPoints.size();

					if (!newD.equals(w.descriptor)) {
						w.descriptor = newD;
					}
				} else {
					Feature f = points[rand.nextInt(points.length)];
					System.arraycopy(f.descriptor, 0, w.descriptor, 0, f.descriptor.length);
				}
			}
		}

		if (lastSavedDistortion < distortion) {
			kDistortion = lastSavedDistortion;
			return savedMedians;
		} else {
			kDistortion = distortion;
			return medians;
		}

	}

	/* Do not change anything from here */

	// initial sigma
	private static float initial_sigma = 1.6f;
	// feature descriptor size
	private static int fdsize = 4;
	// feature descriptor orientation bins
	private static int fdbins = 8;
	// size restrictions for scale octaves, use octaves < max_size and > min_size only
	private static int min_size = 64;
	private static int max_size = 1024;

	public CbirWithSift() throws IOException {
		super("Clustering");
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(500, 400);

		t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					setTitle("Learning: readData");
					LinkedList<IgsImage> trainingImages = readImages(TRAINING_DIR, readImages);

					setTitle("Learning: VisualWord by Clustering");

					Vector<Feature> allLearnFeatchers = new Vector<Feature>();
					for (IgsImage i : trainingImages)
						allLearnFeatchers.addAll(i.features);

					long startTimeVW = System.currentTimeMillis();
					//calculate the visual words with k-means
					bagofwords = doClusteringVisualWords(allLearnFeatchers.toArray(new Feature[0]), K, MIN_CLASS_SIZE);
					long endTimeVW = System.currentTimeMillis();

					setTitle("Show: visualWords in TraningsData");
					Map<String, Vector<int[]>> imageContentTrainingData = new HashMap<String, Vector<int[]>>();

					//create the VisiualWordHistograms for each training image
					for (IgsImage i : trainingImages) {
						if (!imageContentTrainingData.containsKey(i.className))
							imageContentTrainingData.put(i.className, new Vector<int[]>());
						int[] ImageVisualWordHistogram = new int[K];

						for (Feature f : i.features) {
							Integer wordClass = doClassifyVisualWord(f);
							if (wordClass != null)
								ImageVisualWordHistogram[wordClass.intValue()]++;
						}

						imageContentTrainingData.get(i.className).add(ImageVisualWordHistogram);

						cur_image = i;
						repaint();
						Thread.sleep(wait);
					}

					long startTimeDM = System.currentTimeMillis();
					setTitle("Learning: decisionModel");

					//IClassifier classifier = new StatisticClassifier(K);
					//IClassifier classifier = new SchwambiClassifier();
					IClassifier classifier = new KSpecialClassifier(20);
					classifier.learn(imageContentTrainingData);
					long endTimeDM = System.currentTimeMillis();

					setTitle("Testing: readData");
					LinkedList<IgsImage> testImages = readImages(TEST_DIR, readImages);

					long startTime = System.currentTimeMillis();

					Map<String, Integer> classStat = new HashMap<String, Integer>();
					int total = testImages.size();
					int success = 0;
					setTitle("Verify: test data");

					//create the VisiualWordHistograms for each test image and classify it
					for (IgsImage i : testImages) {
						int[] ImageVisualWordHistogram = new int[K];

						for (Feature f : i.features) {
							Integer wordClass = doClassifyVisualWord(f);
							if (wordClass != null)
								ImageVisualWordHistogram[wordClass.intValue()]++;
						}

						i.classifiedName = classifier.classify(ImageVisualWordHistogram);
						if (classStat.containsKey(i.classifiedName)) {
							classStat.put(i.classifiedName, classStat.get(i.classifiedName) + 1);
						} else {
							classStat.put(i.classifiedName, 1);
						}

						if (i.isClassificationCorect())
							success++;

						cur_image = i;
						repaint();
						Thread.sleep(wait);
					}

					long endTime = System.currentTimeMillis();

					System.out.println("Verified " + success / (double) testImages.size() * 100 + "% in " + (endTime - startTime) + "ms");
					System.out.println("Learned " + K + " Visual Words in: " + (endTimeVW - startTimeVW) + "ms!");
					System.out.println("Learned the image classification in: " + (endTimeDM - startTimeDM) + "ms");

					System.out.println();
					for (Entry<String, Integer> e : classStat.entrySet()) {
						System.out.println("Classified " + 100 * e.getValue() / (double) total + "% as " + e.getKey() + ".");
					}

					/*
					 * synchronized (logLock) { KSpecialClassifier ks =
					 * (KSpecialClassifier) classifier; FileWriter fw = new
					 * FileWriter(logFile1, true); //the true will append the
					 * new data fw.write(String.valueOf(success / (double)
					 * testImages.size() * 100) + ";" + kDistortion + ";" +
					 * ks.distortion + "\n");//appends the string to the file
					 * fw.close(); }
					 */

				} catch (Exception _e) {
					_e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Reads maxImages from a folder, calculates the SIFT features and wraps the
	 * results into a IgsImage also paints each image on the GUI
	 * 
	 * @param folder
	 * @param maxImages
	 * @return the list of read IgsImages
	 * @throws IOException
	 * @throws InterruptedException
	 */
	LinkedList<IgsImage> readImages(String folder, int maxImages) throws IOException, InterruptedException {
		LinkedList<IgsImage> images = new LinkedList<IgsImage>();

		File actual = new File("./images/" + folder);

		int i = 0;

		List<File> files = new ArrayList<File>();
		for (File f : actual.listFiles()) {
			files.add(f);
		}

		if (CHOOSE_IMAGES_RANDOMLY) {
			Collections.shuffle(files);
		} else {
			Collections.sort(files);
		}

		int p = Runtime.getRuntime().availableProcessors();
		System.out.println("Pool with " + p + " Threads created.");
		ExecutorService pool = Executors.newFixedThreadPool(p);
		LinkedList<Future<IgsImage>> futures = new LinkedList<Future<IgsImage>>();

		for (File f : files) {
			if (!f.getName().equals(".svn")) {
				if (i++ > maxImages)
					break;
				IgsImage image = new IgsImage();

				futures.add(pool.submit(new ImageRunnable(f, image), image));
			}
		}

		pool.shutdown();
		pool.awaitTermination(1, TimeUnit.DAYS);

		for (Future<IgsImage> f : futures) {
			try {
				images.add(f.get());
			} catch (ExecutionException e) {
				System.err.println("image not added");
			}
		}

		return images;
	}

	public class ImageRunnable implements Runnable {
		File file;
		IgsImage image;

		public ImageRunnable(File f, IgsImage i) {
			file = f;
			image = i;
		}

		@Override
		public void run() {
			try {
				image.image = ImageIO.read(file);
				image.className = file.getName().substring(0, file.getName().indexOf('_'));
				image.features = calculateSift(image.image);
			} catch (Exception e) {
				image = null;
			}
		}
	}

	/**
	 * draws a rotated square with center point center, having size and
	 * orientation
	 */
	static void drawSquare(Graphics _g, double[] o, double scale, double orient, Integer _class) {
		scale /= 2;

		double sin = Math.sin(orient);
		double cos = Math.cos(orient);

		int[] x = new int[6];
		int[] y = new int[6];

		x[0] = (int) (o[0] + (sin - cos) * scale);
		y[0] = (int) (o[1] - (sin + cos) * scale);

		x[1] = (int) o[0];
		y[1] = (int) o[1];

		x[2] = (int) (o[0] + (sin + cos) * scale);
		y[2] = (int) (o[1] + (sin - cos) * scale);
		x[3] = (int) (o[0] - (sin - cos) * scale);
		y[3] = (int) (o[1] + (sin + cos) * scale);
		x[4] = (int) (o[0] - (sin + cos) * scale);
		y[4] = (int) (o[1] - (sin - cos) * scale);
		x[5] = x[0];
		y[5] = y[0];

		//if(_class==null || _class.intValue()==92 || _class.intValue()==69 || _class.intValue()==91) {

		_g.setColor(Color.red);
		_g.drawPolygon(new Polygon(x, y, x.length));
		_g.setColor(Color.yellow);
		if (_class != null)
			_g.drawString(_class + "", x[0], y[0]);
		//}

	}

	@Override
	public synchronized void paint(Graphics _g) {

		_g.clearRect(0, 0, 1000, 1000);

		if (cur_image == null)
			return;

		_g.drawImage(cur_image.image, 0, 0, null);

		_g.setColor(cur_image.isClassificationCorect() ? Color.green : Color.red);

		_g.drawString(cur_image.className + " > " + cur_image.classifiedName, 20, cur_image.image.getHeight() + 40);

		if (cur_image.features != null)
			for (Feature f : cur_image.features)
				drawSquare(_g, new double[] { f.location[0], f.location[1] }, fdsize * 4.0 * f.scale, f.orientation, doClassifyVisualWord(f));

	}

	public static FloatArray2D ImageToFloatArray2D(BufferedImage image) throws IOException {
		FloatArray2D image_float = null;

		int count = 0;
		image_float = new FloatArray2D(image.getWidth(), image.getHeight());

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgbV = image.getRGB(x, y);
				int b = rgbV & 0xff;
				rgbV = rgbV >> 8;
				int g = rgbV & 0xff;
				rgbV = rgbV >> 8;
				int r = rgbV & 0xff;
				image_float.data[count++] = 0.3f * r + 0.6f * g + 0.1f * b;
			}
		}

		return image_float;
	}

	public static void main(String[] _args) throws Exception {
		//I know this is ugly, but I am lazy

		CbirWithSift s = new CbirWithSift();
	}

	private Vector<Feature> calculateSift(BufferedImage image) throws IOException {

		Vector<Feature> _features = new Vector<Feature>();

		FloatArray2DSIFT sift = new FloatArray2DSIFT(fdsize, fdbins);

		FloatArray2D fa = ImageToFloatArray2D(image);
		Filter.enhance(fa, 1.0f);

		fa = Filter.computeGaussianFastMirror(fa, (float) Math.sqrt(initial_sigma * initial_sigma - 0.25));

		long start_time = System.currentTimeMillis();

		sift.init(fa, steps, initial_sigma, min_size, max_size);
		_features = sift.run(max_size);

		System.out.println("processing SIFT took " + (System.currentTimeMillis() - start_time) + "ms to find \t" + _features.size() + " features");

		return _features;
	}

}
