

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Manages the details of EvilHangman. This class keeps tracks of the possible
 * words from a dictionary during rounds of hangman, based on guesses so far.
 *
 */
public class HangmanManager {

	// instance variables / fields
	private TreeSet<Character> guessed;
	private ArrayList<String> dict;
	private int remainingGuesses;
	private String secretWord;
	private HangmanDifficulty diff;
	private ArrayList<String> active;
	private boolean debug;

	/**
	 * Create a new HangmanManager from the provided set of words and phrases. pre:
	 * words != null, words.size() > 0
	 * 
	 * @param words   A set with the words for this instance of Hangman.
	 * @param debugOn true if we should print out debugging to System.out.
	 */
	public HangmanManager(Set<String> words, boolean debugOn) {
		if (words == null || words.size() <= 0) {
			throw new IllegalStateException("cannot be null, must have atleast 1 word");
		}
		debug = debugOn;
		dict = new ArrayList<>();
		Object[] temp = words.toArray();
		for (int index = 0; index < temp.length; index++) {
			dict.add((String) temp[index]);
		}
	}

	/**
	 * Create a new HangmanManager from the provided set of words and phrases.
	 * Debugging is off. pre: words != null, words.size() > 0
	 * 
	 * @param words A set with the words for this instance of Hangman.
	 */
	public HangmanManager(Set<String> words) {
		this(words, false);
	}

	/**
	 * Get the number of words in this HangmanManager of the given length. pre: none
	 * 
	 * @param length The given length to check.
	 * @return the number of words in the original Dictionary with the given length
	 */
	public int numWords(int length) {
		int numMatch = 0;
		for (int index = 0; index < dict.size(); index++) {
			if (dict.get(index).length() == length) {
				numMatch++;
			}
		}
		return numMatch;
	}

	/**
	 * Get for a new round of Hangman. Think of a round as a complete game of
	 * Hangman.
	 * 
	 * @param wordLen    the length of the word to pick this time. numWords(wordLen)
	 *                   > 0
	 * @param numGuesses the number of wrong guesses before the player loses the
	 *                   round. numGuesses >= 1
	 * @param diff       The difficulty for this round.
	 */
	public void prepForRound(int wordLen, int numGuesses, HangmanDifficulty diff) {
		if (wordLen <= 0 || numGuesses < 1) {
			throw new IllegalStateException("length of word must be greater than 1, "
					+ "max allowed guesses must be atleast 1");
		}
		remainingGuesses = numGuesses;
		this.diff = diff;
		active = new ArrayList<>();
		// use treeset because it is ordered, and doesnt allow duplicates
		guessed = new TreeSet<>();
		final char dash = '-';
		StringBuilder sb = new StringBuilder();
		for (int length = 0; length < wordLen; length++) {
			sb.append(dash);
		}
		secretWord = sb.toString();
		for (String currWord : dict) {
			if (currWord.length() == wordLen) {
				active.add(currWord);
			}
		}
	}

	/**
	 * The number of words still possible (live) based on the guesses so far.
	 * Guesses will eliminate possible words.
	 * 
	 * @return the number of words that are still possibilities based on the
	 *         original dictionary and the guesses so far.
	 */
	public int numWordsCurrent() {
		return active.size();
	}

	/**
	 * Get the number of wrong guesses the user has left in this round (game) of
	 * Hangman.
	 * 
	 * @return the number of wrong guesses the user has left in this round (game) of
	 *         Hangman.
	 */
	public int getGuessesLeft() {
		return remainingGuesses;
	}

	/**
	 * Return a String that contains the letters the user has guessed so far during
	 * this round. The characters in the String are in alphabetical order. The
	 * String is in the form [let1, let2, let3, ... letN]. For example [a, c, e, s,
	 * t, z]
	 * 
	 * @return a String that contains the letters the user has guessed so far during
	 *         this round.
	 */
	public String getGuessesMade() {
		return guessed.toString();
	}

	/**
	 * Check the status of a character.
	 * 
	 * @param guess The character to check.
	 * @return true if guess has been used or guessed this round of Hangman, false
	 *         otherwise.
	 */
	public boolean alreadyGuessed(char guess) {
		return guessed.contains(guess);
	}

	/**
	 * Get the current pattern. The pattern contains '-''s for unrevealed (or
	 * guessed) characters and the actual character for "correctly guessed"
	 * characters.
	 * 
	 * @return the current pattern.
	 */
	public String getPattern() {
		return secretWord;
	}

