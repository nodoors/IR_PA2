import java.util.*;
import java.io.*;

public class Dictionary {
	static ArrayList<String> stopList = new ArrayList<String>();

	public static void main(String[] args) {

		Scanner readStop = null;
		try {
			readStop = new Scanner(new FileInputStream("stoplist.txt"));
		} catch (FileNotFoundException e) {
			System.out.println("File not found.");
			System.exit(0);
		}
		while (readStop.hasNext()) {
			stopList.add(readStop.next());
		}

		HashMap<String, Integer> DF = new HashMap<String, Integer>(); // 存term與DF的對照
		HashMap<String, Double> IDF = new HashMap<String, Double>(); // 存term與IDF的對照
		HashMap<String, Integer> tID = new HashMap<String, Integer>(); // 存term與t_index的對照
		HashMap<String, Double> TFIDF = new HashMap<String, Double>(); // 存term的TFIDF

		// 這三個可以先算好以節省時間，時間就是金錢啊
		DF = DF();
		IDF = IDF(DF());
		tID = tID(DF());

		// 將Dictionary轉成List，並按字母順序重新排列
		List<Map.Entry<String, Integer>> list_Data = new ArrayList<Map.Entry<String, Integer>>(
				tID.entrySet());
		Collections.sort(list_Data,
				new Comparator<Map.Entry<String, Integer>>() {
					public int compare(Map.Entry<String, Integer> entry1,
							Map.Entry<String, Integer> entry2) {
						return (entry1.getKey().compareTo(entry2.getKey()));
					}
				});
		try {
			// Create file
			FileWriter fstream = new FileWriter("dictionary.txt");
			BufferedWriter fileOut = new BufferedWriter(fstream);
			fileOut.write("t_index term df\r\n");
			for (Map.Entry<String, Integer> entry : list_Data) {
				fileOut.write(entry.getValue() + " " + entry.getKey() + " "
						+ DF.get(entry.getKey()));
				fileOut.write("\r\n");
			}

			fileOut.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		// 把每篇文章每個term的TFIDF算出來照順序排好
		for (int i = 1; i <= 1095; i++) {
			TFIDF = TFIDF(normalTF(tokenize(i)), IDF);
			List<Map.Entry<String, Double>> list_Data2 = new ArrayList<Map.Entry<String, Double>>(
					TFIDF.entrySet());
			Collections.sort(list_Data2,
					new Comparator<Map.Entry<String, Double>>() {
						public int compare(Map.Entry<String, Double> entry1,
								Map.Entry<String, Double> entry2) {
							return (entry1.getKey().compareTo(entry2.getKey()));
						}
					});
			try {
				// Create file
				FileWriter fstream = new FileWriter("TFIDF/" + i + ".txt");
				BufferedWriter fileOut = new BufferedWriter(fstream);

				for (Map.Entry<String, Double> entry : list_Data2) {
					fileOut.write(tID.get(entry.getKey()) + " "
							+ entry.getValue());
					fileOut.write("\r\n");
				}

				fileOut.close();
			} catch (Exception e) {// Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
		}
		System.out.println(cosine(1, 2, IDF));
	}

	// 輸入文章的ID回傳一個tokenize過的ArrayList
	public static ArrayList<String> tokenize(int file) {

		Scanner fileIn = null;
		ArrayList<String> words = new ArrayList<String>();

		try {
			fileIn = new Scanner(new FileInputStream("IRTM/" + file + ".txt"));
		} catch (FileNotFoundException e) {
			System.out.println("File not found.");
			System.exit(0);
		}

		while (fileIn.hasNext()) {
			Stemmer s = new Stemmer();
			String token = fileIn.next().toLowerCase()
					.replaceAll("[^a-zA-Z0-9]", ""); // 將非字母與數字的char清除

			// 濾掉stop word
			boolean isStop = false;
			for (int i = 0; i < stopList.size(); i++) {
				if (token.equals(stopList.get(i)) || token.equals("")) {
					isStop = true;
					break;
				}
			}
			if (!isStop) {
				// 用stemmer將字stem
				for (int k = 0; k < token.length(); k++) {
					s.add(token.charAt(k));
				}
				s.stem();

				// 在濾一次stop word
				isStop = false;
				for (int i = 0; i < stopList.size(); i++) {
					if (s.toString().equals(stopList.get(i))
							|| s.toString().equals("")) {
						isStop = true;
						break;
					}
				}
				if (!isStop) {
					words.add(s.toString());
				}
			}

		}
		fileIn.close();
		// readStop.close();
		return words;
	}

	// 輸入ArrayList回傳TF，且TF為normalize過的
	public static HashMap<String, Double> normalTF(ArrayList<String> tokens) {
		HashMap<String, Integer> TF = new HashMap<String, Integer>();
		HashMap<String, Double> normalTF = new HashMap<String, Double>();

		for (String term : tokens) {
			if (TF.get(term) == null) {
				TF.put(term, 1);
			} else {
				TF.put(term, TF.get(term) + 1);

			}
		}

		for (String term : tokens) {
			normalTF.put(term,
					(double) (TF.get(term)) / (double) (tokens.size()));
		}
		return normalTF;
	}

	// 從每一篇文章的TF求得DF
	public static HashMap<String, Integer> DF() {
		HashMap<String, Double> tf = new HashMap<String, Double>();
		HashMap<String, Integer> df = new HashMap<String, Integer>();
		for (int i = 1; i <= 1095; i++) {
			tf = normalTF(tokenize(i));
			for (String term : tf.keySet()) {
				if (df.containsKey(term)) {
					df.put(term, df.get(term) + 1);
				} else {
					df.put(term, 1);
				}
			}
		}
		return df;
	}

	// 從DF算出IDF = log(N/(1+DF))
	public static HashMap<String, Double> IDF(HashMap<String, Integer> DF) {

		HashMap<String, Double> idf = new HashMap<String, Double>();
		for (String term : DF.keySet()) {
			idf.put(term,
					Math.log10((double) (1095 / (double) (1 + DF.get(term)))));
		}
		return idf;
	}

	// 輸入DF，每個term給他一個t_index
	public static HashMap<String, Integer> tID(HashMap<String, Integer> DF) {
		HashMap<String, Integer> tID = new HashMap<String, Integer>();
		List<Map.Entry<String, Integer>> list_Data = new ArrayList<Map.Entry<String, Integer>>(
				DF.entrySet());
		Collections.sort(list_Data,
				new Comparator<Map.Entry<String, Integer>>() {
					public int compare(Map.Entry<String, Integer> entry1,
							Map.Entry<String, Integer> entry2) {
						return (entry1.getKey().compareTo(entry2.getKey()));
					}
				});
		int ID = 1;
		for (Map.Entry<String, Integer> entry : list_Data) {
			tID.put(entry.getKey(), ID);
			ID++;
		}

		return tID;
	}

	// 輸入TF 跟IDF算出TFIDF，並除以長度算出uni vector
	public static HashMap<String, Double> TFIDF(
			HashMap<String, Double> normalTF, HashMap<String, Double> IDF) {

		HashMap<String, Double> TFIDF = new HashMap<String, Double>();
		HashMap<String, Double> uniVector = new HashMap<String, Double>();
		for (String term : normalTF.keySet()) {
			TFIDF.put(term, (normalTF.get(term)) * IDF.get(term));
		}
		double length = 0;
		for (String term : TFIDF.keySet()) {
			length += Math.pow(TFIDF.get(term), 2);
		}
		length = Math.pow(length, 0.5);
		for (String term : TFIDF.keySet()) {
			uniVector.put(term, TFIDF.get(term) / length);
		}
		return uniVector;
	}

	// 輸入兩篇文章ID，回傳cosine的值
	public static double cosine(int x, int y, HashMap<String, Double> IDF) {

		HashMap<String, Double> uniX = new HashMap<String, Double>();
		HashMap<String, Double> uniY = new HashMap<String, Double>();

		uniX = TFIDF(normalTF(tokenize(x)), IDF);
		uniY = TFIDF(normalTF(tokenize(y)), IDF);

		double cos = 0;
		for (String term : uniX.keySet()) {
			if (uniY.containsKey(term)) {
				cos += uniX.get(term) * uniY.get(term);
			}
		}
		return cos;
	}
}
