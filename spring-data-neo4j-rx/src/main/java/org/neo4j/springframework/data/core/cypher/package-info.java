/**
 * Contains an internal DSL for creating Cypher. It is part of our internal API and may change without further notice.
 * <p/>
 * While we have extensive tests in place to check that queries SDN needs are correctly generated, the AST itself is not
 * validated for type errors. That is, one could misuse the DSL here to create an AST that renders to a syntactically
 * correct Cypher statement, that will explode during runtime. For example using wrongly typed expression (an expression
 * referencing a list while a map is needed or something like that).
 * <p/>
 * With this in mind, please use this DSL consciously if you find it useful. It won't go away anytime soon, but might
 * change in ways that break your code without further notice.
 *
 * @author Michael J. Simons
 */
@NonNullApi
package org.neo4j.springframework.data.core.cypher;

import org.springframework.lang.NonNullApi;
