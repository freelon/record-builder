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
package io.soabase.recordbuilder.processor;

import com.squareup.javapoet.*;
import io.soabase.recordbuilder.core.RecordBuilder;

import javax.lang.model.element.Modifier;
import java.util.*;

import static io.soabase.recordbuilder.processor.RecordBuilderProcessor.generatedRecordBuilderAnnotation;

class CollectionBuilderUtils {
    private final boolean useImmutableCollections;
    private final boolean addSingleItemCollectionBuilders;
    private final String listShimName;
    private final String mapShimName;
    private final String setShimName;
    private final String collectionShimName;

    private final String listMakerMethodName;
    private final String mapMakerMethodName;
    private final String setMakerMethodName;

    private boolean needsListShim;
    private boolean needsMapShim;
    private boolean needsSetShim;
    private boolean needsCollectionShim;

    private boolean needsListMutableMaker;
    private boolean needsMapMutableMaker;
    private boolean needsSetMutableMaker;

    private static final TypeName listType = TypeName.get(List.class);
    private static final TypeName mapType = TypeName.get(Map.class);
    private static final TypeName setType = TypeName.get(Set.class);
    private static final TypeName collectionType = TypeName.get(Collection.class);

    private static final Class<?> mutableListType = ArrayList.class;
    private static final Class<?> mutableMapType = HashMap.class;
    private static final Class<?> mutableSetType = HashSet.class;
    private static final TypeName mutableListTypeName = TypeName.get(mutableListType);
    private static final TypeName mutableMapTypeName = TypeName.get(mutableMapType);
    private static final TypeName mutableSetTypeName = TypeName.get(mutableSetType);

    private static final TypeVariableName tType = TypeVariableName.get("T");
    private static final TypeVariableName kType = TypeVariableName.get("K");
    private static final TypeVariableName vType = TypeVariableName.get("V");
    private static final ParameterizedTypeName parameterizedListType = ParameterizedTypeName.get(ClassName.get(List.class), tType);
    private static final ParameterizedTypeName parameterizedMapType = ParameterizedTypeName.get(ClassName.get(Map.class), kType, vType);
    private static final ParameterizedTypeName parameterizedSetType = ParameterizedTypeName.get(ClassName.get(Set.class), tType);
    private static final ParameterizedTypeName parameterizedCollectionType = ParameterizedTypeName.get(ClassName.get(Collection.class), tType);

    CollectionBuilderUtils(List<RecordClassType> recordComponents, RecordBuilder.Options metaData) {
        useImmutableCollections = metaData.useImmutableCollections();
        addSingleItemCollectionBuilders = metaData.addSingleItemCollectionBuilders();

        listShimName = disambiguateGeneratedMethodName(recordComponents, "__list", 0);
        mapShimName = disambiguateGeneratedMethodName(recordComponents, "__map", 0);
        setShimName = disambiguateGeneratedMethodName(recordComponents, "__set", 0);
        collectionShimName = disambiguateGeneratedMethodName(recordComponents, "__collection", 0);

        listMakerMethodName = disambiguateGeneratedMethodName(recordComponents, "__ensureListMutable", 0);
        setMakerMethodName = disambiguateGeneratedMethodName(recordComponents, "__ensureSetMutable", 0);
        mapMakerMethodName = disambiguateGeneratedMethodName(recordComponents, "__ensureMapMutable", 0);
    }

    enum SingleItemsMetaDataMode {
        STANDARD,
        STANDARD_FOR_SETTER,
        EXCLUDE_WILDCARD_TYPES
    }

    record SingleItemsMetaData(Class<?> singleItemCollectionClass, List<TypeName> typeArguments, TypeName wildType) {}

