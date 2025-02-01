package dev.flikas.spring.boot.assistant.idea.plugin.metadata.source;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents all entries present in {@code classpath:/META-INF/spring-configuration-metadata.json}.
 * <p>
 * Reference to <a href="https://docs.spring.io/spring-boot/docs/3.0.2/reference/html/configuration-metadata.html">Spring Boot Doc</a>.
 */
@Data
public class ConfigurationMetadata {
  /**
   * The "groups" are higher level items that do not themselves specify a value but instead provide a contextual grouping for properties.
   */
  @Nullable
  private List<Group> groups;
  /**
   * Each "property" is a configuration item that the user specifies with a given value.
   */
  private List<Property> properties = new ArrayList<>();
  /**
   * "hints" are additional information used to assist the user in configuring a given property.
   */
  @Nullable
  private List<Hint> hints;


  public boolean isEmpty() {
    return (hints == null || hints.isEmpty()) &&
        (groups == null || groups.isEmpty()) &&
        (properties == null || properties.isEmpty());
  }


  /**
   * The "groups" are higher level items that do not themselves specify a value but instead provide a contextual grouping for properties.
   */
  @Data
  public static class Group {
    /**
     * The full name of the group. This attribute is mandatory.
     */
    private String name;
    /**
     * The class name of the data type of the group. For example, if the group were based on a class annotated with
     * {@code @ConfigurationProperties}, the attribute would contain the fully qualified name of that class.
     * If it were based on a @Bean method, it would be the return type of that method.
     * If the type is not known, the attribute may be omitted.
     */
    @Nullable
    private String type;
    /**
     * A short description of the group that can be displayed to users. If no description is available, it may be omitted.
     * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
     * The last line in the description should end with a period (.).
     */
    @Nullable
    private String description;
    /**
     * The class name of the source that contributed this group. For example, if the group were based on a {@code @Bean} method
     * annotated with {@code @ConfigurationProperties}, this attribute would contain the fully qualified name of the
     * {@code @Configuration} class that contains the method. If the source type is not known, the attribute may be omitted.
     */
    @Nullable
    private String sourceType;
    /**
     * The full name of the method (include parenthesis and argument types) that contributed this group
     * (for example, the name of a {@code @ConfigurationProperties} annotated {@code @Bean} method).
     * If the source method is not known, it may be omitted.
     */
    @Nullable
    private String sourceMethod;
  }


  /**
   * Each "property" is a configuration item that the user specifies with a given value.
   */
  @Data
  public static class Property {
    /**
     * The full name of the PROPERTY. Names are in lower-case period-separated form (for example, server.servlet.path).
     * This attribute is mandatory.
     */
    private String name;
    /**
     * The full signature of the data type of the property (for example, {@code java.lang.String}) but also a full generic type
     * (such as {@code java.util.Map<java.lang.String,com.example.MyEnum>}).
     * You can use this attribute to guide the user as to the types of values that they can enter.
     * For consistency, the type of a primitive is specified by using its wrapper counterpart
     * (for example, boolean becomes java.lang.Boolean).
     * Note that this class may be a complex type that gets converted from a String as values are bound.
     * If the type is not known, it may be omitted.
     */
    @Nullable
    private String type;
    /**
     * A short description of the property that can be displayed to users.
     * If no description is available, it may be omitted.
     * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
     * The last line in the description should end with a period (.).
     */
    @Nullable
    private String description;
    /**
     * The class name of the source that contributed this property.
     * For example, if the property were from a class annotated with {@code @ConfigurationProperties},
     * this attribute would contain the fully qualified name of that class.
     * If the source type is unknown, it may be omitted.
     */
    @Nullable
    private String sourceType;
    /**
     * The default value, which is used if the property is not specified.
     * If the type of the property is an array, it can be an array of value(s).
     * If the default value is unknown, it may be omitted.
     */
    @Nullable
    private Object defaultValue;
    /**
     * Specify whether the property is deprecated.
     * If the field is not deprecated or if that information is not known, it may be omitted.
     * <p>
     * Prior to Spring Boot 1.3, a single deprecated boolean attribute can be used instead of the deprecation element.
     * This is still supported in a deprecated fashion and should no longer be used.
     * If no reason and replacement are available, an empty deprecation object should be set.
     */
    @Nullable
    private Deprecation deprecation;


