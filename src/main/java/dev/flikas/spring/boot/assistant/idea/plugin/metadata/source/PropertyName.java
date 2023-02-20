package dev.flikas.spring.boot.assistant.idea.plugin.metadata.source;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * A {@linkplain ConfigurationPropertyName} that accepts wildcard index.
 */
public class PropertyName extends ConfigurationPropertyName {
  public static final PropertyName EMPTY = new PropertyName(Elements.EMPTY);

  private static final Set<Character> SEPARATORS = Set.of('-', '_');


  private PropertyName(Elements elements) {
    super(elements);
  }


  public static PropertyName of(String propertyName) {
    return new PropertyName(elementsOf(propertyName, false));
  }


  public static PropertyName ofIfValid(String propertyName) {
    Elements elements = elementsOf(propertyName, true);
    return elements != null ? new PropertyName(elements) : null;
  }


  public static PropertyName ofCamelCase(String camelCase) {
    return of(toKebabCase(camelCase));
  }


  public static String toCamelCase(String kebabCase) {
    char[] dashedName = kebabCase.toCharArray();
    char[] fieldName = new char[dashedName.length];
    int j = 0;
    boolean upperNeeded = false;
    for (char c : dashedName) {
      if (Character.isJavaIdentifierPart(c)) {
        if (upperNeeded) {
          c = Character.toUpperCase(c);
          upperNeeded = false;
        }
        fieldName[j++] = c;
      } else {
        upperNeeded = true;
      }
    }
    return new String(fieldName, 0, j);
  }


  public static String toKebabCase(String camelCase) {
    StringBuilder dashed = new StringBuilder();
    Character previous = null;
    for (int i = 0; i < camelCase.length(); i++) {
      char current = camelCase.charAt(i);
      if (SEPARATORS.contains(current)) {
        dashed.append("-");
      } else if (Character.isUpperCase(current) && previous != null && !SEPARATORS.contains(previous)) {
        dashed.append("-").append(current);
      } else {
        dashed.append(current);
      }
      previous = current;

    }
    return dashed.toString().toLowerCase(Locale.ENGLISH);
  }


  public PropertyName appendAnyMapKey() {
    return append("[*]");
  }


  public PropertyName appendAnyNumericalIndex() {
    return append("[#]");
  }


  @Override
  public PropertyName append(String suffix) {
    if (!StringUtils.hasLength(suffix)) {
      return this;
    } else {
      Elements additionalElements = probablySingleElementOf(suffix);
      return new PropertyName(this.elements.append(additionalElements));
    }
  }


  @Override
  public PropertyName getParent() {
    int numberOfElements = getNumberOfElements();
    return (numberOfElements <= 1) ? EMPTY : chop(numberOfElements - 1);
  }


  @Override
  public PropertyName chop(int size) {
    if (size >= getNumberOfElements()) {
      return this;
    } else {
      return new PropertyName(this.elements.chop(size));
    }
  }


  @Override
  public PropertyName subName(int offset) {
    if (offset == 0) {
      return this;
    }
    if (offset == getNumberOfElements()) {
      return this;
    }
    if (offset < 0 || offset > getNumberOfElements()) {
      throw new IndexOutOfBoundsException("Offset: " + offset + ", NumberOfElements: " + getNumberOfElements());
    }
    return new PropertyName(this.elements.subElements(offset));
  }


  public boolean isParentOf(PropertyName name) {
    return super.isParentOf(name);
  }


  public boolean isAncestorOf(PropertyName name) {
    return super.isAncestorOf(name);
  }


  @Override
  protected int compare(String e1, ElementType type1, String e2, ElementType type2) {
    if (e1 != null && e2 != null && type1.isIndexed() && type2.isIndexed()) {
      if ((e1.equals("*") && type2 == ElementType.INDEXED)
          || (e2.equals("*") && type1 == ElementType.INDEXED)
          || (e1.equals("#") && type2 == ElementType.NUMERICALLY_INDEXED)
          || (e2.equals("#") && type1 == ElementType.NUMERICALLY_INDEXED)) {
        return 0;
      }
    }
    return super.compare(e1, type1, e2, type2);
  }


  @Override
  boolean defaultElementEquals(Elements e1, Elements e2, int i) {
    ElementType type1 = e1.getType(i);
    ElementType type2 = e2.getType(i);
    if (type1.isIndexed() && type2.isIndexed()) {
      if ((type1 == ElementType.INDEXED && e2.getLength(i) == 1 && e2.charAt(i, 0) == '*')
          || (type2 == ElementType.INDEXED && e1.getLength(i) == 1 && e1.charAt(i, 0) == '*')
          || (type1 == ElementType.NUMERICALLY_INDEXED && e2.getLength(i) == 1 && e2.charAt(i, 0) == '#')
          || (type2 == ElementType.NUMERICALLY_INDEXED && e1.getLength(i) == 1 && e1.charAt(i, 0) == '#')) {
        return true;
      }
    }
    return super.defaultElementEquals(e1, e2, i);
  }
}
