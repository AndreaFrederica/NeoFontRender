package neofontrender.addons.compat;

/** Detects mods by checking for their class files without loading classes. */
public final class ClassPresenceChecker {
    private ClassPresenceChecker() {}

    public static boolean isPresent(String binaryClassName) {
        if (binaryClassName == null || binaryClassName.isEmpty()) return false;
        String resource = binaryClassName.replace('.', '/').concat(".class");
        return ClassPresenceChecker.class.getClassLoader().getResource(resource) != null;
    }
}
