package com.sandboni.core.engine.finder.bcel;

import com.sandboni.core.engine.Application;
import com.sandboni.core.engine.FinderTestBase;
import com.sandboni.core.engine.contract.ChangeDetector;
import com.sandboni.core.engine.contract.Finder;
import com.sandboni.core.engine.finder.bcel.visitors.TestClassVisitor;
import com.sandboni.core.engine.sta.graph.Link;
import com.sandboni.core.engine.sta.graph.LinkType;
import com.sandboni.core.engine.sta.graph.vertex.TestVertex;
import com.sandboni.core.scm.scope.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.sandboni.core.engine.MockChangeDetector.PACKAGE_NAME;
import static com.sandboni.core.engine.sta.graph.vertex.VertexInitTypes.START_VERTEX;

public class BcelFinderSuiteTest extends FinderTestBase {

    private static final Logger log = LoggerFactory.getLogger(BcelFinderSuiteTest.class);

    @Before
    public void setUp() {
        super.initializeContext();
    }

    private void testVisitor(Link[] expectedLinks, ClassVisitor... visitors) {
        Finder f = new BcelFinder(visitors);
        f.findSafe(context);
        log.info(String.format("Context SrcLocations: %s;TestLocations: %s ", context.getSrcLocations(), context.getTestLocations()));
        log.info(String.format("this.location: %s", this.location));
        context.getLinks().forEach(l -> log.info(String.format("Link: %s; caller isSpecial: %s, callee isSpecial: %s", l.toString(), l.getCaller().isSpecial(), l.getCallee().isSpecial())));
        assertLinksExist(expectedLinks);
    }

    private void testTestClassVisitor(Link... expectedLinks) {
        testVisitor(expectedLinks, new TestClassVisitor());
    }

    @Test
    public void testTestSuiteIsDetected() {
        TestVertex tv1 = new TestVertex.Builder(PACKAGE_NAME + ".SuiteTestClass1", "print()", null).build();
        Link expectedLink1 = newLink(START_VERTEX, tv1, LinkType.ENTRY_POINT);
        TestVertex tv2 = new TestVertex.Builder(PACKAGE_NAME + ".SuiteTestClass2", "print()", null).build();
        Link expectedLink2 = newLink(START_VERTEX, tv2, LinkType.ENTRY_POINT);
        TestVertex tv3 = new TestVertex.Builder(PACKAGE_NAME + ".SuiteTestClass3", "print()", null).build();
        Link expectedLink3 = newLink(START_VERTEX, tv3, LinkType.ENTRY_POINT);

        testTestClassVisitor(expectedLink1, expectedLink2, expectedLink3);
    }

}
