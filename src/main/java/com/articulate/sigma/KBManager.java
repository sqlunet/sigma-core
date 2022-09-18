package com.articulate.sigma;

/* This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This is a class that manages a group of knowledge bases.  It should only
 * have one instance, contained in its own static member variable.
 */
public class KBManager
{
	@NotNull
	private static final KBManager manager = new KBManager();

	//private static Logger logger;
	private static final Logger logger = Logger.getLogger(KBManager.class.getName());

	@NotNull
	@SuppressWarnings("CanBeFinal") public Map<String, String> preferences = new HashMap<>();

	public final Map<String, KB> kbs = new HashMap<>();

	private String error = "";

	public KBManager()
	{}

	/**
	 * Set an error string for file loading.
	 *
	 * @param er error string
	 */
	public void setError(String er)
	{
		error = er;
	}

	/**
	 * Get the error string for file loading.
	 *
	 * @return error string
	 */
	public String getError()
	{
		return error;
	}

	/**
	 * Add KB
	 *
	 * @param name      name
	 * @param isVisible whether it is visible
	 */
	public void addKB(@NotNull String name, boolean isVisible)
	{
		@NotNull KB kb = new KB(name, preferences.get("kbDir"), isVisible);
		kbs.put(name.intern(), kb);
		logger.info("Adding KB: " + name);
	}

	/**
	 * Get the KB that has the given name.
	 *
	 * @param name name
	 * @return knowledge base
	 */
	public KB getKB(@NotNull String name)
	{
		if (!kbs.containsKey(name))
			logger.warning("KB " + name + " not found.");
		return kbs.get(name.intern());
	}

	/**
	 * Get the one instance of KBManager from its class variable.
	 *
	 * @return knowledge base manager
	 */
	@NotNull
	public static KBManager getMgr()
	{
		return manager;
	}

	/**
	 * Get the preference corresponding to the given kef.
	 *
	 * @param key key
	 * @return value
	 */
	@NotNull
	public String getPref(String key)
	{
		String result = preferences.get(key);
		if (result == null)
		{
			result = "";
		}
		return result;
	}
}
