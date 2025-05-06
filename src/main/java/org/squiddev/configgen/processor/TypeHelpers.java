package org.squiddev.configgen.processor;

import java.lang.reflect.Array;
import java.util.List;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Helpers for type
 */
public final class TypeHelpers {

    public enum Type {

        ARRAY,
        GENERIC_ARRAY,
        BOOLEAN,
        DOUBLE,
        INT,
        STRING,
        UNKNOWN;

        public boolean isArray() {
            return this == ARRAY || this == GENERIC_ARRAY;
        }
    }

    public interface IType {

        /**
         * Get the {@link Type} for this type
         *
         * @return The simple type
         */
        Type getType();

        /**
         * Get the name used to read it from a {@link net.minecraftforge.common.config.Configuration}
         *
         * @return The function name used to read
         */
        String accessName();

        /**
         * Extract a value for this type.
         *
         * Does not perform type checking.
         *
         * @param value The value to extract from
         * @param def   The default value
         * @return The extracted value or the default
         */
        Object extractValue(Object value, Object def);

        /**
         * Get the class for this type
         *
         * @return The appropriate class
         */
        TypeMirror getMirror();

        /**
         * Get the component type
         *
         * @return The component type
         */
        IType getComponentType();

        /**
         * Initialise the type through a constructor. Only applies to {@link Type#GENERIC_ARRAY}.
         *
         * @return If the type should be inialised through a constructor.
         */
        boolean throughConstructor();

        /**
         * Get the default value
         *
         * @return The default value for this class
         */
        Object getDefault();
    }

    private static final class ArrayPropertyType implements IType {

        private final IType component;
        private final TypeMirror mirror;

        public ArrayPropertyType(ArrayType mirror, Types types) {
            this.component = TypeHelpers.getType(mirror.getComponentType(), types);
            this.mirror = mirror;
        }

        @Override
        public Type getType() {
            return Type.ARRAY;
        }

        @Override
        public String accessName() {
            return component.accessName() + "List";
        }

        @Override
        public Object extractValue(Object value, Object def) {
            if (value == null) {
                return def;
            } else if (value.getClass()
                .isArray()) {
                    return value;
                } else {
                    return new Object[] { value };
                }
        }

        @Override
        public TypeMirror getMirror() {
            return mirror;
        }

        @Override
        public Object getDefault() {
            return Array.newInstance(
                component.getDefault()
                    .getClass(),
                0);
        }

        @Override
        public IType getComponentType() {
            return component;
        }

        @Override
        public boolean throughConstructor() {
            return false;
        }

        @Override
        public String toString() {
            return "ArrayPropertyType{" + "component=" + component + ", mirror=" + mirror + '}';
        }
    }

    private static final class BasicType implements IType {

        private final Type type;
        private final TypeMirror mirror;

        public BasicType(Type type, TypeMirror mirror) {
            this.type = type;
            this.mirror = mirror;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String accessName() {
            String name = type.toString();
            return name.substring(0, 1)
                .toUpperCase()
                + name.substring(1)
                    .toLowerCase();
        }

        @Override
        public Object extractValue(Object value, Object def) {
            if (value == null) {
                return def;
            } else if (value.getClass()
                .isArray()) {
                    return Array.getLength(value) > 0 && Array.get(value, 0) != null ? Array.get(value, 0) : def;
                } else {
                    return value;
                }
        }

        @Override
        public TypeMirror getMirror() {
            return mirror;
        }

        @Override
        public IType getComponentType() {
            return this;
        }

        @Override
        public boolean throughConstructor() {
            return false;
        }

        @Override
        public String toString() {
            return type.toString()
                .toLowerCase();
        }

        @Override
        public Object getDefault() {
            switch (type) {
                case BOOLEAN:
                    return false;
                case INT:
                    return 0;
                case DOUBLE:
                    return 0.0;
                case STRING:
                    return "";
                default:
                    return null;
            }
        }
    }

    private static final class GenericArray implements IType {

        private final TypeMirror mirror;
        private final IType child;
        private final boolean ctor;

