/**
 * Copyright 2019 Jordan Zimmerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.soabase.recordbuilder.core;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Inherited
public @interface RecordBuilder {
    @Target({ElementType.TYPE, ElementType.PACKAGE})
    @Retention(RetentionPolicy.SOURCE)
    @Inherited
    @interface Include {
        /**
         * @return list of classes to include
         */
        Class<?>[] value() default {};

        /**
         * Synonym for {@code value()}. When using the other attributes it maybe more clear to
         * use {@code classes()} instead of {@code value()}. Note: both attributes are applied
         * (i.e. a union of classes from both attributes).
         *
         * @return list of classes
         */
        Class<?>[] classes() default {};

        /**
         * Optional list of package names. All records in the packages will get processed as
         * if they were listed as classes to include.
         *
         * @return list of package names
         */
        String[] packages() default {};

        /**
         * Pattern used to generate the package for the generated class. The value
         * is the literal package name however two replacement values can be used. '@'
         * is replaced with the package of the {@code Include} annotation. '*' is replaced with
         * the package of the included class.
         *
         * @return package pattern
         */
        String packagePattern() default "@";
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Options {
        /**
         * The builder class name will be the name of the record (prefixed with any enclosing class) plus this suffix. E.g.
         * if the record name is "Foo", the builder will be named "FooBuilder".
         */
        String suffix() default "Builder";

        /**
         * Used by {@code RecordInterface}. The generated record will have the same name as the annotated interface
         * plus this suffix. E.g. if the interface name is "Foo", the record will be named "FooRecord".
         */
        String interfaceSuffix() default "Record";

        /**
         * The name to use for the copy builder
         */
        String copyMethodName() default "builder";

        /**
         * The name to use for the builder
         */
        String builderMethodName() default "builder";

        /**
         * The name to use for the build method
         */
        String buildMethodName() default "build";

        /**
         * The name to use for the from-to-wither method
         */
        String fromMethodName() default "from";

        /**
         * The name to use for the method that returns the record components as a stream
         */
        String componentsMethodName() default "stream";

        /**
         * If true, a "With" interface is generated and an associated static factory
         */
        boolean enableWither() default true;

        /**
         * The name to use for the nested With class
         */
        String withClassName() default "With";

        /**
         * The prefix to use for the methods in the With class
         */
        String withClassMethodPrefix() default "with";

        /**
         * Return the comment to place at the top of generated files. Return null or an empty string for no comment.
         */
        String fileComment() default "Auto generated by io.soabase.recordbuilder.core.RecordBuilder: https://github.com/Randgalt/record-builder";

        /**
         * Return the file indent to use
         */
        String fileIndent() default "    ";

        /**
         * If the record is declared inside of another class, the outer class's name will
         * be prefixed to the builder name if this returns true.
         */
        boolean prefixEnclosingClassNames() default true;

        /**
         * If true, any annotations (if applicable) on record components are copied
         * to the builder methods
         *
         * @return true/false
         */
        boolean inheritComponentAnnotations() default true;

        /**
         * Set the default value of {@code Optional} record components to
         * {@code Optional.empty()}
         */
        boolean emptyDefaultForOptional() default true;

        /**
         * Add non-optional setter methods for optional record components.
         */
        boolean addConcreteSettersForOptional() default false;

        /**
         * Add not-null checks for record components annotated with any annotation named either "NotNull",
         * "NoNull", or "NonNull" (see {@link #interpretNotNullsPattern()} for the actual regex matching pattern).
         */
        boolean interpretNotNulls() default false;

        /**
         * If {@link #interpretNotNulls()} is true, this is the regex pattern used to determine if an annotation name
         * means "not null"
         */
        String interpretNotNullsPattern() default "(?i)((notnull)|(nonnull)|(nonull))";

        /**
         * <p>Pass built records through the Java Validation API if it's available in the classpath.</p>
         *
         * <p>IMPORTANT:
         * if this option is enabled you must include the {@code record-builder-validator} dependency in addition
         * to {@code record-builder-core}. {@code record-builder-validator} is implemented completely via reflection and
         * does not require other dependencies. Alternatively, you can define your own class with the package {@code package io.soabase.recordbuilder.validator;}
         * named {@code RecordBuilderValidator} which has a public static method: {@code public static <T> T validate(T o)}.</p>
         */
        boolean useValidationApi() default false;

        /**
         * Adds special handling for record components of type {@link java.util.List}, {@link java.util.Set},
         * {@link java.util.Map} and {@link java.util.Collection}. When the record is built, any components
         * of these types are passed through an added shim method that uses the corresponding immutable collection
         * (e.g. {@code List.copyOf(o)}) or an empty immutable collection if the component is {@code null}.
         */
        boolean useImmutableCollections() default false;

        /**
         * When enabled, collection types ({@code List}, {@code Set} and {@code Map}) are handled specially.
         * The setters for these types now create an internal collection and items are added to that
         * collection. Additionally, "adder" methods prefixed with {@link #singleItemBuilderPrefix()} are created
         * to add single items to these collections.
         */
        boolean addSingleItemCollectionBuilders() default false;

        /**
         * The prefix for adder methods when {@link #addSingleItemCollectionBuilders()} is enabled
         */
        String singleItemBuilderPrefix() default "add";

        /**
         * When enabled, adds functional methods to the nested "With" class (such as {@code map()} and {@code accept()}).
         */
        boolean addFunctionalMethodsToWith() default false;

        /**
         * If set, all builder setter methods will be prefixed with this string. Camel-casing will
         * still be enforced, so if this option is set to "set" a field named "myField" will get
         * a corresponding setter named "setMyField".
         */
        String setterPrefix() default "";

        /**
         * If true, getters will be generated for the Builder class.
         */
        boolean enableGetters() default true;

        /**
         * If set, all builder getter methods will be prefixed with this string. Camel-casing will
         * still be enforced, so if this option is set to "get", a field named "myField" will get
         * a corresponding getter named "getMyField".
         */
        String getterPrefix() default "";

        /**
         * If set, all boolean builder getter methods will be prefixed with this string.
         * Camel-casing will still be enforced, so if this option is set to "is", a field named
         * "myField" will get a corresponding getter named "isMyField".
         */
        String booleanPrefix() default "";

        /**
         * If set, the Builder will contain an internal interface with this name. This interface
         * contains getters for all the fields in the Record prefixed with the value supplied in
         * {@link this.getterPrefix} and {@link this.booleanPrefix}. This interface can be
         * implemented by the original Record to have proper bean-style prefixed getters.
         *
         * Please note that unless either of the aforementioned prefixes are set,
         * this option does nothing.
         */
        String beanClassName() default "";

        /**
         * If true, generated classes are annotated with {@code RecordBuilderGenerated} which has a retention
         * policy of {@code CLASS}. This ensures that analyzers such as Jacoco will ignore the generated class.
         */
        boolean addClassRetainedGenerated() default false;

        /**
         * The {@link #fromMethodName} method instantiates an internal private class. This is the
         * name of that class.
         */
        String fromWithClassName() default "_FromWith";

        /**
         * If true, a functional-style builder is added so that record instances can be instantiated
         * without {@code new}.
         */
        boolean addStaticBuilder() default true;

        /**
         * If {@link #addSingleItemCollectionBuilders()} and {@link #useImmutableCollections()} are enabled the builder
         * uses an internal class to track changes to lists. This is the name of that class.
         */
        String mutableListClassName() default "_MutableList";

        /**
         * If {@link #addSingleItemCollectionBuilders()} and {@link #useImmutableCollections()} are enabled the builder
         * uses an internal class to track changes to sets. This is the name of that class.
         */
        String mutableSetClassName() default "_MutableSet";

        /**
         * If {@link #addSingleItemCollectionBuilders()} and {@link #useImmutableCollections()} are enabled the builder
         * uses an internal class to track changes to maps. This is the name of that class.
         */
        String mutableMapClassName() default "_MutableMap";
    }

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.ANNOTATION_TYPE)
    @Inherited
    @interface Template {
        RecordBuilder.Options options();

        boolean asRecordInterface() default false;
    }
}
