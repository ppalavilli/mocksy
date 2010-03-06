package org.mocksy.rules;

/*
 * Copyright 2009, PayPal
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.util.LinkedList;
import java.util.List;
import org.mocksy.Request;
import org.mocksy.Response;
import org.mocksy.config.Configurator;

/**
 * Represents a group of {@link org.mocksy.rules.Rule}s.  The only reason
 * that this is separated from the {@link org.mocksy.rules.RulesetRule} class
 * is to be better suited for the top-level set of Rules.  However, the 
 * separate also makes it a little easier to separate Rulesets physically,
 * allowing the Rule that delegates to the Ruleset to be managed in one area,
 * and the list of rules in another (see the documentation about XML configuration
 * files).
 * 
 * A Ruleset can be associated with a {@link org.mocksy.config.Configurator},
 * which allows it to be easily updated during runtime, e.g. responding to
 * updates in file-based configuration without need to recycling the server.
 *  
 * @author Saleem Shafi
 */
public class Ruleset {
	private List<Rule> rules = new LinkedList<Rule>();
	private Configurator config;
	private Response defaultResponse;

	/**
	 * Creates a Ruleset without an associated {@link org.mocksy.config.Configurator}.
	 */
	public Ruleset() {
		this.clear();
	}

	/**
	 * Creates a Ruleset with an associated {@link org.mocksy.config.Configurator},
	 * which will help to update the contents of the Ruleset, if needed.
	 * 
	 * @param config the Configurator instance used to build this Ruleset
	 */
	public Ruleset(Configurator config) {
		this();
		this.config = config;
	}

	/**
	 * Clears the contents of this Ruleset in preparation for it to be re-populated
	 * either by the {@link org.mocksy.config.Configurator} or programmatically
	 * for the first time.
	 */
	public void clear() {
		this.rules.clear();
		this.defaultResponse = null;
	}

	/**
	 * Adds a {@link org.mocksy.rules.Rule} to this Ruleset.  Rules are processed
	 * in order and this method will add the given Rule to the end of the current list.
	 * 
	 * @param rule the Rule to add to the end of this Ruleset
	 */
	public void addRule(Rule rule) {
		this.rules.add( rule );
	}

	/**
	 * Sets the {@link org.mocksy.Response} object to return if none
	 * of the Rules are suitable for a given Request.
	 * 
	 * @param defaultResponse the default Response for this set of Rules
	 */
	public void setDefaultResponse(Response defaultResponse) {
		this.defaultResponse = defaultResponse;
	}

	/**
	 * Processes the given Request by checking to see if any of the 
	 * Rules can be used to generate a Response.  If not, the default
	 * response is returned.
	 * 
	 * Before processing the Request, if a {@link org.mocksy.config.Configurator}
	 * was provided, we'll first check for updates to the configuration
	 * and update the contents of the Ruleset before continuing with
	 * the processing.
	 * 
	 * Note: The logic that updates the Ruleset based on the Configurator
	 * is synchronized, so it should be thread-safe.
	 * 
	 * @param request the Request to process
	 * @return the Response from the appropriate Rule in the Ruleset; or the
	 * 		default response if none of the Rules match
	 * @throws Exception
	 */
	public Response process(Request request) throws Exception {
		// make sure we've got the most up-to-date configuration
		if ( this.config != null ) {
			synchronized ( this.config ) {
				this.config.checkForUpdates();
			}
		}
		Response response = null;
		// check for matches in the list of Rules
		for ( Rule rule : this.rules ) {
			if ( rule.matches( request ) ) {
				response = rule.process( request );
				break; // first match wins
			}
		}
		// if none of 'em match, send back a default response
		if ( response == null ) {
			response = this.defaultResponse;
		}
		return response;
	}

	/**
	 * Returns the default {@link org.mocksy.Response} instance for this
	 * Ruleset.
	 * 
	 * @return the default Response
	 */
	public Response getDefaultResponse() {
		return this.defaultResponse;
	}

	/**
	 * Returns the list of Rules comprising this Ruleset.
	 * 
	 * @return list of Rules in the Ruleset
	 */
	public List<Rule> getRules() {
		return this.rules;
	}
}