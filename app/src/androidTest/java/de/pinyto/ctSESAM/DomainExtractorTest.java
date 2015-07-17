package de.pinyto.ctSESAM;

import junit.framework.TestCase;

/**
 * Testing domain extraction
 */
public class DomainExtractorTest extends TestCase {

    public void testExtract () {
        assertEquals("test.com",
                DomainExtractor.extract("http://www.test.com/some/path/index.html"));
        assertEquals("test.com",
                DomainExtractor.extract("http://test.com/some/path/index.html"));
        assertEquals("test.com",
                DomainExtractor.extract("http://complicated.subdomain.structure.test.com/some/path/index.html"));
        assertEquals("test.com",
                DomainExtractor.extract("https://www.test.com/some/path/index.html"));
        assertEquals("test.com",
                DomainExtractor.extract("https://test.com/some/path/index.html"));
        assertEquals("test.com",
                DomainExtractor.extract("https://complicated.subdomain.structure.test.com/some/path/index.html"));
        assertEquals("test.com",
                DomainExtractor.extract("test.com"));
        assertEquals("test.com",
                DomainExtractor.extract("www.test.com"));
        assertEquals("test.com",
                DomainExtractor.extract("complicated.subdomain.structure.test.com"));
        assertEquals("test.com",
                DomainExtractor.extract("test.com/path/to/things"));
        assertEquals("amazon.co.jp",
                DomainExtractor.extract("www.amazon.co.jp/search=?some(characters)[strange]"));
        assertEquals("english.co.uk",
                DomainExtractor.extract("english.co.uk"));
        assertEquals("noUrl",
                DomainExtractor.extract("noUrl"));
    }

}
