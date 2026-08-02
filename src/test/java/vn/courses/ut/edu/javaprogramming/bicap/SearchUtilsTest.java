package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.common.util.SearchUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchUtilsTest {

    @Test
    void escapeLike_shouldEscapeWildcards() {
        assertEquals("!%", SearchUtils.escapeLike("%"));
        assertEquals("!_", SearchUtils.escapeLike("_"));
        assertEquals("!!", SearchUtils.escapeLike("!"));
        assertEquals("sầu!%riêng", SearchUtils.escapeLike("sầu%riêng"));
    }

    @Test
    void escapeLike_shouldKeepPlainTextUntouched() {
        assertEquals("Trang Trại Xanh", SearchUtils.escapeLike("Trang Trại Xanh"));
        assertEquals("", SearchUtils.escapeLike(""));
    }

    @Test
    void escapeLike_shouldHandleNull() {
        assertNull(SearchUtils.escapeLike(null));
    }
}
