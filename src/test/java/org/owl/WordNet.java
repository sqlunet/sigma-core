/*
 * Copyright (c) 2022.
 * This code is copyright Bernard Bou <1313ou@gmail.com>
 * This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
 */

package org.owl;

import org.sigma.core.NotNull;
import org.sigma.core.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordNet
{
	enum POS
	{
		NOUN, VERB, ADJ, ADV, ADJSAT;

		int toNum()
		{
			if (this == ADJSAT)
			{
				return ADJ.ordinal() + 1;
			}
			return this.ordinal() + 1;
		}

		static POS parse(char pos)
		{
			switch (pos)
			{
				case 'n':
					return NOUN;
				case 'v':
					return VERB;
				case 'a':
					return ADJ;
				case 'r':
					return ADV;
				case 's':
					return ADJSAT;
			}
			throw new IllegalArgumentException(Character.toString(pos));
		}

		String toSynset9(String synset8)
		{
			return toNum() + synset8;
		}
	}

	/**
	 * Pos to int
	 *
	 * @param pos pos
	 * @return int
	 */
	static int posLettersToNumber(final String pos)
	{
		switch (pos.toUpperCase())
		{
			case "NN":
				return 1;
			case "VB":
				return 2;
			case "JJ":
				return 3;
			case "RB":
				return 4;
			case "AS":
				return 5;
		}
		throw new IllegalArgumentException(pos);
	}

	public static String posNumberToLetters(String pos)
	{
		switch (pos)
		{
			case "1":
				return "NN";
			case "2":
				return "VB";
			case "3":
				return "JJ";
			case "4":
				return "RB";
			case "5":
				return "AS";
		}
		throw new IllegalArgumentException(pos);
	}

	static class Entry<K, V>
	{
		final K attribute;
		final V value;

		public Entry(final K attribute, final V value)
		{
			this.attribute = attribute;
			this.value = value;
		}
	}

	/**
	 * A HashMap where the keys are of the form word%POS:lex_filenum:lex_id (numeric POS)
	 * and values are 8 digit WordNet synset byte offsets. Note that all words are
	 * from index.sense, which reduces all words to lower case
	 */
	public HashMap<String, String> senseKeys = new HashMap<>();

	public HashMap<String, ArrayList<String>> wordsToSenseKeys = new HashMap<>();

	/**
	 * Keys are SUMO terms,
	 * Values are Collection of POS-prefixed 9-digit synset ids
	 * meaning that the part of speech code is prepended to the synset number.
	 */
	public Map<String, Collection<String>> SUMOTerms = new HashMap<>();

	/**
	 * Keys are noun synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> nounSUMOTerms = new HashMap<>();

	/**
	 * Keys are verb synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> verbSUMOTerms = new HashMap<>();

	/**
	 * Keys are adjective synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> adjectiveSUMOTerms = new HashMap<>();

	/**
	 * Keys are adverb synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public Map<String, String> adverbSUMOTerms = new HashMap<>();

	/**
	 * Keys are POS-prefixed synset ids.
	 * Values are Lists of words.
	 * Note that the order of words in the file is preserved.
	 */
	public Map<String, List<String>> synsetsToWords = new HashMap<>();

	/**
	 * Keys are words as keys
	 * Values are Lists of word senses which are Strings of the form
	 * word_POS_num (alpha POS like "VB") signifying the word, part of speech and number of
	 * the sense in WordNet.
	 * Note that all words are from index.sense, which reduces all words to lower case
	 */
	public Map<String, List<String>> wordToSenseKeys = new HashMap<>();

	/**
	 * Keys are of the form word%POS:lex_filenum:lex_id (numeric POS)
	 * Values are 8 digit WordNet synset byte offsets.
	 * Note that all words are from index.sense, which reduces all words to lower case
	 */
	public Map<String, String> senseKeyToSynset = new HashMap<>();

	/**
	 * Keys are of the form word_POS_sensenum (alpha POS like "VB")
	 * Values are 8 digit WordNet synset byte offsets.
	 * Note that all words are from index.sense, which reduces all words to lower case
	 */
	public HashMap<String, String> senseIndex = new HashMap<>();

	/**
	 * Keys are 9 digit POS prefixed WordNet synset byte offsets
	 * Values are of the form word_POS_sensenum (alpha POS like "VB").
	 * Note that all words are from index.sense, which reduces
	 * all words to lower case
	 */
	public Map<String, String> senseInvIndex = new HashMap<>();

	public Map<String, List<Entry<String, String>>> relations = new HashMap<>();

	/**
	 * Irregular plural forms.
	 * Key is the plural,
	 * Value is the singular.
	 */
	public Map<String, String> nounExceptions = new HashMap<>();

	/**
	 * Irregular plural forms.
	 * Key is the singular.
	 * Value is the plural,
	 */
	public Map<String, String> nounInvExceptions = new HashMap<>();

	/**
	 * Irregular past tenses
	 * Key is the past tense,
	 * Value is infinitive (without "to")
	 */
	public Map<String, String> verbExceptions = new HashMap<>();

	/**
	 * Irregular past tenses
	 * Key is infinitive (without "to")
	 * Value is the past tense,
	 */
	public Map<String, String> verbInvExceptions = new HashMap<>();

	public Map<String, String> nounDocumentation = new HashMap<>();

	public Map<String, String> verbDocumentation = new HashMap<>();

	public Map<String, String> adjectiveDocumentation = new HashMap<>();

	public Map<String, String> adverbDocumentation = new HashMap<>();

	/**
	 * Keys are 8 digit WordNet synset byte offsets or synsets appended
	 * with a dash and a specific word such as "12345678-foo".
	 * Values are Colelctions of verb frame numbers as strings.
	 */
	public Map<String, Collection<String>> verbFrames = new HashMap<>();

	/**
	 * Key is a 9-digit POS-prefixed sense
	 * Value is the number of times that sense occurs in the Brown corpus.
	 */
	public Map<String, Integer> senseFrequencies = new HashMap<>();

	/**
	 * Key is a word
	 * Value is a set of pairs with
	 * -the attribute is a 9-digit POS-prefixed sense which is the value of the pair,
	 * -the number of times that sense occurs in the Brown corpus, which is the key of the pair
	 */
	public Map<String, Set<Entry<String, String>>> wordFrequencies = new HashMap<>();

	/**
	 * Key is a word sense of the form word_POS_num signifying
	 * the word, part of speech and number of the sense in WordNet.
	 * Value is a Map of words and the number of times that word cooccurs in sentences
	 * with the word sense given in the key.
	 */
	public Map<String, Map<String, Integer>> wordCoFrequencies = new HashMap<>();

	/**
	 * Keys are uppercase words
	 * Values are their possibly mixed case original versions
	 */
	public Map<String, String> caseMap = new HashMap<>();

	/**
	 * List of English "stop words" such as "a", "at", "them", which have no or little
	 * inherent meaning when taken alone.
	 */
	public List<String> stopwords = new ArrayList<>();

	public void initOnce() throws IOException
	{
		load();
	}

	/**
	 * Read the WordNet files only on initialization of the class.
	 */
	private void load() throws IOException
	{
		makeFileMap();
		readNouns();
		readVerbs();
		readAdjectives();
		readAdverbs();
		createIgnoreCaseMap();
		readWordCoFrequencies();
		readStopWords();
		readSenseIndex(null);
		readSenseCount();
	}

	private void readNouns() throws IOException
	{
		// data
		Pattern pattern6 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern7 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$") -- no SUMO mapping
		File nounFile = getWnFile("noun_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(nounFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				Matcher m = pattern6.matcher(line);
				boolean anyAreNull = false;
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					for (int i = 1; i <= 4; i++)
					{
						anyAreNull = (m.group(i) == null);
						if (anyAreNull)
						{
							break;
						}
					}
					if (!anyAreNull)
					{
						// 1-synset, 2-pointers, 3-docu, 4-SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						String term = m.group(4).trim();
						String term2 = term.substring(2, term.length() - 1);
						SUMOTerms.computeIfAbsent(term2, k -> new ArrayList<>()).add(POS.NOUN.toSynset9(synset));
						nounSUMOTerms.put(synset, term);
						nounDocumentation.put(synset, docu);
						processPointers(synset, POS.NOUN, pointers);
					}
				}
				else
				{
					// match without SUMO mapping
					m = pattern7.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, no SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						nounDocumentation.put(synset, docu);
						processPointers(synset, POS.NOUN, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match in for line " + line);
						}
					}
				}
			}
		}

		// except
		// synset_offset  lex_filenum  ss_type  w_cnt  word  lex_id  [word  lex_id...]  p_cnt  [ptr...]  [frames...]  |   gloss
		Pattern pattern8 = Pattern.compile("(\\S+)\\s+(\\S+)"); // "(\\S+)\\s+(\\S+)"
		Pattern pattern9 = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)"); // "(\\S+)\\s+(\\S+)\\s+(\\S+)"
		File nounFileExcep = getWnFile("noun_exceptions");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(nounFileExcep))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				Matcher m = pattern8.matcher(line);
				if (m.matches())
				{
					String plural = m.group(1);
					String singular = m.group(2);
					nounExceptions.put(plural, singular);
					nounInvExceptions.put(singular, plural);
				}
				else
				{
					m = pattern9.matcher(line);
					if (m.matches())
					{
						String plural = m.group(1);
						String singular = m.group(2);
						String singular2 = m.group(3);
						nounExceptions.put(plural, singular);
						nounInvExceptions.put(singular, plural);
						nounInvExceptions.put(singular2, plural);
					}
					else if (!line.isEmpty() && line.charAt(0) != ';')
					{
						throw new IllegalArgumentException("No match in for line " + line);
					}
				}
			}
		}
	}

	private void readVerbs() throws IOException
	{
		Pattern pattern10 = Pattern.compile("^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern11 = Pattern.compile("^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+)$" -- no SUMO mapping
		File verbFile = getWnFile("verb_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				// match with SUMO mapping
				Matcher m = pattern10.matcher(line);
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					String synset = m.group(1);
					String pointers = m.group(2);
					String docu = m.group(3);
					String term = m.group(4).trim();
					String term2 = term.substring(2, term.length() - 1);
					SUMOTerms.computeIfAbsent(term2, k -> new ArrayList<>()).add(POS.VERB.toSynset9(synset));
					verbSUMOTerms.put(synset, term);
					verbDocumentation.put(synset, docu);
					processPointers(synset, POS.VERB, pointers);
				}
				else
				{
					// match without SUMO mapping
					m = pattern11.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, no SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						verbDocumentation.put(synset, docu);
						processPointers(synset, POS.VERB, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match for line " + line);
						}
					}
				}
			}
		}

		Pattern pattern12 = Pattern.compile("(\\S+)\\s+(\\S+).*"); // "(\\S+)\\s+(\\S+).*"
		File verbFileExcep = getWnFile("verb_exceptions");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFileExcep))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// 12: p = Pattern.compile();
				Matcher m = pattern12.matcher(line);  // TODO: Note we ignore more then one base form
				if (m.matches())
				{
					// 1-past, 2-infinitive
					String past = m.group(1);
					String infinitive = m.group(2);
					verbExceptions.put(past, infinitive);
					verbInvExceptions.put(infinitive, past);
				}
				else if (line.length() > 0 && line.charAt(0) != ';')
				{
					throw new IllegalArgumentException("No match for line " + line);
				}
			}
		}
	}

	private void readAdjectives() throws IOException
	{
		Pattern pattern13 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern14 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$" -- no SUMO mapping
		File verbFile = getWnFile("adj_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFile))))
		{

			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				// match with SUMO mapping
				Matcher m = pattern13.matcher(line);
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					String synset = m.group(1);
					String pointers = m.group(2);
					String docu = m.group(3);
					String term = m.group(4).trim();
					String term2 = term.substring(2, term.length() - 1);
					SUMOTerms.computeIfAbsent(term2, k -> new ArrayList<>()).add(POS.ADJ.toSynset9(synset));
					adjectiveSUMOTerms.put(synset, term);
					adjectiveDocumentation.put(synset, docu);
					processPointers(synset, POS.ADJ, pointers);
				}
				else
				{
					// match without SUMO mapping
					m = pattern14.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, no SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						adjectiveDocumentation.put(synset, docu);
						processPointers(synset, POS.ADJ, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match for line " + line);
						}
					}
				}
			}
		}
	}

	private void readAdverbs() throws IOException
	{
		Pattern pattern15 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?&%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern16 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"
		File adverbFile = getWnFile("adv_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(adverbFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				// match with SUMO mapping
				Matcher m = pattern15.matcher(line);
				if (m.matches())
				{
					// 1-synset, 2-pointers, 3-docu, 4-SUMO term
					String synset = m.group(1);
					String pointers = m.group(2);
					String docu = m.group(3);
					String term = m.group(4).trim();
					String term2 = term.substring(2, term.length() - 1);
					SUMOTerms.computeIfAbsent(term2, k -> new ArrayList<>()).add(POS.ADV.toSynset9(synset));
					adverbSUMOTerms.put(synset, term);
					adverbDocumentation.put(synset, docu);
					processPointers(synset, POS.ADV, pointers);
				}
				else
				{
					// match without SUMO mapping
					m = pattern16.matcher(line);
					if (m.matches())
					{
						// 1-synset, 2-pointers, 3-docu, 4-SUMO term
						String synset = m.group(1);
						String pointers = m.group(2);
						String docu = m.group(3);
						adverbDocumentation.put(synset, docu);
						processPointers(synset, POS.ADV, pointers);
					}
					else
					{
						if (line.length() > 0 && line.charAt(0) != ';')
						{
							throw new IllegalArgumentException("No match for line " + line);
						}
					}
				}
			}
		}
	}

	private void readSenseIndex(final String filePath) throws IOException
	{
		Pattern pattern18 = Pattern.compile("([^%]+)%([^:]*):([^:]*):([^:]*)?:([^:]*)?:([^ ]*)? ([^ ]+)? ([^ ]+).*"); // "([^%]+)%([^:]*):([^:]*):([^:]*):([^:]*):([^ ]*) ([^ ]+) ([^ ]+) .*"
		File senseIndexFile = getWnFile("sense_indexes");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(senseIndexFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();

				Matcher m = pattern18.matcher(line);
				if (m.matches())
				{
					String word = m.group(1);
					String pos = m.group(2);  // WN's ss_type
					String lexFilenum = m.group(3);
					String lexID = m.group(4);
					String headword = m.group(5);
					String headID = m.group(6);
					String synset = m.group(7);
					String sensenum = m.group(8);

					// sensekey
					String posString = posNumberToLetters(pos); // alpha POS - NN,VB etc
					String key = word + "_" + posString + "_" + sensenum;
					String sensekey = word + "%" + pos + ":" + lexFilenum + ":" + lexID;
					senseKeyToSynset.put(sensekey, synset);

					List<String> al = wordToSenseKeys.computeIfAbsent(word, k -> new ArrayList<String>());
					al.add(key);

					senseIndex.put(key, synset);
					senseInvIndex.put(pos + synset, key);
				}
			}
		}
	}

	private void readSenseCount() throws IOException
	{
		Pattern pattern26 = Pattern.compile("([^ ]+) ([^%]+)%([^:]*):[^:]*:[^:]*:[^:]*:[^ ]* ([^ ]+)");
		File senseIndexFile = getWnFile("cntlist");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(senseIndexFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				Matcher m = pattern26.matcher(line);
				if (m.matches())
				{
					String count = m.group(1);
					String word = m.group(2);
					String pos = m.group(3);
					String sensenum = m.group(4);

					caseMap.put(word.toUpperCase(), word);
					String pos2 = posNumberToLetters(pos);

					// word_POS_sensenum
					String key = word + "_" + pos2 + "_" + sensenum;

					// resolvable
					String synset8 = senseIndex.get(key);
					if (synset8 != null)
					{
						String synset = getSenseFromKey(key);

						Entry<String, String> entry = new Entry<>(key, synset);
						if (isValidSynset9(entry.attribute))
						{
							wordFrequencies.computeIfAbsent(word, k -> new HashSet<>()).add(entry);
						}

						int freq = Integer.parseInt(count);
						senseFrequencies.put(synset, freq);
					}
				}
			}
		}
	}

	private void readWordCoFrequencies() throws IOException
	{
		Pattern pattern17 = Pattern.compile("^Word: ([^ ]+) Values: (.*)"); // "^Word: ([^ ]+) Values: (.*)"
		File wordFrequenciesFile = getWnFile("word_frequencies");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(wordFrequenciesFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				Matcher m = pattern17.matcher(line);
				if (m.matches())
				{
					String key = m.group(1);
					String values = m.group(2);

					String[] words = values.split(" ");
					HashMap<String, Integer> frequencies = new HashMap<String, Integer>();
					for (int i = 0; i < words.length - 3; i++)
					{
						if (words[i].equals("SUMOterm:"))
						{
							i = words.length;
						}
						else
						{
							if (words[i].indexOf("_") == -1)
							{
								//System.out.println("INFO in WordNet.readWordFrequencies().  word: " + words[i]);
								//System.out.println("INFO in WordNet.readWordFrequencies().  line: " + line);
							}
							else
							{
								String word = words[i].substring(0, words[i].indexOf("_"));
								String freq = words[i].substring(words[i].lastIndexOf("_") + 1, words[i].length());
								frequencies.put(word.intern(), Integer.decode(freq));
							}
						}
					}
					wordCoFrequencies.put(key.intern(), frequencies);
				}
			}
		}
	}

	private void readStopWords() throws IOException
	{
		File stopWordsFile = getWnFile("stopwords");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(stopWordsFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 1000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				stopwords.add(line);
			}
		}
	}

	private void createIgnoreCaseMap()
	{
	}

	private void processPointers(final String synset8, final POS pos, final String pointers0)
	{
		Pattern pattern0 = Pattern.compile("^\\s*\\d\\d\\s\\S\\s\\d\\S\\s"); // "^\\s*\\d\\d\\s\\S\\s\\d\\S\\s",
		Pattern pattern1 = Pattern.compile("^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s"); // "^([a-zA-Z0-9'._\\-]\\S*)\\s([0-9a-f])\\s"
		Pattern pattern2 = Pattern.compile("^...\\s"); // "^...\\s"
		Pattern pattern3 = Pattern.compile("^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?"); // "^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?"
		Pattern pattern4 = Pattern.compile("^..\\s"); // "^..\\s"
		Pattern pattern5 = Pattern.compile("^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?"); // "^\\+\\s(\\d\\d)\\s(\\d\\d)\\s?"

		String synset9 = pos.toSynset9(synset8);

		// process and remove prefix, should be left with: word lex_id [word  lex_id...]  p_cnt  [ptr...]  [frames...]
		String pointers = pointers0;
		Matcher m = pattern0.matcher(pointers);
		pointers = m.replaceFirst("");

		// word lex_id
		// process and remove words, should be left with: p_cnt  [ptr...]  [frames...]
		m = pattern1.matcher(pointers);
		while (m.lookingAt())
		{
			// word
			String word = m.group(1);
			// String count = m.group(2);

			// remove adj position
			if (word.length() > 3 && (word.substring(word.length() - 3, word.length()).equals("(a)") || word.substring(word.length() - 3, word.length()).equals("(p)")))
			{
				word = word.substring(0, word.length() - 3);
			}
			if (word.length() > 4 && word.substring(word.length() - 4, word.length()).equals("(ip)"))
			{
				word = word.substring(0, word.length() - 4);
			}

			// store
			synsetsToWords.computeIfAbsent(synset9, k -> new ArrayList<>()).add(word);

			// eat this word + lex_di
			pointers = m.replaceFirst("");

			// next
			m = pattern1.matcher(pointers);
		}

		// process and remove p_cnt, should be left with: [ptr...]  [frames...]
		m = pattern2.matcher(pointers);
		pointers = m.replaceFirst("");

		// process [ptr...]  [frames...]
		// where ptr is pointer_symbol synset_offset pos source/target
		m = pattern3.matcher(pointers);
		while (m.lookingAt())
		{
			// "^(\\S\\S?)\\s([0-9]{8})\\s(.)\\s([0-9a-f]{4})\\s?"
			//     symbol     offset       pos    src/target
			String ptr = m.group(1);
			String targetSynset = m.group(2);
			String targetPOS = m.group(3);
			// String sourceTarget = m.group(4);

			POS targetPos = POS.parse(targetPOS.charAt(0));

			// eat this ptr
			pointers = m.replaceFirst("");

			// next
			m = pattern3.matcher(pointers);

			// store
			ptr = convertWordNetPointer(ptr);
			Entry<String, String> av = new Entry<>(ptr, targetPos.toSynset9(targetSynset));
			List<Entry<String, String>> synsetRelations = relations.computeIfAbsent(synset9, k -> new ArrayList<>());
			synsetRelations.add(av);
		}

		// verb frames
		if (pointers.length() > 0 && !pointers.equals(" "))
		{
			// Only for verbs may we have the following leftover
			// f_cnt + f_num w_num [ +  f_num  w_num...]
			if (pos == POS.VERB) // verb prefix
			{
				// eat f_cnt
				m = pattern4.matcher(pointers);
				pointers = m.replaceFirst("");

				// process f_num w_num
				m = pattern5.matcher(pointers);
				while (m.lookingAt())
				{
					String frameNum = m.group(1);
					String wordNum = m.group(2);
					String key;
					if (wordNum.equals("00"))
					{
						key = synset8;
					}
					else
					{
						int num = Integer.parseInt(wordNum);
						List<String> members = synsetsToWords.get(synset9);
						if (members == null)
						{
							throw new IllegalArgumentException(synset9 + " has no words for pointers: \"" + pointers + "\"");
						}
						String word = members.get(num - 1);
						key = synset8 + "-" + word;
					}

					// store
					Collection<String> frames = verbFrames.computeIfAbsent(key, k -> new ArrayList<>());
					frames.add(frameNum);

					// eat
					pointers = m.replaceFirst("");

					// next
					m = pattern5.matcher(pointers);
				}
			}
			else
			{
				throw new IllegalArgumentException("Leftover pointers: \"" + pointers + "\"");
			}
		}
	}

	private static String convertWordNetPointer(final String ptr)
	{
		switch (ptr)
		{
			case "!":
				return "antonym";
			case "@":
				return "hypernym";
			case "@i":
				return "instance hypernym";
			case "~":
				return "hyponym";
			case "~i":
				return "instance hyponym";
			case "#m":
				return "member holonym";
			case "#s":
				return "substance holonym";
			case "#p":
				return "part holonym";
			case "%m":
				return "member meronym";
			case "%s":
				return "substance meronym";
			case "%p":
				return "part meronym";
			case "=":
				return "attribute";
			case "+":
				return "derivationally related";
			case ";c":
				return "domain topic";
			case "-c":
				return "member topic";
			case ";r":
				return "domain region";
			case "-r":
				return "member region";
			case ";u":
				return "domain usage";
			case "-u":
				return "member usage";
			case "*":
				return "entailment";
			case ">":
				return "cause";
			case "^":
				return "also see";
			case "$":
				return "verb group";
			case "&":
				return "similar to";
			case "<":
				return "participle";
			case "\\":
				return "pertainym";
		}
		throw new IllegalArgumentException(ptr);
	}

	/**
	 * Extract the POS from a word_POS_num sense key.
	 * Should be an alpha key, such as "VB".
	 */
	@NotNull
	static String getPOSfromKey(final String key)
	{
		int lastUS = key.lastIndexOf("_");
		if (lastUS < 0)
		{
			throw new IllegalArgumentException("Missing POS: " + key);
		}
		return key.substring(lastUS - 2, lastUS);
	}

	/**
	 * Extract the word from a word_POS_num sense key.
	 */
	@NotNull
	static String getWordFromKey(final String senseKey)
	{
		int lastUS = senseKey.lastIndexOf("_");
		if (lastUS < 0)
		{
			throw new IllegalArgumentException("Missing word: " + senseKey);
		}
		return senseKey.substring(0, lastUS - 3);
	}

	/**
	 * Extract the synset corresponding to a word_POS_num sense key.
	 */
	private String getSenseFromKey(final String senseKey)
	{
		String pos = getPOSfromKey(senseKey);
		int n = posLettersToNumber(pos);
		return n + senseIndex.get(senseKey);
	}

	/**
	 * Check whether a synset format is valid
	 */
	private static boolean isValidSynset8(final String synset)
	{
		try
		{
			Integer.parseInt(synset);
			return synset.length() == 8;
		}
		catch (NumberFormatException nfr)
		{
			return false;
		}
	}

	/**
	 * Check whether a synset format is valid
	 */
	private static boolean isValidSynset9(final String synset)
	{
		try
		{
			Integer.parseInt(synset);
			return synset.length() == 9;
		}
		catch (NumberFormatException nfr)
		{
			return false;
		}
	}

	/**
	 * Check whether a sense key format is valid
	 */
	private static boolean isValidKey(final String senseKey)
	{
		return senseKey.matches(".*_(NN|VB|JJ|RB|AS)_[\\d]+");
	}

	// S T A R T

	Map<String, String> wnFilenames = makeFileMap();

	private static Map<String, String> makeFileMap()
	{
		Map<String, String> map = new HashMap<>();
		map.put("noun_mappings", "data/WordNetMappings/WordNetMappings30-noun.txt");
		map.put("verb_mappings", "data/WordNetMappings/WordNetMappings30-verb.txt");
		map.put("adj_mappings", "data/WordNetMappings/WordNetMappings30-adj.txt");
		map.put("adv_mappings", "data/WordNetMappings/WordNetMappings30-adv.txt");
		map.put("word_frequencies", "data/WordNetMappings/wordFrequencies.txt");
		map.put("stopwords", "data/WordNetMappings/stopwords.txt");

		map.put("noun_exceptions", "wordnet/noun.exc");
		map.put("verb_exceptions", "wordnet/verb.exc");
		map.put("adj_exceptions", "wordnet/adj.exc");
		map.put("adv_exceptions", "wordnet/adv.exc");
		map.put("sense_indexes", "wordnet/index.sense");
		map.put("cntlist", "wordnet/cntlist");
		return map;
	}

	private File getWnFile(final String fileKey)
	{
		return new File(wnFilenames.get(fileKey));
	}

	/**
	 * Main
	 */
	public static void main(@Nullable String[] args) throws IOException
	{
		WordNet wn = new WordNet();
		wn.initOnce();
	}
}
