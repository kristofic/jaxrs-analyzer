package com.sebastian_daschner.jaxrs_analyzer.analysis.javadoc;

import com.sebastian_daschner.jaxrs_analyzer.model.methods.MethodIdentifier;
import com.sebastian_daschner.jaxrs_analyzer.model.results.ClassResult;
import com.sebastian_daschner.jaxrs_analyzer.model.results.MethodResult;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sebastian Daschner
 */
public class JavaDocAnalyzer {

    private static final Map<MethodIdentifier, MethodDoc> METHOD_DOCS = new ConcurrentHashMap<>();
    private static final Map<String, ClassDoc> CLASS_DOCS = new ConcurrentHashMap<>();

    public void analyze(final Set<ClassResult> classResults, final Set<String> packages, final Set<Path> projectSourcePaths) {
        invokeDoclet(packages, projectSourcePaths);

        combineResults(classResults);
    }

    private void invokeDoclet(final Set<String> packages, final Set<Path> projectSourcePaths) {
        final String[] args = Stream.concat(
                Stream.of("-sourcepath",
                        projectSourcePaths.stream().map(Path::toString).collect(Collectors.joining(":")),
                        "-quiet",
                        "-doclet",
                        "com.sebastian_daschner.jaxrs_analyzer.analysis.javadoc.JAXRSDoclet"),
                packages.stream())
                .toArray(String[]::new);

        com.sun.tools.javadoc.Main.execute(args);
    }

    private void combineResults(final Set<ClassResult> classResults) {

        // TODO handle class docs as well

        METHOD_DOCS.entrySet().forEach(e -> classResults.stream()
                .map(c -> findMethodResult(e.getKey(), c))
                .filter(Objects::nonNull)
                .findAny()
                .ifPresent(m -> m.setMethodDoc(e.getValue())));
    }

    private MethodResult findMethodResult(final MethodIdentifier identifier, final ClassResult classResult) {
        if (classResult.getOriginalClass().equals(identifier.getContainingClass()))
            return classResult.getMethods().stream()
                    .filter(methodResult -> methodResult.getOriginalMethodSignature().equals(identifier.getSignature()))
                    .findAny().orElse(null);

        return classResult.getMethods().stream()
                .map(MethodResult::getSubResource)
                .filter(Objects::nonNull)
                .map(c -> findMethodResult(identifier, c))
                .filter(Objects::nonNull)
                .findAny().orElse(null);
    }

    public static void put(final MethodIdentifier identifier, final MethodDoc methodDoc) {
        METHOD_DOCS.put(identifier, methodDoc);
    }

    public static void put(final String className, final ClassDoc classDoc) {
        CLASS_DOCS.put(className, classDoc);
    }

    public static MethodDoc get(final MethodIdentifier identifier) {
        return METHOD_DOCS.get(identifier);
    }

    public static ClassDoc get(final String className) {
        return CLASS_DOCS.get(className);
    }

}
