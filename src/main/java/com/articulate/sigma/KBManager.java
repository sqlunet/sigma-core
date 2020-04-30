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
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a class that manages a group of knowledge bases.  It should only
 * have one instance, contained in its own static member variable.
 */
public class KBManager
{
	private static KBManager manager = new KBManager();

	private static Logger logger;

	@SuppressWarnings("CanBeFinal") public Map<String, String> preferences = new HashMap<>();

	public final Map<String, KB> kbs = new HashMap<>();

	private String error = "";

	public KBManager()
	{
		if (logger == null)
		{
			logger = Logger.getAnonymousLogger();
			logger.addHandler(new ConsoleHandler());
			logger.setLevel(Level.FINEST);
		}
	}

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

    /* Future:
    private SigmaServer sigmaServer = null;

    public void setSigmaServer(SigmaServer ss) {
        this.sigmaServer = ss;
        return;
    }

    public SigmaServer getSigmaServer() {
        return this.sigmaServer;
    }
    */

    /*
      Reads an XML configuration file from the most likely locations,
      trying the value of the System property "user.dir" as a last
      resort.  The method initializeOnce() sets the preferences based
      on the contents of the configuration file.  This routine has
      the side effect of setting the variable called "configuration".
      It also creates the KBs directory and an empty configuration
      file if none exists.
     */
	//private SimpleElement readConfiguration() {
	//    return readConfiguration(null);
	//}

	/**
	 * Add KB
	 *
	 * @param name      name
	 * @param isVisible whether it is visible
	 */
	public void addKB(String name, boolean isVisible)
	{
		KB kb = new KB(name, preferences.get("kbDir"), isVisible);
		kbs.put(name.intern(), kb);
		logger.info("Adding KB: " + name);
	}

	/**
	 * Get the KB that has the given name.
	 *
	 * @param name name
	 * @return knowledge base
	 */
	public KB getKB(String name)
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
	public static KBManager getMgr()
	{
		if (manager == null)
			manager = new KBManager();
		return manager;
	}

	/**
	 * Get the preference corresponding to the given kef.
	 *
	 * @param key key
	 * @return value
	 */
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