    @Data
    public static class Deprecation {
      /**
       * The level of deprecation, which can be either warning (the default) or error.
       * When a property has a warning deprecation level, it should still be bound in the environment.
       * However, when it has an error deprecation level, the property is no longer managed and is not bound.
       */
      @Nullable
      private Level level = Level.WARNING;
      /**
       * A short description of the reason why the property was deprecated.
       * If no reason is available, it may be omitted.
       * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
       * The last line in the description should end with a period (.).
       */
      @Nullable
      private String reason;
      /**
       * The full name of the property that replaces this deprecated property.
       * If there is no replacement for this property, it may be omitted.
       */
      @Nullable
      private String replacement;


      /**
       * The level of deprecation, which can be either warning or error.
       * When a property has a warning deprecation level, it should still be bound in the environment.
       * However, when it has an error deprecation level, the property is no longer managed and is not bound.
       */
      public enum Level {
        /**
         * When a property has a warning deprecation level, it should still be bound in the environment.
         */
        @SerializedName("warning")
        WARNING,
        /**
         * However, when it has an error deprecation level, the property is no longer managed and is not bound.
         */
        @SerializedName("error")
        ERROR
      }
    }
  }


  /**
   * "hints" are additional information used to assist the user in configuring a given property.
   */
  @Data
  public static class Hint {
    /**
     * The full name of the property to which this hint refers.
     * Names are in lower-case period-separated form (such as spring.mvc.servlet.path).
     * If the property refers to a map (such as {@code system.contexts}), the hint either applies to the keys of the map
     * ({@code system.contexts.keys}) or the values ({@code system.contexts.values}) of the map.
     * This attribute is mandatory.
     */
    private String name;
    /**
     * A list of valid values as defined by the ValueHint object.
     * Each entry defines the value and may have a description.
     */
    @Nullable
    private ValueHint[] values;
    /**
     * A list of providers as defined by the ValueProvider object.
     * Each entry defines the name of the provider and its parameters, if any.
     */
    @Nullable
    private ValueProvider[] providers;


    @Data
    public static class ValueHint {
      /**
       * A valid value for the element to which the hint refers.
       * If the type of the property is an array, it can also be an array of value(s).
       * This attribute is mandatory.
       */
      private Object value;
      /**
       * A short description of the value that can be displayed to users.
       * If no description is available, it may be omitted.
       * It is recommended that descriptions be short paragraphs, with the first line providing a concise summary.
       * The last line in the description should end with a period (.).
       */
      @Nullable
      private String description;
    }


    @Data
    public static class ValueProvider {
      /**
       * The name of the provider to use to offer additional content assistance for the element to which the hint refers.
       */
      private Type name;
      /**
       * Any additional parameter that the provider supports (check the documentation of the provider for more details).
       */
      @Nullable
      private Map<String, Object> parameters;


