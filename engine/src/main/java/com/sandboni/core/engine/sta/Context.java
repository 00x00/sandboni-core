package com.sandboni.core.engine.sta;

import com.sandboni.core.engine.sta.graph.Link;
import com.sandboni.core.engine.sta.graph.LinkType;
import com.sandboni.core.engine.utils.StringUtil;
import com.sandboni.core.scm.scope.Change;
import com.sandboni.core.scm.scope.ChangeScope;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class Context {
    private static final Logger log = LoggerFactory.getLogger(Context.class);
    private static final String CLASSPATH_PROPERTY_NAME = "java.class.path";
    private static final String DEFAULT_APPLICATION_ID = "sandboni.default.AppId";
    private static final String ALWAYS_RUN_ANNOTATION = "AlwaysRun";

    private final Set<String> filters;
    private final ConcurrentHashMap<Link, Boolean> links;
    private final String currentLocation;
    private final ChangeScope<Change> changeScope;
    private final Collection<String> srcLocations;
    private final Collection<String> testLocations;
    private final Collection<String> dependencyJars;
    private final String classPath;
    private final String applicationId;
    private final String alwaysRunAnnotation;
    private final String seloniFilepath;
    private final boolean enablePreview;
    private final ConcurrentHashMap<LinkType, Boolean> adoptedLinkTypes;

    public boolean inScope(String actor) {
        return filters.isEmpty() || (actor != null && filters.stream().anyMatch(actor::contains));
    }

    public ChangeScope<Change> getChangeScope() {
        return changeScope;
    }

    // Visible for testing only
    public Context(String[] srcLocation, String[] testLocation, String filter, ChangeScope<Change> changes, String seloniFilepath) {
        this(DEFAULT_APPLICATION_ID, srcLocation, testLocation, new String[0], filter, changes, null, seloniFilepath, true);
    }

    @SuppressWarnings("squid:S00107")
    public Context(String applicationId, String[] srcLocation, String[] testLocation, String[] dependencies,
                   String filter, ChangeScope<Change> changes, String includeTestAnnotation, String seloniFilepath,
                   boolean enablePreview) {
        this.links = new ConcurrentHashMap<>();
        this.adoptedLinkTypes = new ConcurrentHashMap<>();
        this.applicationId = applicationId == null ? DEFAULT_APPLICATION_ID : applicationId;
        this.srcLocations = getCollection(srcLocation);
        this.testLocations = getCollection(testLocation);
        this.dependencyJars = getCollection(dependencies);
        this.currentLocation = "";
        this.classPath = getExecutionClasspath(srcLocations, testLocations, getCollection(dependencies));

        this.filters = getFilters(filter);
        this.changeScope = changes;
        this.alwaysRunAnnotation = StringUtil.isEmptyOrNull(includeTestAnnotation) ? ALWAYS_RUN_ANNOTATION : includeTestAnnotation;
        this.seloniFilepath = seloniFilepath;
        this.enablePreview = enablePreview;
    }

    private Set<String> getFilters(String filter) {
        if (filter == null) {
            return Collections.emptySet();
        }
        List<String> tokens = Arrays.asList(filter.split(","));
        Set<String> result = new HashSet<>(tokens);
        result.addAll(tokens.stream().map(s -> s.replace(".", File.separator).trim()).collect(Collectors.toSet()));
        return result;
    }

    private Collection<String> getCollection(String[] array) {
        return array == null ? Collections.emptySet() :
                Arrays.stream(array).map(l -> new File(l).getAbsolutePath()).collect(Collectors.toSet());
    }

    private String getExecutionClasspath(Collection<String> srcLocation, Collection<String> testLocation, Collection<String> dependencies) {
        String currentJavaClasspath = System.getProperty(CLASSPATH_PROPERTY_NAME, "");
        log.debug("Current java.class.path is: {}", currentJavaClasspath);

        Set<String> projectClasspath = new HashSet<>(Arrays.asList(currentJavaClasspath.split(File.pathSeparator)));
        projectClasspath.addAll(srcLocation);
        projectClasspath.addAll(testLocation);
        projectClasspath.addAll(dependencies);

        String updatedClassPath = String.join(File.pathSeparator, projectClasspath);
        log.debug("Execution java.class.path is: {}", updatedClassPath);

        return updatedClassPath;
    }

    private Context(Context source) {
        this(source, source.currentLocation);
    }

    private Context(Context source, String currentLocation) {
        this.links = new ConcurrentHashMap<>();
        this.adoptedLinkTypes = new ConcurrentHashMap<>();
        this.applicationId = source.applicationId;
        this.srcLocations = Collections.unmodifiableCollection(source.srcLocations);
        this.testLocations = Collections.unmodifiableCollection(source.testLocations);
        this.dependencyJars = Collections.unmodifiableCollection(source.dependencyJars);
        this.classPath = source.classPath;
        this.filters = Collections.unmodifiableSet(source.filters);
        this.changeScope = source.changeScope;
        this.currentLocation = currentLocation;
        this.alwaysRunAnnotation = source.alwaysRunAnnotation;
        this.seloniFilepath = source.seloniFilepath;
        this.enablePreview = source.enablePreview;
    }

    public Context getLocalContext() {
        return new Context(this);
    }

    public Context getLocalContext(String currentLocation) {
        return new Context(this, currentLocation);
    }

    public String getClassPath() {
        return classPath;
    }

    public Stream<Link> getLinks() {
        return Collections.unmodifiableSet(links.keySet()).parallelStream();
    }

    private int adoptLink(Link link) {
        adoptedLinkTypes.put(link.getLinkType(), Boolean.TRUE);
        return links.put(link, Boolean.TRUE) == null ? 1 : 0;
    }

    public int addLink(Link link) {
        return adoptLink(link);
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void addLinks(Link... linksToAdd) {
        for (Link link : linksToAdd) {
            addLink(link);
        }
    }

    public void addLinks(Stream<Link> linksToAdd) {
        linksToAdd.forEach(this::addLink);
    }

    public boolean isAdoptedLinkType(LinkType... linkTypes) {
        return Arrays.stream(linkTypes).allMatch(adoptedLinkTypes::containsKey);
    }
}