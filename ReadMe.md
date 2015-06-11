# ConfigGen
Simple configs for Forge.

The most simple config class looks like this:
```java
@Config(languagePrefix = "gui.config.primitive:")
public class PrimitiveConfig {
	/**
	 * Javadoc is used to represent documentation
	 * in the config file as well!
	 */
	public static class Section {
		/**
		 * Specify constraints and default values
		 */
		@Range(min = 0, max = 10)
		@DefaultInt(3)
		public static int thing;

		/**
		 * Arrays too!
		 */
		@Range(min = 0, max = 10)
		@DefaultDouble({3, 2, 3})
		public static double[] things;

		/**
		 * Set world and mc level restarts
		 */
		@RequiresRestart
		public static class SubThing {
			@DefaultString({"1", "2", "3"})
			@RequiresRestart
			public static String[] bars;

			/**
			 * Exclude any field or class - finals are automagically excluded.
			 */
			@Exclude
			public static double ignore;

			public static final double IGNORE = 0;
		}
	}

	/**
	 * Use a method called 'sync' to work with custom properties
	 * For instance - calculate a HashMap
	 */
	public static void sync() {
		System.out.println("Syncing ...");
	}
}
```

## Usage
Add this to your `build.gradle` (or equivalent)
```groovy
repositories {
	mavenCentral()

	maven {
		name = "squiddev"
		url = "http://maven.bonzodandd.co.uk"
	}
}

dependencies {
	compile 'org.squiddev:ConfigGen:1.0-SNAPSHOT'
}
```

Then annotate your classes with `@Config`. To load and sync simply do:

```java
PrimitiveConfigLoader.init(new File("config.cfg"));

// Change things
PrimitiveConfigLoader.sync()
```

## Problems
 - The use of `@DefaultWhatever(...)` is obviously inconvenient. I'm looking at a way of using `com.sun.tools` to get the default
   value, but until then the default annotations will have to stay.
 - Class generation does mean you will get errors in the IDE when writing your code. Sorry.
