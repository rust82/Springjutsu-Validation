
package org.springjutsu.validation.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ValidationRuleTest {

	@Test
	public void testValidationRule() {
		ValidationRule rule = new ValidationRule("test", "type", "value");
		assertEquals("test", rule.getPath());
		assertEquals("type", rule.getType());
		assertEquals("value", rule.getValue());
		assertNotNull(rule.getRules());
		assertEquals(0, rule.getRules().size());
	}

	@Test
	public void testCloneWithPath() {
		ValidationRule rule = new ValidationRule("test", "type", "value");
		ValidationRule clone = rule.cloneWithPath("newPath");
		assertEquals("newPath", clone.getPath());
		assertEquals("type", clone.getType());
		assertEquals("value", clone.getValue());
		assertNotNull(clone.getRules());
		assertEquals(0, clone.getRules().size());
		
	}

	@Test
	public void testHasChildren() {
		ValidationRule rule = new ValidationRule("test", "type", "value");
		assertFalse(rule.hasChildren());
		rule.addRule(new ValidationRule("subRule", "subRuleType", "subRuleValue"));
		assertTrue(rule.hasChildren());
	}

	@Test
	public void testToString() {
		ValidationRule rule = new ValidationRule("", "", "");
		assertEquals("<rule />", rule.toString());
		rule.setPath("path");
		assertEquals("<rule path=\"path\" />", rule.toString());
		rule.setType("type");
		assertEquals("<rule path=\"path\" type=\"type\" />", rule.toString());
		rule.setValue("value");
		assertEquals("<rule path=\"path\" type=\"type\" value=\"value\" />", rule.toString());
		rule.setMessage("message");
		assertEquals("<rule path=\"path\" type=\"type\" value=\"value\" " +
						"message=\"message\" />", rule.toString());
		rule.setErrorPath("errorPath");
		assertEquals("<rule path=\"path\" type=\"type\" value=\"value\" " +
						"message=\"message\" errorPath=\"errorPath\" />", rule.toString());
		
	}

}
