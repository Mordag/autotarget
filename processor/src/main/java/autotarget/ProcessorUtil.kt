package autotarget

import autotarget.annotation.TargetParameterItem
import com.squareup.javapoet.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeMirror

object ProcessorUtil {

    private fun getValueType(element: TargetParameterItem): TypeMirror? {
        try {
            element.type
        } catch (mte: MirroredTypeException) {
            return mte.typeMirror
        }
        return null
    }

    private fun isParcelableObject(processingEnv: ProcessingEnvironment, typeMirror: TypeMirror?): Boolean {
        return processingEnv.typeUtils.isAssignable(typeMirror,
                processingEnv.elementUtils.getTypeElement(classParcelable).asType())
    }

    fun populateParamListBody(processingEnv: ProcessingEnvironment, list: ArrayList<TargetParameterItem>, builder: MethodSpec.Builder): Int {
        var paramCount = 0

        list.forEach {
            var valueName = it.name
            val valueKey = it.key
            val typeMirror = getValueType(it)
            val valueType = ClassName.get(typeMirror)

            if (valueName == "unspecified") {
                valueName = "param$paramCount"
                paramCount++
            }

            val parameterBuilder = ParameterSpec.builder(valueType, valueName)
            if (typeMirror !is PrimitiveType) {
                parameterBuilder.addAnnotation(classNullable)
            }

            builder.addParameter(parameterBuilder.build())

            when {
                valueType == classBundle -> {
                    builder.addStatement("parameterList.add(new $classBundleParameterProvider(\"$valueKey\", $valueName))")
                }
                isParcelableObject(processingEnv, typeMirror) -> {
                    builder.addStatement("parameterList.add(new $classParcelableParameterProvider(\"$valueKey\", $valueName))")
                }
                else -> {
                    builder.addStatement("parameterList.add(new $classValueParameterProvider(\"$valueKey\", $valueName))")
                }
            }
        }

        return paramCount
    }

    fun populateBundleModel(processingEnv: ProcessingEnvironment, list: ArrayList<TargetParameterItem>, builder: TypeSpec.Builder): Int {
        var paramCount = 0

        val constructorBuilder = MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(classBundle, "bundle")
                        .addAnnotation(classNullable)
                        .build()
                )
                .addStatement("if(bundle == null) return")
                .addCode("\n")

        list.forEach {
            var valueName = it.name
            val typeMirror = getValueType(it)
            val valueType = ClassName.get(typeMirror)

            if (valueName == "unspecified") {
                valueName = "param$paramCount"
                paramCount++
            }

            val fieldBuilder = FieldSpec.builder(valueType, valueName, Modifier.PRIVATE)
            builder.addField(fieldBuilder.build())

            when {
                valueType == classBundle -> constructorBuilder.addStatement("$valueName = bundle.getBundle(\"${it.key}\")")
                isParcelableObject(processingEnv, typeMirror) -> constructorBuilder.addStatement("$valueName = bundle.getParcelable(\"${it.key}\")")
                valueType == ClassName.INT -> constructorBuilder.addStatement("$valueName = bundle.getInt(\"${it.key}\")")
                valueType == ClassName.CHAR -> constructorBuilder.addStatement("$valueName = bundle.getChar(\"${it.key}\")")
                valueType == ClassName.BYTE -> constructorBuilder.addStatement("$valueName = bundle.getByte(\"${it.key}\")")
                valueType == ClassName.BOOLEAN -> constructorBuilder.addStatement("$valueName = bundle.getBoolean(\"${it.key}\")")
                valueType == ClassName.LONG -> constructorBuilder.addStatement("$valueName = bundle.getLong(\"${it.key}\")")
                valueType == ClassName.DOUBLE -> constructorBuilder.addStatement("$valueName = bundle.getDouble(\"${it.key}\")")
                valueType == ClassName.FLOAT -> constructorBuilder.addStatement("$valueName = bundle.getFloat(\"${it.key}\")")
                valueType == classString -> constructorBuilder.addStatement("$valueName = bundle.getString(\"${it.key}\")")
                else -> constructorBuilder.addStatement("$valueName = ($valueType) bundle.getSerializable(\"${it.key}\")")
            }

            val valueGetter = MethodSpec.methodBuilder("get${valueName.substring(0, 1).toUpperCase() + valueName.substring(1)}")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(valueType)
                    .addStatement("return $valueName")

            if (typeMirror !is PrimitiveType) valueGetter.addAnnotation(classNullable)
            builder.addMethod(valueGetter.build())
        }

        builder.addMethod(constructorBuilder.build())

        return paramCount
    }

    val libraryServicePackageName = "autotarget.service"
    val classActivityTarget: ClassName = ClassName.get(libraryServicePackageName, "ActivityTarget")
    val classFragmentTarget: ClassName = ClassName.get(libraryServicePackageName, "FragmentTarget")
    val classParameterProvider: ClassName = ClassName.get(libraryServicePackageName, "ParameterProvider")
    val classBundleParameterProvider: ClassName = ClassName.get(libraryServicePackageName, "BundleParameterProvider")
    val classParcelableParameterProvider: ClassName = ClassName.get(libraryServicePackageName, "ParcelableParameterProvider")
    val classValueParameterProvider: ClassName = ClassName.get(libraryServicePackageName, "ValueParameterProvider")

    val classNullable: ClassName = ClassName.get("androidx.annotation", "Nullable")
    val classNonNull: ClassName = ClassName.get("androidx.annotation", "NonNull")

    val classBundle: ClassName = ClassName.get("android.os", "Bundle")
    val classParcelable = "android.os.Parcelable"

    val classString: ClassName = ClassName.get("java.lang", "String")
    val classList: ClassName = ClassName.get("java.util", "List")
    val classArrayList: ClassName = ClassName.get("java.util", "ArrayList")

    val defaultGroupName = "default"
}