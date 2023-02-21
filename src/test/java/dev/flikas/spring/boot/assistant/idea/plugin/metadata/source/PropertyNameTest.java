package dev.flikas.spring.boot.assistant.idea.plugin.metadata.source;

import org.junit.jupiter.api.Test;

import static dev.flikas.spring.boot.assistant.idea.plugin.metadata.source.ConfigurationPropertyName.Form.UNIFORM;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyNameTest {

  @Test
  void of() {
    PropertyName name = PropertyName.of("spring.application.name");
    assertEquals(3, name.getNumberOfElements());
    assertEquals("spring", name.getElement(0, UNIFORM));
    assertEquals("application", name.getElement(1, UNIFORM));
    assertEquals("name", name.getElement(2, UNIFORM));
  }


  @Test
  void ofIfValid() {
  }


  @Test
  void ofCamelCase() {
  }


  @Test
  void toCamelCase() {
  }


  @Test
  void toKebabCase() {
  }


  @Test
  void appendAnyMapKey() {
  }


  @Test
  void appendAnyNumericalIndex() {
  }


  @Test
  void append() {
  }


  @Test
  void getParent() {
  }


  @Test
  void chop() {
  }


  @Test
  void subName() {
  }


  @Test
  void isParentOf() {
  }


  @Test
  void isAncestorOf() {
  }


  @Test
  void compare() {
  }


  @Test
  void defaultElementEquals() {
  }
}