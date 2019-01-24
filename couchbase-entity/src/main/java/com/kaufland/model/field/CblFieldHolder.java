package com.kaufland.model.field;

import com.kaufland.ElementMetaModel;
import com.kaufland.generation.TypeConversionMethodsGeneration;
import com.kaufland.util.TypeUtil;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.model.impl.DefaultJavaParameterizedType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import kaufland.com.coachbasebinderapi.Field;

public class CblFieldHolder extends CblBaseFieldHolder {

    private String subEntityPackage;

    private String subEntitySimpleName;

    private boolean subEntityIsTypeParam;

    private boolean isIterable;

    private String typeParamPackage;

    private String typeParamSimpleName;

    private CblDefaultHolder defaultHolder;

    public CblFieldHolder(Field field, Element fieldElement, JavaField metaField, CblDefaultHolder defaultHolder, ElementMetaModel metaModel) {
        super(field.value(), fieldElement, metaField);
        this.defaultHolder = defaultHolder;


        String typeName = metaField.getType().getCanonicalName();
        if (metaModel.isMapWrapper(typeName)) {
            subEntitySimpleName = metaField.getType().getSimpleName() + "Wrapper";
            subEntityPackage = metaField.getType().getPackageName();
        } else if (metaField.getType() instanceof DefaultJavaParameterizedType) {
            for (JavaType typeParameter : ((DefaultJavaParameterizedType) metaField.getType()).getActualTypeArguments()) {

                String canonicalName = typeParameter.getCanonicalName();
                isIterable = metaField.getType().isArray() || metaField.getType().isA(Iterable.class.getCanonicalName());
                if(isIterable){
                    JavaClass metaClazz = metaModel.getMetaFor(canonicalName);
                    typeParamSimpleName = metaClazz.getSimpleName();
                    typeParamPackage = metaClazz.getPackageName();
                }
                if (metaModel.isMapWrapper(canonicalName)) {
                    JavaClass metaClazz = metaModel.getMetaFor(canonicalName);
                    subEntitySimpleName = metaClazz.getSimpleName() + "Wrapper";
                    subEntityPackage = metaClazz.getPackageName();
                    subEntityIsTypeParam = true;
                    break;
                }
            }
        }
    }

    public String getSubEntitySimpleName() {
        return subEntitySimpleName;
    }

    public TypeName getSubEntityTypeName() {
        return ClassName.get(subEntityPackage, subEntitySimpleName);
    }

    public boolean isSubEntityIsTypeParam() {
        return subEntityIsTypeParam;
    }

    public boolean isIterable() {
        return isIterable;
    }

    public TypeName getTypeParamTypeName() {
        return ClassName.get(typeParamPackage, typeParamSimpleName);
    }

    public CblDefaultHolder getDefaultHolder() {
        return defaultHolder;
    }

    public boolean isTypeOfSubEntity() {
        return !StringUtils.isBlank(subEntitySimpleName);
    }

    @Override
    public MethodSpec getter(String dbName, boolean useMDocChanges) {
        TypeName returnType = TypeUtil.parseMetaType(getMetaField().getType(), getSubEntitySimpleName());

        MethodSpec.Builder builder = MethodSpec.methodBuilder("get" + WordUtils.capitalize(getMetaField().getName())).
                addModifiers(Modifier.PUBLIC).
                returns(returnType);


        if (isTypeOfSubEntity()) {
            returnType = TypeUtil.parseMetaType(getMetaField().getType(), getSubEntitySimpleName());
            TypeName castType = isSubEntityIsTypeParam() ? TypeUtil.createListWithMapStringObject() : TypeUtil.createMapStringObject();

            if (useMDocChanges) {
                builder.addCode(CodeBlock.builder().beginControlFlow("if(mDocChanges.containsKey($N))", getConstantName()).
                        addStatement("return ($T) $T.fromMap(($T)mDocChanges.get($N))", returnType, getSubEntityTypeName(), castType, getConstantName()).
                        endControlFlow().
                        build());
            }
            builder.addCode(CodeBlock.builder().beginControlFlow("if(mDoc.containsKey($N))", getConstantName()).
                    addStatement("return ($T) $T.fromMap(($T)mDoc.get($N))", returnType, getSubEntityTypeName(), castType, getConstantName()).
                    endControlFlow().
                    build());

            builder.addStatement("return null");
        } else {

            TypeName forTypeConversion = evaluateClazzForTypeConversion();
            if (useMDocChanges) {
                builder.addCode(CodeBlock.builder().beginControlFlow("if(mDocChanges.containsKey($N))", getConstantName()).
                        addStatement("return " + TypeConversionMethodsGeneration.READ_METHOD_NAME + "(mDocChanges.get($N), $T.class)", getConstantName(), forTypeConversion).
                        endControlFlow().
                        build());
            }

            builder.addCode(CodeBlock.builder().beginControlFlow("if(mDoc.containsKey($N))", getConstantName()).
                    addStatement("return " + TypeConversionMethodsGeneration.READ_METHOD_NAME + "(mDoc.get($N), $T.class)", getConstantName(), forTypeConversion).
                    endControlFlow().
                    build());

            if (defaultHolder != null) {
                builder.addStatement("return " + TypeConversionMethodsGeneration.READ_METHOD_NAME + "(mDocDefaults.get($N), $T.class)", getConstantName(), forTypeConversion);
            } else {
                builder.addStatement("return null");
            }

        }

        return builder.build();
    }


    @Override
    public MethodSpec setter(String dbName, TypeName entityTypeName, boolean useMDocChanges) {
        TypeName fieldType = TypeUtil.parseMetaType(getMetaField().getType(), getSubEntitySimpleName());
        MethodSpec.Builder builder = MethodSpec.methodBuilder("set" + WordUtils.capitalize(getMetaField().getName())).
                addModifiers(Modifier.PUBLIC).
                addParameter(fieldType, "value").
                returns(entityTypeName);

        String docName = useMDocChanges ? "mDocChanges" : "mDoc";

        if (isTypeOfSubEntity()) {
            builder.addStatement("$N.put($N, $T.toMap(($T)value))", docName, getConstantName(), getSubEntityTypeName(), fieldType);
            builder.addStatement("return this");
        } else {
            TypeName forTypeConversion = evaluateClazzForTypeConversion();
            builder.addStatement("$N.put($N, "+ TypeConversionMethodsGeneration.WRITE_METHOD_NAME +"(value, $T.class))", docName, getConstantName(), forTypeConversion);
            builder.addStatement("return this");
        }

        return builder.build();
    }

    @Override
    public List<FieldSpec> createFieldConstant() {

        FieldSpec fieldAccessorConstant = FieldSpec.builder(String.class, getConstantName(), Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC).
                initializer("$S", getDbField()).
                build();

        return Collections.singletonList(fieldAccessorConstant);
    }

    private TypeName evaluateClazzForTypeConversion(){
        if(isIterable && getTypeParamTypeName() != null){
            return getTypeParamTypeName();
        }

       return TypeUtil.parseMetaType(getMetaField().getType(), getSubEntitySimpleName());
    }
}
