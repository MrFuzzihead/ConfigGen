package org.squiddev.configgen.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Array;

/**
 * Helpers for type
 */
public final class TypeHelpers {
	public enum Type {
		ARRAY(new Object[0]),
		BOOLEAN(false),
		DOUBLE(0),
		INT(0),
		STRING(""),
		UNKNOWN(null);

		private final Object def;

		Type(Object def) {
			this.def = def;
		}

		/**
		 * Get the default value
		 *
		 * @return The default value for this class
		 */
		public Object getDefault() {
			return def;
		}
	}

	public interface IType {
		/**
		 * Get the {@link Type} for this type
		 */
		Type getType();

		/**
		 * Get the name used to read it from a {@link net.minecraftforge.common.config.Configuration}
		 */
		String accessName();

		/**
		 * Extract a value for this type.
		 *
		 * Does not perform type checking.
		 *
		 * @param value The value to extract from
		 * @return The extracted value or the default
		 */
		Object extractValue(Object value, Object def);

		/**
		 * Get the class for this type
		 *
		 * @return The appropriate class
		 */
		Class<?> getTypeClass();
	}

	public final static class ArrayType implements IType {
		private final IType component;

		public ArrayType(IType component) {
			this.component = component;
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
			} else if (value.getClass().isArray()) {
				return value;
			} else {
				return new Object[]{value};
			}
		}

		@Override
		public Class<?> getTypeClass() {
			return Array.newInstance(component.getTypeClass(), 0).getClass();
		}

		public IType getComponentType() {
			return component;
		}

		@Override
		public String toString() {
			return component.toString() + "[]";
		}
	}

	private final static class BasicType implements IType {
		private final Type type;

		public BasicType(Type type) {
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public String accessName() {
			String name = type.toString();
			return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
		}

		@Override
		public Object extractValue(Object value, Object def) {
			if (value == null) {
				return def;
			} else if (value.getClass().isArray()) {
				return Array.getLength(value) > 0 && Array.get(value, 0) != null ? Array.get(value, 0) : def;
			} else {
				return value;
			}
		}

		@Override
		public Class<?> getTypeClass() {
			switch (type) {
				case BOOLEAN:
					return boolean.class;
				case INT:
					return int.class;
				case DOUBLE:
					return double.class;
				case STRING:
					return String.class;
				default:
					return null;
			}
		}

		@Override
		public String toString() {
			return type.toString().toLowerCase();
		}
	}

	private final static class UnknownType implements IType {
		private final Class<?> klass;

		public UnknownType(Class<?> klass) {
			this.klass = klass;
		}

		@Override
		public Type getType() {
			return Type.UNKNOWN;
		}

		@Override
		public String accessName() {
			return null;
		}

		@Override
		public Object extractValue(Object value, Object def) {
			return null;
		}

		@Override
		public Class<?> getTypeClass() {
			return null;
		}

		@Override
		public String toString() {
			return klass.toString();
		}
	}

	public static IType getType(TypeMirror mirror) {
		switch (mirror.getKind()) {
			case BOOLEAN:
				return new BasicType(Type.BOOLEAN);
			case DOUBLE:
				return new BasicType(Type.DOUBLE);
			case INT:
				return new BasicType(Type.INT);
			case ARRAY:
				return new ArrayType(getType(((javax.lang.model.type.ArrayType) mirror).getComponentType()));
			case DECLARED:
				if (((TypeElement) ((DeclaredType) mirror).asElement()).getQualifiedName().toString().equals("java.lang.String")) {
					return new BasicType(Type.STRING);
				}
			default:
				return new BasicType(Type.UNKNOWN);
		}
	}

	public static String validateType(IType type) {
		if (type.getType() == Type.UNKNOWN) {
			return "Unknown type " + type;
		} else if (type.getType() == Type.ARRAY) {
			IType component = ((ArrayType) type).getComponentType();

			if (component.getType() == Type.ARRAY) {
				return "Nested arrays are not allowed";
			} else if (component.getType() == Type.UNKNOWN) {
				return "Unknown type " + type;
			}
		}

		return null;
	}

	public static boolean instanceOf(Class<?> a, Object b) {
		if (a.isInstance(b)) return true;

		if (a == Integer.class || a == int.class) {
			return b.getClass() == Integer.class || b.getClass() == int.class;
		} else if (a == Double.class || a == double.class) {
			return b.getClass() == Double.class || b.getClass() == double.class;
		} else if (a == Boolean.class || a == boolean.class) {
			return b.getClass() == Boolean.class || b.getClass() == boolean.class;
		}

		return false;
	}
}
