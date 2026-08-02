package vn.courses.ut.edu.javaprogramming.bicap.common.util;

/**
 * Escapes SQL/JPQL LIKE metacharacters so free-text search treats user input literally.
 * Uses {@code !} as the escape character — repository queries must declare {@code ESCAPE '!'}.
 */
public final class SearchUtils {

    private SearchUtils() {}

    /**
     * Escapes {@code !}, {@code %} and {@code _} in the search term.
     * Returns the term unchanged when null.
     */
    public static String escapeLike(String term) {
        if (term == null) {
            return null;
        }
        return term
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
