package com.articulate.sigma;

import java.util.Collection;
import java.util.Set;

public interface KBIface
{
	Set<String> getTerms();
	Set<String> getForms();
	Collection<Formula> getFormulas();
}
