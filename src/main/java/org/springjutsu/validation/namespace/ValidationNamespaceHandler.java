/*
 * Copyright 2010-2011 Duplichien, Wicksell, Springjutsu.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springjutsu.validation.namespace;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Handles the Validation namespace!
 * @see NamespaceHandlerSupport
 * @see ValidationEntityDefinitionParser
 * @author Clark Duplichien
 * @author Taylor Wicksell
 */
public class ValidationNamespaceHandler extends NamespaceHandlerSupport{

	public void init() {
		registerBeanDefinitionParser("entity", new ValidationEntityDefinitionParser());
		registerBeanDefinitionParser("configuration", new ValidationConfigurationDefinitionParser());
		registerBeanDefinitionParser("mvc-annotations", new ValidationMVCAnnotationsDefinitionParser());
	}
}