        private GenericArray(DeclaredType mirror, Types types) {
            this.mirror = mirror;
            switch (mirror.getTypeArguments()
                .size()) {
                case 0: {
                    ctor = true;
                    TypeElement element = (TypeElement) mirror.asElement();
                    IType child = new BasicType(Type.UNKNOWN, null);
                    for (Element childElement : element.getEnclosedElements()) {
                        if (childElement.getKind() == ElementKind.CONSTRUCTOR) {
                            ExecutableElement method = (ExecutableElement) childElement;
                            List<? extends VariableElement> elem = method.getParameters();

                            if (elem.size() == 1) {
                                TypeMirror paramMirror = elem.get(0)
                                    .asType();
                                if (paramMirror.getKind() == TypeKind.ARRAY) {
                                    child = TypeHelpers.getType(((ArrayType) paramMirror).getComponentType(), types);
                                    break;
                                }
                            }
                        }
                    }

                    this.child = child;
                    break;
                }
                case 1:
                    ctor = false;
                    child = TypeHelpers.getType(
                        mirror.getTypeArguments()
                            .get(0),
                        types);
                    break;
                default:
                    ctor = false;
                    child = new BasicType(Type.UNKNOWN, null);
            }
        }

        @Override
        public Type getType() {
            return Type.GENERIC_ARRAY;
        }

        @Override
        public String accessName() {
            return child.accessName() + "List";
        }

        @Override
        public Object extractValue(Object value, Object def) {
            if (value == null) {
                return def;
            } else if (value.getClass()
                .isArray()) {
                    return value;
                } else {
                    return new Object[] { value };
                }
        }

        @Override
        public TypeMirror getMirror() {
            return mirror;
        }

        @Override
        public IType getComponentType() {
            return child;
        }

        @Override
        public boolean throughConstructor() {
            return ctor;
        }

        @Override
        public Object getDefault() {
            return Array.newInstance(
                child.getDefault()
                    .getClass(),
                0);
        }

        @Override
        public String toString() {
            return "GenericArray{" + "mirror=" + mirror + ", child=" + child + '}';
        }
    }

    public static IType getType(TypeMirror mirror, Types types) {
        switch (mirror.getKind()) {
            case BOOLEAN:
                return new BasicType(Type.BOOLEAN, mirror);
            case DOUBLE:
                return new BasicType(Type.DOUBLE, mirror);
            case INT:
                return new BasicType(Type.INT, mirror);
            case ARRAY:
                return new ArrayPropertyType((ArrayType) mirror, types);
            case DECLARED: {
                DeclaredType declared = (DeclaredType) mirror;
                TypeElement element = (TypeElement) declared.asElement();
                String name = element.getQualifiedName()
                    .toString();
                if (name.equals("java.lang.String")) {
                    return new BasicType(Type.STRING, mirror);
                } else if (name.equals("java.lang.Integer")) {
                    return new BasicType(Type.INT, types.getPrimitiveType(TypeKind.INT));
                } else if (name.equals("java.lang.Boolean")) {
                    return new BasicType(Type.BOOLEAN, types.getPrimitiveType(TypeKind.BOOLEAN));
                } else if (name.equals("java.lang.Double")) {
                    return new BasicType(Type.DOUBLE, types.getPrimitiveType(TypeKind.DOUBLE));
                } else {
                    return new GenericArray(declared, types);
                }
            }
            default:
                return new BasicType(Type.UNKNOWN, mirror);
        }
    }

    public static String validateType(IType type) {
        if (type.getType() == Type.UNKNOWN) {
            return "Unknown type " + type;
        } else if (type.getType()
            .isArray()) {
                IType component = type.getComponentType();

                if (component.getType()
                    .isArray()) {
                    return "Nested arrays are not allowed";
                } else if (component.getType() == Type.UNKNOWN) {
                    return "Unknown type " + type;
                }
            }

        return null;
    }

    public static boolean isType(Class<?> klass, IType type) {
        if (klass == Integer.class || klass == int.class) {
            return type.getType() == Type.INT;
        } else if (klass == Double.class || klass == double.class) {
            return type.getType() == Type.DOUBLE;
        } else if (klass == Boolean.class || klass == boolean.class) {
            return type.getType() == Type.BOOLEAN;
        } else if (klass == String.class) {
            return type.getType() == Type.STRING;
        } else if (klass.isArray()) {
            return (type.getType() == Type.ARRAY || type.getType() == Type.GENERIC_ARRAY)
                && isType(klass.getComponentType(), type.getComponentType());
        } else {
            throw new IllegalStateException("Unknown type " + klass);
        }
    }
}