      public enum Type {
        /**
         * Permits any additional value to be provided.
         * Regular value validation based on the property type should be applied if this is supported.
         * <p>
         * This provider is typically used if you have a list of values and any extra values should still be considered as valid.
         */
        @SerializedName("any")
        ANY,
        /**
         * Auto-completes the classes available in the project.
         * <p>
         * This provider supports the following parameters:
         * <table>
         * <colgroup>
         * <col style="width: 12.5%;">
         * <col style="width: 12.5%;">
         * <col style="width: 25%;">
         * <col style="width: 50%;">
         * </colgroup>
         * <thead>
         * <tr>
         * <th>Parameter</th>
         * <th>Type</th>
         * <th>Default value</th>
         * <th>Description</th>
         * </tr>
         * </thead>
         * <tbody>
         * <tr>
         * <td><code>target</code></p></td>
         * <td><code>String</code> (<code>Class</code>)</p></td>
         * <td><em>none</em></p></td>
         * <td>The fully qualified name of the class that should be assignable to the chosen value.
         * Typically used to filter out-non candidate classes.
         * Note that this information can be provided by the type itself by exposing a class with the appropriate upper bound.</p></td>
         * </tr>
         * <tr>
         * <td><p><code>concrete</code></p></td>
         * <td><p><code>boolean</code></p></td>
         * <td><p>true</p></td>
         * <td><p>Specify whether only concrete classes are to be considered as valid candidates.</p></td>
         * </tr>
         * </tbody>
         * </table>
         */
        @SerializedName("class-reference")
        CLASS_REFERENCE,
        /**
         * The <strong>handle-as</strong> provider lets you substitute the type of the property to a more high-level type.
         * This typically happens when the property has a <code>java.lang.String</code> type, because you do not want your configuration classes to rely on classes that may not be on the classpath.
         * This provider supports the following parameters:
         * <table>
         * <colgroup>
         * <col style="width: 12.5%;">
         * <col style="width: 12.5%;">
         * <col style="width: 25%;">
         * <col style="width: 50%;">
         * </colgroup>
         * <thead>
         * <tr>
         * <th>Parameter</th>
         * <th>Type</th>
         * <th>Default value</th>
         * <th>Description</th>
         * </tr>
         * </thead>
         * <tbody>
         * <tr>
         * <td><p><strong><code>target</code></strong></p></td>
         * <td><p><code>String</code> (<code>Class</code>)</p></td>
         * <td><p><em>none</em></p></td>
         * <td><p>The fully qualified name of the type to consider for the property.
         * This parameter is mandatory.</p></td>
         * </tr>
         * </tbody>
         * </table>
         * The following types can be used:
         * <ul>
         * <li>
         * <p>Any <code>java.lang.Enum</code>: Lists the possible values for the property.
         * (We recommend defining the property with the <code>Enum</code> type, as no further hint should be required for the IDE to auto-complete the values)</p>
         * </li>
         * <li>
         * <p><code>java.nio.charset.Charset</code>: Supports auto-completion of charset/encoding values (such as <code>UTF-8</code>)</p>
         * </li>
         * <li>
         * <p><code>java.util.Locale</code>: auto-completion of locales (such as <code>en_US</code>)</p>
         * </li>
         * <li>
         * <p><code>org.springframework.util.MimeType</code>: Supports auto-completion of content type values (such as <code>text/plain</code>)</p>
         * </li>
         * <li>
         * <p><code>org.springframework.core.io.Resource</code>: Supports auto-completion of Spring’s Resource abstraction to refer to a file on the filesystem or on the classpath (such as <code>classpath:/sample.properties</code>)</p>
         * </li>
         * </ul>
         */
        @SerializedName("handle-as")
        HANDLE_AS,
        /**
         * The <strong>logger-name</strong> provider auto-completes valid logger names and <a href="features.html#features.logging.log-groups">logger groups</a>.
         * Typically, package and class names available in the current project can be auto-completed.
         * If groups are enabled (default) and if a custom logger group is identified in the configuration, auto-completion for it should be provided.
         * Specific frameworks may have extra magic logger names that can be supported as well.
         * <p>
         * This provider supports the following parameters:
         * <table>
         * <colgroup>
         * <col style="width: 12.5%;">
         * <col style="width: 12.5%;">
         * <col style="width: 25%;">
         * <col style="width: 50%;">
         * </colgroup>
         * <thead>
         * <tr>
         * <th>Parameter</th>
         * <th>Type</th>
         * <th>Default value</th>
         * <th>Description</th>
         * </tr>
         * </thead>
         * <tbody>
         * <tr>
         * <td><p><code>group</code></p></td>
         * <td><p><code>boolean</code></p></td>
         * <td><p><code>true</code></p></td>
         * <td><p>Specify whether known groups should be considered.</p></td>
         * </tr>
         * </tbody>
         * </table>
         * Since a logger name can be any arbitrary name, this provider should allow any value but could highlight
         * valid package and class names that are not available in the project’s classpath.
         */
        @SerializedName("logger-name")
        LOGGER_NAME,
        /**
         * The <strong>spring-bean-reference</strong> provider auto-completes the beans that are defined in the
         * configuration of the current project.
         * <p>
         * This provider supports the following parameters:
         * <table>
         * <colgroup>
         * <col style="width: 12.5%;">
         * <col style="width: 12.5%;">
         * <col style="width: 25%;">
         * <col style="width: 50%;">
         * </colgroup>
         * <thead>
         * <tr>
         * <th>Parameter</th>
         * <th>Type</th>
         * <th>Default value</th>
         * <th>Description</th>
         * </tr>
         * </thead>
         * <tbody>
         * <tr>
         * <td<p><code>target</code></p></td>
         * <td<p><code>String</code> (<code>Class</code>)</p></td>
         * <td<p><em>none</em></p></td>
         * <td<p>The fully qualified name of the bean class that should be assignable to the candidate.
         * Typically used to filter out non-candidate beans.</p></td>
         * </tr>
         * </tbody>
         * </table>
         */
        @SerializedName("spring-bean-reference")
        SPRING_BEAN_REFERENCE,
        /**
         * The <strong>spring-profile-name</strong> provider auto-completes the Spring profiles that are defined
         * in the configuration of the current project.
         */
        @SerializedName("spring-profile-name")
        SPRING_PROFILE_NAME
      }
    }
  }
}
