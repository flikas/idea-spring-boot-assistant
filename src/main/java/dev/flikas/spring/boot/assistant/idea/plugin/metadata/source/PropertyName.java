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


  private int hashCode;


  @Override
  public PropertyName subName(int offset) {
    return new PropertyName(super.subName(offset).elements);
  }


  @Override
  protected int compare(String e1, ElementType type1, String e2, ElementType type2) {
    if (e1 != null && e2 != null && type1.isIndexed() && type2.isIndexed()
        && ((e1.equals("*") && type2 == ElementType.INDEXED && !e2.equals("#"))
        || (e2.equals("*") && type1 == ElementType.INDEXED && !e1.equals("#"))
        || (e1.equals("#") && type2 == ElementType.NUMERICALLY_INDEXED)
        || (e2.equals("#") && type1 == ElementType.NUMERICALLY_INDEXED))) {
      return 0;
    }
    return super.compare(e1, type1, e2, type2);
  }


  @Override
  boolean defaultElementEquals(Elements e1, Elements e2, int i) {
    ElementType type1 = e1.getType(i);
    ElementType type2 = e2.getType(i);
    if (type1.isIndexed() && type2.isIndexed()
        && ((type1 == ElementType.INDEXED && e2.getLength(i) == 1 && e2.charAt(i, 0) == '*' && !(e1.getLength(i) == 1 && e1.charAt(i, 0) == '#'))
        || (type2 == ElementType.INDEXED && e1.getLength(i) == 1 && e1.charAt(i, 0) == '*' && !(e2.getLength(i) == 1 && e2.charAt(i, 0) == '#'))
        || (type1 == ElementType.NUMERICALLY_INDEXED && e2.getLength(i) == 1 && e2.charAt(i, 0) == '#')
        || (type2 == ElementType.NUMERICALLY_INDEXED && e1.getLength(i) == 1 && e1.charAt(i, 0) == '#'))) {
      return true;
    }
    return super.defaultElementEquals(e1, e2, i);
  }


  /**
   * Because we support wildcard indexes, this method has been overwritten to ignore any element within '[]'.
   */
  @Override
  public int hashCode() {
    int hc = this.hashCode;
    Elements elements = this.elements;
    if (hc == 0 && elements.getSize() != 0) {
      for (int elementIndex = 0; elementIndex < elements.getSize(); elementIndex++) {
        int elementHashCode = 0;
        if (elements.getType(elementIndex).isIndexed()) continue;
        int length = elements.getLength(elementIndex);
        for (int i = 0; i < length; i++) {
          char ch = elements.charAt(elementIndex, i);
          ch = Character.toLowerCase(ch);
          if (ElementsParser.isAlphaNumeric(ch)) {
            elementHashCode = 31 * elementHashCode + ch;
          }
        }
        hc = 31 * hc + elementHashCode;
      }
      this.hashCode = hc;
    }
    return hc;
  }
}
