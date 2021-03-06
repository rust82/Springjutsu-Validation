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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springjutsu.validation.rules.ValidationEntity;
import org.springjutsu.validation.rules.ValidationRule;
import org.springjutsu.validation.rules.ValidationTemplate;
import org.springjutsu.validation.rules.ValidationTemplateReference;
import org.springjutsu.validation.util.RequestUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
/**
 * Parses XML from the validation namespace into a @link{ValidationEntity}
 * object to be stored in the @link{ValidationEntityContainer}
 * @author Clark Duplichien
 * @author Taylor Wicksell
 *
 */
public class ValidationEntityDefinitionParser implements BeanDefinitionParser {

	/**
	 * Do actual parsing.
	 * Since rules may be nested, delegate to {@link #parseNestedRules(Element, Class)}
	 * where necessary.
	 */
	public BeanDefinition parse(Element entityNode, ParserContext parserContext) {
		
		RootBeanDefinition entityDefinition = new RootBeanDefinition(ValidationEntity.class);
		
		String className = entityNode.getAttribute("class");
		Class<?> modelClass;
		try {
			modelClass = Class.forName(className);
		} catch (ClassNotFoundException cnfe) {
			throw new ValidationParseException("Class " + className + " does not exist as a model class.");
		}
		
		Element modelValidationRuleNode = (Element) 
			entityNode.getElementsByTagNameNS(entityNode.getNamespaceURI(), "model-validation").item(0);
		ValidationStructure modelValidation = parseNestedValidation(modelValidationRuleNode, modelClass);
		
		Map<String, List<ValidationRule>> contextValidationRules = 
			new HashMap<String, List<ValidationRule>>();
		Map<String, List<ValidationTemplateReference>> contextValidationTemplateReferences = 
			new HashMap<String, List<ValidationTemplateReference>>();
		
		Element contextValidationRuleNode = (Element) 
			entityNode.getElementsByTagNameNS(entityNode.getNamespaceURI(), "context-validation").item(0);
		if (contextValidationRuleNode != null) {
			NodeList forms = contextValidationRuleNode.getElementsByTagNameNS(
					contextValidationRuleNode.getNamespaceURI(), "form");
			for (int formNbr = 0; formNbr < forms.getLength(); formNbr++) {
				Element formNode = (Element) forms.item(formNbr);
				String formPaths = formNode.getAttribute("path");
				ValidationStructure contextValidation = parseNestedValidation(formNode, modelClass);
				for (String formPath : formPaths.split(",")) {
					formPath = formPath.trim();
					formPath = RequestUtils.removeLeadingAndTrailingSlashes(formPath).trim();
					if (contextValidationRules.get(formPath) == null) {
						contextValidationRules.put(formPath, new ArrayList<ValidationRule>());
					} 
					if (contextValidationTemplateReferences.get(formPath) == null) {
						contextValidationTemplateReferences.put(formPath, 
							new ArrayList<ValidationTemplateReference>());
					}
					contextValidationRules.get(formPath).addAll(contextValidation.rules);
					contextValidationTemplateReferences.get(formPath).addAll(contextValidation.refs);
				}
			}
		}
		
		List<ValidationTemplate> templates = new ArrayList<ValidationTemplate>();
		Element templatesNode = (Element) 
			entityNode.getElementsByTagNameNS(entityNode.getNamespaceURI(), "templates").item(0);
		if (templatesNode != null) {
			NodeList templateNodes = templatesNode.getElementsByTagNameNS(
					templatesNode.getNamespaceURI(), "template");
			for (int templateNbr = 0; templateNbr < templateNodes.getLength(); templateNbr++) {
				Element templateNode = (Element) templateNodes.item(templateNbr);
				String templateName = templateNode.getAttribute("name");
				ValidationStructure templateValidation = parseNestedValidation(templateNode, modelClass);
				ValidationTemplate template = new ValidationTemplate(templateName, modelClass);
				template.setRules(templateValidation.rules);
				template.setTemplateReferences(templateValidation.refs);
				templates.add(template);
			}			
		}
		
		entityDefinition.getPropertyValues().add("modelValidationRules", modelValidation.rules);
		entityDefinition.getPropertyValues().add("modelValidationTemplateReferences", modelValidation.refs);
		entityDefinition.getPropertyValues().add("contextValidationRules", contextValidationRules);
		entityDefinition.getPropertyValues().add("contextValidationTemplateReferences", contextValidationTemplateReferences);
		entityDefinition.getPropertyValues().add("validationTemplates", templates);
		entityDefinition.getPropertyValues().add("validationClass", modelClass);
		String entityName = parserContext.getReaderContext().registerWithGeneratedName(entityDefinition);
		parserContext.registerComponent(new BeanComponentDefinition(entityDefinition, entityName));
		return null;
	}
	
