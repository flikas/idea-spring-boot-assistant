package dev.flikas.spring.boot.assistant.idea.plugin.metadata.index;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Set;

@UtilityClass
public class PropertyNameUtils {
  private static final Set<Character> SEPARATORS = Set.of('-', '_');


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
}