	/**
	 * Update the game status (pattern, wrong guesses, word list), based on the give
	 * guess.
	 * 
	 * @param guess pre: !alreadyGuessed(ch), the current guessed character
	 * @return return a tree map with the resulting patterns and the number of words
	 *         in each of the new patterns. The return value is for testing and
	 *         debugging purposes.
	 */
	public TreeMap<String, Integer> makeGuess(char guess) {
		if (guessed.contains(guess)) {
			throw new IllegalStateException("guess has already been made");
		}
		guessed.add(guess);
		HashMap<String, ArrayList<String>> secretPatterns= new HashMap<>();
		//goes through the active word List and creates a pattern for it
		//if the pattern exists, add current word to key, if not then add key and word to map
		for (int index = 0; index < active.size(); index++) {
			String currentPattern = createPattern(active.get(index), guess);
			if (secretPatterns.containsKey(currentPattern)) {
				secretPatterns.get(currentPattern).add(active.get(index));
			} else {
				ArrayList<String> wordsInPattern = new ArrayList<>();
				wordsInPattern.add(active.get(index));
				secretPatterns.put(currentPattern, wordsInPattern);
			}
		}
		//copying the patterns into a tree map and adding the length of the list associated
		//with each key 
		TreeMap<String, Integer> appearencesInPattern = new TreeMap<>();
		for (Map.Entry<String, ArrayList<String>> set : secretPatterns.entrySet()) {
			appearencesInPattern.put(set.getKey(), set.getValue().size());
		}
		updatePattern(secretPatterns, guess);
		
		return appearencesInPattern;
	}

	// updates the secret pattern, based on difficulty and increases guesses if it needs to
	//also updates the list of active words
	private void updatePattern(HashMap<String, ArrayList<String>> secretPatterns, char guess) {
		String newPattern = chooseNewPattern(secretPatterns, guess);
		if(newPattern.equals(secretWord)) {
			remainingGuesses--;	
		}else {
			secretWord = newPattern;
		}
		//update list of active words
		ArrayList<String> temp = secretPatterns.get(secretWord);
		active.clear();
		for(String s : temp) {
			active.add(s);
		}
	}

	//chooses which pattern will be next based on difficulty chosen 
	private String chooseNewPattern(HashMap<String, ArrayList<String>> map, char guess) {
		ArrayList<PatternInfo> info = new ArrayList<>();
		// did this so I can sort the patterns lexographically by using nested class and be able
		// to use it when determining when to pick what pattern
		for (Map.Entry<String, ArrayList<String>> m : map.entrySet()) {
			info.add(new PatternInfo(m.getKey(), m.getValue(), guess));
		}
		Collections.sort(info);
		final int EVERY_FOURTH = 4; //in medium, every fourth guess chooses 2nd hardest
		final int EVERY_SECOND = 2; // in easy, every 2 guesses, picks 2nd hardest list
		//if it is easy and if there is a second most difficult list, return the second one
		//else if it is medium and if there is a second most difficult
		if (info.size() > 1 && diff.equals(HangmanDifficulty.EASY)
				&& guessed.size() % EVERY_SECOND == 0) {
			return info.get(1).pattern();
		} else if (info.size() > 1 && diff.equals(HangmanDifficulty.MEDIUM)
				&& guessed.size() % EVERY_FOURTH == 0) {
			return info.get(1).pattern();
		}
		return info.get(0).pattern();
	}

	// creates a secret pattern for this word based on if the guess matches, if it
	// doesnt add the current secretwords char
	private String createPattern(String currWord, char guess) {
		StringBuilder sb = new StringBuilder();
		for (int index = 0; index < currWord.length(); index++) {
			if (currWord.charAt(index) == guess) {
				sb.append(guess);
			} else {
				sb.append(secretWord.charAt(index));
			}
		}
		return sb.toString();
	}

	/**
	 * Return the secret word this HangmanManager finally ended up picking for this
	 * round. If there are multiple possible words left one is selected at random.
	 * <br>
	 * pre: numWordsCurrent() > 0
	 * 
	 * @return return the secret word the manager picked.
	 */
	public String getSecretWord() {
		//TODO simplify this using rng.nextInt(active.size);
		if(numWordsCurrent() <= 0) {
			throw new IllegalStateException("current active words must be atleast 1");
		}
		Random rng = new Random();
		int index = rng.nextInt(active.size());
		return active.get(index);
	}

	// class used to sort lexographically easier
	//stores the pattern and how many words are associated with this pattern
	public static class PatternInfo implements Comparable<PatternInfo> {
		private String pattern;
		private int numWords;
		private char guess;

		//pre: checked in the other methods
		//creates a PatternInfo object used to get info on the secretPatterns
		public PatternInfo(String key, ArrayList<String> words, char guess) {
			pattern = key;
			numWords = words.size();
			this.guess = guess;
		}

		//returns the secret pattern of this object
		public String pattern() {
			return pattern;
		}
		
		//gets the number of times the guess appears in the pattern
		private int getAppearences() {
			int appearences = 0;
			for(int index = 0; index < pattern.length(); index++) {
				if(pattern.charAt(index) == guess) {
					appearences++;
				}
			}
			
			return appearences;
		}

		//compares two PatternInfo objects to choose which one is "harder"
		//based on sinze, appearences of guess, and lexographical order
		public int compareTo(PatternInfo other) {
			// if sizes are different it will return this result
			int sizeDiff = other.numWords - numWords;
			if (sizeDiff != 0) {
				return sizeDiff;
			}
			int appearsInThis = getAppearences();
			int appearsInOther = other.getAppearences();
			// if the guess appears in each pattern different times, returns the difference
			if (appearsInThis - appearsInOther != 0) {
				return appearsInThis - appearsInOther;
			}
			// if difference can't be told between the above 2, use strings compare to
			return pattern.compareTo(other.pattern);
		}
	}
}
