/**
 * General purpose functions.
 * <p>
 * Function implementations come here.
 *
 *
 * <h3>Custom Functions</h3>
 * <p>
 * It is possible to define custom functions on the host code and invoke them from  within the template files.
 * Custom functions must implement the {@link io.github.erdos.stencil.functions.Function} interface and be registered
 * to the template evaluator engine.
 * <p>
 * Functions are found by their <b>case-insensitive</b> name so make sure not to override any existing function implementation.
 * <p>
 * Functions on <b>null</b> arguments should return null.
 * <p>
 * Functions are <b>variadic</b>, i.e. they accept a variable number of arguments. Handling the correct number and type of
 * arguments is the responsibility of the functions implementation.
 * <p>
 * Make your functions as <b>flexible</b> as possible by considering proper type and error handling.
 */
package io.github.erdos.stencil.functions;