    Optional<SingleItemsMetaData> singleItemsMetaData(RecordClassType component, SingleItemsMetaDataMode mode) {
        if (addSingleItemCollectionBuilders && (component.typeName() instanceof ParameterizedTypeName parameterizedTypeName)) {
            Class<?> collectionClass = null;
            ClassName wildcardClass = null;
            int typeArgumentQty = 0;
            if (isList(component)) {
                collectionClass = mutableListType;
                wildcardClass = ClassName.get(Collection.class);
                typeArgumentQty = 1;
            } else if (isSet(component)) {
                collectionClass = mutableSetType;
                wildcardClass = ClassName.get(Collection.class);
                typeArgumentQty = 1;
            } else if (isMap(component)) {
                collectionClass = mutableMapType;
                wildcardClass = (ClassName) component.rawTypeName();
                typeArgumentQty = 2;
            }
            var hasWildcardTypeArguments = hasWildcardTypeArguments(parameterizedTypeName, typeArgumentQty);
            if (collectionClass != null) {
                return switch (mode) {
                    case STANDARD -> singleItemsMetaDataWithWildType(parameterizedTypeName, collectionClass, wildcardClass, typeArgumentQty);

                    case STANDARD_FOR_SETTER -> {
                        if (hasWildcardTypeArguments) {
                            yield Optional.of(new SingleItemsMetaData(collectionClass, parameterizedTypeName.typeArguments, component.typeName()));
                        }
                        yield singleItemsMetaDataWithWildType(parameterizedTypeName, collectionClass, wildcardClass, typeArgumentQty);
                    }

                    case EXCLUDE_WILDCARD_TYPES -> {
                        if (hasWildcardTypeArguments) {
                            yield Optional.empty();
                        }
                        yield singleItemsMetaDataWithWildType(parameterizedTypeName, collectionClass, wildcardClass, typeArgumentQty);
                    }
                };
            }
        }
        return Optional.empty();
    }

    boolean isImmutableCollection(RecordClassType component) {
        return useImmutableCollections && (isList(component) || isMap(component) || isSet(component) || component.rawTypeName().equals(collectionType));
    }

    boolean isList(RecordClassType component) {
        return component.rawTypeName().equals(listType);
    }

    boolean isMap(RecordClassType component) {
        return component.rawTypeName().equals(mapType);
    }

    boolean isSet(RecordClassType component) {
        return component.rawTypeName().equals(setType);
    }

    void add(CodeBlock.Builder builder, RecordClassType component) {
        if (useImmutableCollections) {
            if (isList(component)) {
                needsListShim = true;
                needsListMutableMaker = true;
                builder.add("$L($L)", listShimName, component.name());
            } else if (isMap(component)) {
                needsMapShim = true;
                needsMapMutableMaker = true;
                builder.add("$L($L)", mapShimName, component.name());
            } else if (isSet(component)) {
                needsSetShim = true;
                needsSetMutableMaker = true;
                builder.add("$L($L)", setShimName, component.name());
            } else if (component.rawTypeName().equals(collectionType)) {
                needsCollectionShim = true;
                builder.add("$L($L)", collectionShimName, component.name());
            } else {
                builder.add("$L", component.name());
            }
        } else {
            builder.add("$L", component.name());
        }
    }

    String mutableMakerName(RecordClassType component) {
        if (isList(component)) {
            return listMakerMethodName;
        } else if (isMap(component)) {
            return mapMakerMethodName;
        } else if (isSet(component)) {
            return setMakerMethodName;
        } else {
            throw new IllegalArgumentException(component + " is not a supported collection type");
        }
    }

    void addShims(TypeSpec.Builder builder) {
        if (!useImmutableCollections) {
            return;
        }

        if (needsListShim) {
            builder.addMethod(buildShimMethod(listShimName, listType, parameterizedListType, tType));
        }
        if (needsSetShim) {
            builder.addMethod(buildShimMethod(setShimName, setType, parameterizedSetType, tType));
        }
        if (needsMapShim) {
            builder.addMethod(buildShimMethod(mapShimName, mapType, parameterizedMapType, kType, vType));
        }
        if (needsCollectionShim) {
            builder.addMethod(buildCollectionsShimMethod());
        }
    }