	/**
	 * Parses nested validation rule and template-ref structures
	 * @param ruleNode the ValidationRule node
	 * @param modelClass the model class
	 * @return a @link{ValidationStructure} object whose
	 *  nested rules and template references are those
	 *  rules and template references stemming from the ruleNode.
	 */
	protected ValidationStructure parseNestedValidation(Element ruleNode, Class<?> modelClass) {
		
		ValidationStructure structure = new ValidationStructure();
		
		if (ruleNode == null) {
			return structure;
		}
		
		List<Element> validationRuleNodes = DomUtils.getChildElementsByTagName(ruleNode, "rule");
		List<Element> validationTemplateReferenceNodes = 
			DomUtils.getChildElementsByTagName(ruleNode, "template-ref");
		
		if ((validationRuleNodes == null || validationRuleNodes.isEmpty())
			&& (validationTemplateReferenceNodes == null 
					|| validationTemplateReferenceNodes.isEmpty())) {
			return structure;
		}
		
		if (validationRuleNodes != null) {
			for (Element rule : validationRuleNodes) {
				String path = rule.getAttribute("path");
				if (path != null && path.length() > 0 
						&& !path.contains("${") 
						&& !pathExists(modelClass, path)) {
					throw new ValidationParseException("Path \"" + path 
							+ "\" does not exist on class " + modelClass.getCanonicalName());
				}
				String type = rule.getAttribute("type");
				String value = rule.getAttribute("value");
				String message = rule.getAttribute("message");
				String errorPath = rule.getAttribute("errorPath");
				String validateWhenNotInRequest = rule.getAttribute("validateWhenNotInRequest");
				ValidationRule validationRule = new ValidationRule(path, type, value);
				validationRule.setMessage(message);
				validationRule.setErrorPath(errorPath);
				ValidationStructure subStructure = parseNestedValidation(rule, modelClass);
				validationRule.setRules(subStructure.rules);
				validationRule.setTemplateReferences(subStructure.refs);
				validationRule.setValidateWhenNotInRequest(Boolean.valueOf(validateWhenNotInRequest));
				structure.rules.add(validationRule);
			}
		}
		
		if (validationTemplateReferenceNodes != null) {
			for (Element templateReference : validationTemplateReferenceNodes) {
				String basePath = templateReference.getAttribute("basePath");
				if (basePath != null && basePath.length() > 0 
						&& !basePath.contains("${") 
						&& !pathExists(modelClass, basePath)) {
					throw new ValidationParseException("Path \"" + basePath 
							+ "\" does not exist on class " + modelClass.getCanonicalName());
				}
				String templateName = templateReference.getAttribute("templateName");
				ValidationTemplateReference templateRef = 
					new ValidationTemplateReference(basePath, templateName);
				structure.refs.add(templateRef);
			}
		}
		return structure;
	}
	
	/**
	 * Determine if a path exists on the given class.
	 * @param clazz Class to check 
	 * @param path Path to check
	 * @return true if path exists.
	 */
	public boolean pathExists(Class clazz, String path) {
        if (path.contains(".")) {
                Class intermediateClass = clazz;
                String[] pathTokens = path.split("\\.");
                for (String token : pathTokens) {
                        PropertyDescriptor descriptor =
                                BeanUtils.getPropertyDescriptor(intermediateClass, token);
                        if (descriptor == null) {
                                return false;
                        } else {
                                intermediateClass = descriptor.getPropertyType();
                        }
                }
        } else {
                if (!new BeanWrapperImpl(clazz).isReadableProperty(path)) {
                        return false;
                }
        }
        return true;
	}
	
	/**
	 * Exception when validation cannot be parsed.
	 * @author Clark Duplichien
	 * @author Taylor Wicksell
	 *
	 */
	public class ValidationParseException extends RuntimeException {
		
		public ValidationParseException() {
			super();		
		}
		
		public ValidationParseException(String message) {
			super(message);
		}
	}
	
	protected class ValidationStructure {
		public List<ValidationRule> rules = new ArrayList<ValidationRule>();
		public List<ValidationTemplateReference> refs = new ArrayList<ValidationTemplateReference>();
	}
}

