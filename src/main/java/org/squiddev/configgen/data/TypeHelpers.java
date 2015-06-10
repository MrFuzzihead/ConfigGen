package org.squiddev.configgen.data;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * Helpers for type
 */
public final class TypeHelpers {
	public enum Type {
		ARRAY,
		BOOLEAN,
		DOUBLE,
		INT,
		STRING,
		UNKNOWN,
	}

	public interface IType {
		Type getType();
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

		public IType getComponentType() {
			return component;
		}

		@Override
		public String toString() {
			return component.toString() + "[]";
		}
	}

	public final static class BasicType implements IType {
		private final Type type;

		public BasicType(Type type) {
			this.type = type;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public String toString() {
			return type.toString().toLowerCase();
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
}