    void addMutableMakers(TypeSpec.Builder builder) {
        if (!useImmutableCollections) {
            return;
        }

        if (needsListMutableMaker) {
            builder.addMethod(buildMutableMakerMethod(listMakerMethodName, mutableListTypeName, parameterizedListType, tType));
        }
        if (needsSetMutableMaker) {
            builder.addMethod(buildMutableMakerMethod(setMakerMethodName, mutableSetTypeName, parameterizedSetType, tType));
        }
        if (needsMapMutableMaker) {
            builder.addMethod(buildMutableMakerMethod(mapMakerMethodName, mutableMapTypeName, parameterizedMapType, kType, vType));
        }
    }

    private Optional<SingleItemsMetaData> singleItemsMetaDataWithWildType(ParameterizedTypeName parameterizedTypeName, Class<?> collectionClass, ClassName wildcardClass, int typeArgumentQty) {
        TypeName wildType;
        if (typeArgumentQty == 1) {
            wildType = ParameterizedTypeName.get(wildcardClass, WildcardTypeName.subtypeOf(parameterizedTypeName.typeArguments.get(0)));
        } else { // if (typeArgumentQty == 2)
            wildType = ParameterizedTypeName.get(wildcardClass, WildcardTypeName.subtypeOf(parameterizedTypeName.typeArguments.get(0)), WildcardTypeName.subtypeOf(parameterizedTypeName.typeArguments.get(1)));
        }
        return Optional.of(new SingleItemsMetaData(collectionClass, parameterizedTypeName.typeArguments, wildType));
    }

    private boolean hasWildcardTypeArguments(ParameterizedTypeName parameterizedTypeName, int argumentCount) {
        for (int i = 0; i < argumentCount; ++i) {
            if (parameterizedTypeName.typeArguments.size() > i) {
                if (parameterizedTypeName.typeArguments.get(i) instanceof WildcardTypeName) {
                    return true;
                }
            }
        }
        return false;
    }

    private String disambiguateGeneratedMethodName(List<RecordClassType> recordComponents, String baseName, int index) {
        var name = (index == 0) ? baseName : (baseName + index);
        if (recordComponents.stream().anyMatch(component -> component.name().equals(name))) {
            return disambiguateGeneratedMethodName(recordComponents, baseName, index + 1);
        }
        return name;
    }

    private MethodSpec buildShimMethod(String name, TypeName mainType, ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        var code = CodeBlock.of("return (o != null) ? $T.copyOf(o) : $T.of()", mainType, mainType);
        return MethodSpec.methodBuilder(name)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariables(Arrays.asList(typeVariables))
                .returns(parameterizedType)
                .addParameter(parameterizedType, "o")
                .addStatement(code)
                .build();
    }

    private MethodSpec buildMutableMakerMethod(String name, TypeName mutableCollectionType, ParameterizedTypeName parameterizedType, TypeVariableName... typeVariables) {
        var nullCase = CodeBlock.of("if (o == null) return new $T<>()", mutableCollectionType);
        var isMutableCase = CodeBlock.of("if (o instanceof $T) return o", mutableCollectionType);
        var defaultCase = CodeBlock.of("return new $T(o)", mutableCollectionType);
        return MethodSpec.methodBuilder(name)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariables(Arrays.asList(typeVariables))
                .returns(parameterizedType)
                .addParameter(parameterizedType, "o")
                .addStatement(nullCase)
                .addStatement(isMutableCase)
                .addStatement(defaultCase)
                .build();
    }

    private MethodSpec buildCollectionsShimMethod() {
        var code = CodeBlock.builder()
                .add("if (o instanceof Set) {\n")
                .indent()
                .addStatement("return $T.copyOf(o)", setType)
                .unindent()
                .addStatement("}")
                .addStatement("return (o != null) ? $T.copyOf(o) : $T.of()", listType, listType)
                .build();
        return MethodSpec.methodBuilder(collectionShimName)
                .addAnnotation(generatedRecordBuilderAnnotation)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addTypeVariable(tType)
                .returns(parameterizedCollectionType)
                .addParameter(parameterizedCollectionType, "o")
                .addCode(code)
                .build();
    }
}
