/*
 * Copyright 2013 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mitre.eyesfirst.processor.web;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Used to configure Log4J. Note that most of the classes actually use SLF4J,
 * so if Log4J is removed, this listener should also be disabled.
 */
@WebListener("Listener used to configure Log4J.")
public class Log4jListener implements ServletContextListener {

	/**
	 * Default constructor. 
	 */
	public Log4jListener() {
	}

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent event) {
		InitialContext context;
		try {
			context = new InitialContext();
			Object o = context.lookup("java:comp/env/eyesfirst/log4j.configuration");
			if (o instanceof String) {
				String name = (String) o;
				if (name.length() > 0) {
					PropertyConfigurator.configure(name);
				}
			}
		} catch (NamingException e) {
			// This is, effectively, forcing Log4J to use the default configuration
			Logger.getLogger(getClass()).error("Naming exception while attempting to configure Log4J - falling back to default configuration!", e);
		}
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent event) {
		// Don't care
	}
}
