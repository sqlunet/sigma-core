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

		public String toCode()
		{
			switch (this)
			{
				case NOUN:
					return "NN";
				case VERB:
					return "VB";
				case ADJ:
					return "JJ";
				case ADV:
					return "RB";
				case ADJSAT:
					return "AS";
			}
			throw new IllegalArgumentException();
		}

		static POS parseCode(final String code)
		{
			switch (code.toUpperCase())
			{
				case "NN":
					return NOUN;
				case "VB":
					return VERB;
				case "JJ":
					return ADJ;
				case "RB":
					return ADV;
				case "AS":
					return ADJSAT;
			}
			throw new IllegalArgumentException(code);
		}

		static POS parseNum(final int num)
		{
			switch (num)
			{
				case 1:
					return NOUN;
				case 2:
					return VERB;
				case 3:
					return ADJ;
				case 4:
					return ADV;
				case 5:
					return ADJSAT;
			}
			throw new IllegalArgumentException(Integer.toString(num));
		}
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

	// W O R D S

	/**
	 * Keys are POS-prefixed synset ids.
	 * Values are Lists of words.
	 * Note that the order of words in the file is preserved.
	 */
	public final Map<String, List<String>> wordsBySynset9 = new HashMap<>();

	// S Y N S E T S

	/**
	 * Keys are of the form word_POS_sensenum (alpha POS like "VB")
	 * Values are 8 digit WordNet synset byte offsets.
	 * Note that all words are from 'index.sense', which reduces all words to lower case
	 */
	public final HashMap<String, String> synsets8BySense = new HashMap<>();

	/**
	 * Keys are of the form word%POS:lex_filenum:lex_id (numeric POS)
	 * Values are 8 digit WordNet synset byte offsets.
	 * Note that all words are from 'index.sense', which reduces all words to lower case
	 */
	public final Map<String, String> synsets8BySensekey = new HashMap<>();

	// S E N S E S

	/**
	 * Keys are words as keys
	 * Values are Lists of word senses which are Strings of the form
	 * word_POS_num (alpha POS like "VB") signifying the word, part of speech and number of
	 * the sense in WordNet.
	 * Note that all words are from 'index.sense', which reduces all words to lower case
	 */
	public final Map<String, List<String>> sensesByWord = new HashMap<>();

	/**
	 * Keys are 9 digit POS prefixed WordNet synset byte offsets
	 * Values are of the form word_POS_sensenum (alpha POS like "VB").
	 * Note that all words are from 'index.sense', which reduces
	 * all words to lower case
	 */
	public final Map<String, String> sensesBySynset9 = new HashMap<>();

	// C A S E

	/**
	 * Keys are uppercase words
	 * Values are their possibly mixed case original versions
	 */
	public final Map<String, String> caseMap = new HashMap<>();

	public final Map<String, List<Entry<String, String>>> relations = new HashMap<>();

	/**
	 * Irregular plural forms.
	 * Key is the plural,
	 * Value is the singular.
	 */
	public final Map<String, String> nounExceptions = new HashMap<>();

	/**
	 * Irregular plural forms.
	 * Key is the singular.
	 * Value is the plural,
	 */
	public final Map<String, String> nounInvExceptions = new HashMap<>();

	/**
	 * Irregular past tenses
	 * Key is the past tense,
	 * Value is infinitive (without "to")
	 */
	public final Map<String, String> verbExceptions = new HashMap<>();

	/**
	 * Irregular past tenses
	 * Key is infinitive (without "to")
	 * Value is the past tense,
	 */
	public final Map<String, String> verbInvExceptions = new HashMap<>();

	public final Map<String, String> nounDocumentation = new HashMap<>();

	public final Map<String, String> verbDocumentation = new HashMap<>();

	public final Map<String, String> adjectiveDocumentation = new HashMap<>();

	public final Map<String, String> adverbDocumentation = new HashMap<>();

	/**
	 * Keys are 8 digit WordNet synset byte offsets or synsets appended
	 * with a dash and a specific word such as "12345678-foo".
	 * Values are Colelctions of verb frame numbers as strings.
	 */
	public final Map<String, Collection<String>> verbFrames = new HashMap<>();

	/**
	 * Key is a word
	 * Value is a set of pairs with
	 * -the attribute is a 9-digit POS-prefixed sense which is the value of the pair,
	 * -the number of times that sense occurs in the Brown corpus, which is the key of the pair
	 */
	public final Map<String, Set<Entry<String, String>>> wordFrequencies = new HashMap<>();

	/**
	 * Key is a word sense of the form word_POS_num signifying
	 * the word, part of speech and number of the sense in WordNet.
	 * Value is a Map of words and the number of times that word cooccurs in sentences
	 * with the word sense given in the key.
	 */
	public final Map<String, Map<String, Integer>> wordCoFrequencies = new HashMap<>();

	/**
	 * Key is a 9-digit POS-prefixed sense
	 * Value is the number of times that sense occurs in the Brown corpus.
	 */
	public final Map<String, Integer> senseFrequencies = new HashMap<>();

	/**
	 * List of English "stop words" such as "a", "at", "them", which have no or little
	 * inherent meaning when taken alone.
	 */
	public final List<String> stopwords = new ArrayList<>();

	/**
	 * Keys are SUMO terms,
	 * Values are Collection of POS-prefixed 9-digit synset ids
	 * meaning that the part of speech code is prepended to the synset number.
	 */
	public final Map<String, Collection<String>> SUMOTerms = new HashMap<>();

	/**
	 * Keys are noun synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public final Map<String, String> nounSUMOTerms = new HashMap<>();

	/**
	 * Keys are verb synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public final Map<String, String> verbSUMOTerms = new HashMap<>();

	/**
	 * Keys are adjective synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public final Map<String, String> adjectiveSUMOTerms = new HashMap<>();

	/**
	 * Keys are adverb synset ids,
	 * Values are SUMO terms with the &% prefix and =, +, @ or [ suffix.
	 */
	public final Map<String, String> adverbSUMOTerms = new HashMap<>();

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
		readSenseIndex();
		readSenseCount();
		readWordCoFrequencies();
		readStopWords();
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
				if (lr.getLineNumber() % 10000 == 1)
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
		Pattern pattern10 = Pattern.compile("^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern11 = Pattern.compile("^([0-9]{8})([^|]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([^\\|]+)\\|\\s([\\S\\s]+)$" -- no SUMO mapping
		File verbFile = getWnFile("verb_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 10000 == 1)
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
		Pattern pattern13 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?&%\\S+[\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+?)\\s(\\(?\\&\\%\\S+[\\S\\s]+)$"
		Pattern pattern14 = Pattern.compile("^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$"); // "^([0-9]{8})([\\S\\s]+)\\|\\s([\\S\\s]+)$" -- no SUMO mapping
		File verbFile = getWnFile("adj_mappings");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(verbFile))))
		{

			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 10000 == 1)
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
				if (lr.getLineNumber() % 10000 == 1)
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

	private void readSenseIndex() throws IOException
	{
		Pattern pattern18 = Pattern.compile("([^%]+)%([^:]*):([^:]*):([^:]*)?:([^:]*)?:([^ ]*)? ([^ ]+)? ([^ ]+).*"); // "([^%]+)%([^:]*):([^:]*):([^:]*):([^:]*):([^ ]*) ([^ ]+) ([^ ]+) .*"
		File senseIndexFile = getWnFile("sense_indexes");
		try (LineNumberReader lr = new LineNumberReader(new BufferedReader(new FileReader(senseIndexFile))))
		{
			String line;
			while ((line = lr.readLine()) != null)
			{
				// progress
				if (lr.getLineNumber() % 10000 == 1)
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
					//String headword = m.group(5);
					//String headID = m.group(6);
					String synset8 = m.group(7);
					String sensenum = m.group(8);

					// sense = alpha-POS-(NN|VB|...)
					POS pos2 = POS.parseNum(Integer.parseInt(pos));
					String sense = word + "_" + pos2.toCode() + "_" + sensenum;

					sensesByWord.computeIfAbsent(word, k -> new ArrayList<>()).add(sense);
					synsets8BySense.put(sense, synset8);
					sensesBySynset9.put(pos2.toSynset9(synset8), sense);

					String sensekey = word + "%" + pos + ":" + lexFilenum + ":" + lexID;
					synsets8BySensekey.put(sensekey, synset8);
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
				if (lr.getLineNumber() % 10000 == 1)
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
					String posCode = POS.parseNum(Integer.parseInt(pos)).toCode();

					// sense = word_POS_sensenum
					String sense = word + "_" + posCode + "_" + sensenum;

					// resolvable
					String synset8 = synsets8BySense.get(sense);
					if (synset8 != null)
					{
						String synset = getSynsetFromSense(sense);

						Entry<String, String> entry = new Entry<>(sense, synset);
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
				if (lr.getLineNumber() % 10000 == 1)
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
					HashMap<String, Integer> frequencies = new HashMap<>();
					for (int i = 0; i < words.length - 3; i++)
					{
						if (words[i].equals("SUMOterm:"))
						{
							i = words.length;
						}
						else
						{
							if (words[i].contains("_"))
							{
								String word = words[i].substring(0, words[i].indexOf("_"));
								String freq = words[i].substring(words[i].lastIndexOf("_") + 1);
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
				if (lr.getLineNumber() % 10000 == 1)
				{
					System.out.print('.');
				}

				// process
				line = line.trim();
				stopwords.add(line);
			}
		}
	}

	// P O I N T E R S

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
			if (word.length() > 3 && (word.endsWith("(a)") || word.endsWith("(p)")))
			{
				word = word.substring(0, word.length() - 3);
			}
			if (word.length() > 4 && word.endsWith("(ip)"))
			{
				word = word.substring(0, word.length() - 4);
			}

			// store
			wordsBySynset9.computeIfAbsent(synset9, k -> new ArrayList<>()).add(word);

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

					// key
					String key;
					if (wordNum.equals("00"))
					{
						key = synset8;
					}
					else
					{
						int num = Integer.parseInt(wordNum);
						List<String> members = wordsBySynset9.get(synset9);
						if (members == null)
						{
							throw new IllegalArgumentException(synset9 + " has no words for pointers: \"" + pointers + "\"");
						}
						String word = members.get(num - 1);
						key = synset8 + '-' + word;
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

	// S E N S E

	/**
	 * Extract the POS from a word_POS_num sense key.
	 * Should be an alpha key, such as "VB".
	 */
	@NotNull
	static String getPOSFromSense(final String key)
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
	static String getWordFromSense(final String sense)
	{
		int lastUS = sense.lastIndexOf("_");
		if (lastUS < 0)
		{
			throw new IllegalArgumentException("Missing word: " + sense);
		}
		return sense.substring(0, lastUS - 3);
	}

	/**
	 * Check whether a sense key format is valid
	 */
	public static boolean isValidSense(final String sense)
	{
		return sense.matches(".*_(NN|VB|JJ|RB|AS)_\\d+");
	}

	// S Y N S E T

	/**
	 * Extract the synset corresponding to a word_POS_num sense key.
	 */
	private String getSynsetFromSense(final String sense)
	{
		return POS.parseCode(getPOSFromSense(sense)).toSynset9(synsets8BySense.get(sense));
	}

	/**
	 * Check whether a synset format is valid
	 */
	public static boolean isValidSynset8(final String synset)
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
	public static boolean isValidSynset9(final String synset)
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

	// S T A R T

	private static final String SUMO_MAPPINGS_DIR = "data/WordNetMappings";

	private static final String WORDNET_DIR = "wordnet";

	private static final Map<String, String> FILES = makeFileMap();

	private static Map<String, String> makeFileMap()
	{
		Map<String, String> map = new HashMap<>();

		map.put("noun_mappings", SUMO_MAPPINGS_DIR + "/WordNetMappings30-noun.txt");
		map.put("verb_mappings", SUMO_MAPPINGS_DIR + "/WordNetMappings30-verb.txt");
		map.put("adj_mappings", SUMO_MAPPINGS_DIR + "/WordNetMappings30-adj.txt");
		map.put("adv_mappings", SUMO_MAPPINGS_DIR + "/WordNetMappings30-adv.txt");
		map.put("word_frequencies", SUMO_MAPPINGS_DIR + "/wordFrequencies.txt");
		map.put("stopwords", SUMO_MAPPINGS_DIR + "/stopwords.txt");

		map.put("noun_exceptions", WORDNET_DIR + "/noun.exc");
		map.put("verb_exceptions", WORDNET_DIR + "/verb.exc");
		map.put("adj_exceptions", WORDNET_DIR + "/adj.exc");
		map.put("adv_exceptions", WORDNET_DIR + "/adv.exc");
		map.put("sense_indexes", WORDNET_DIR + "/index.sense");
		map.put("cntlist", WORDNET_DIR + "/cntlist");
		return map;
	}

	private File getWnFile(final String fileKey)
	{
		return new File(FILES.get(fileKey));
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
