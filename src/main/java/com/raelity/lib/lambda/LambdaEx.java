/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package com.raelity.lib.lambda;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Support for dealing with exceptions in lambdas.
 * <p>
 * Reference: <a href="https://www.baeldung.com/java-lambda-exceptions">
 * Exceptions in Java 8 Lambda Expressions</a>
 * <p>
 * And consider:
 * https://4comprehension.com/sneakily-throwing-exceptions-in-lambda-expressions-in-java/
 * <br>
 * https://github.com/pivovarit/throwing-function
 */
public class LambdaEx
{
// and consider https://4comprehension.com/sneakily-throwing-exceptions-in-lambda-expressions-in-java/

/**
 * It is recommended that a {@link FunctionalInterface} use
 * a {@code FunctionalExWrapper} to rethrow exceptions encountered
 * so the case is easily detected.
 */
@SuppressWarnings("serial")
public static class FunctionalExWrapper extends RuntimeException
{
public FunctionalExWrapper(Throwable thrwbl)
{
    super(thrwbl);
}
    }

/**
 * Like a {@link Function}, but throws an exception
 * @param <T> function arg type
 * @param <R> function return type
 * @param <E> exception
 */
@FunctionalInterface
public interface ThrowingFunction<T,R,E extends Exception>
{
    R apply(T t) throws E;
}

/**
 * Return a Function that wraps a ThrowingFunction
 * which catches/throws an exception;
 * used.
 * @param <T> function arg type
 * @param <R> function return type
 * @param throwingFunction wrap this into a plain Function
 * @return function
 */
public static <T,R> Function<T,R> throwExFunction(
    ThrowingFunction<T, R, Exception> throwingFunction)
{
    return (T t) -> {
        try {
            return throwingFunction.apply(t);
        } catch (Exception ex) {
            throw new FunctionalExWrapper(ex);
        }
    };
}

/**
 * Return a Function that wraps a ThrowingFunction and,
 * if an exception occurs, invokes the specified exception handler.
 * 
 * @param <T> function arg type
 * @param <R> function return type
 * @param throwingFunction wrap this into a plain Function
 * @param handler 
 * @return function
 */
public static <T,R> Function<T,R> handleExFunction(
    ThrowingFunction<T, R, Exception> throwingFunction, Function<Exception,R> handler)
{
    return (T t) -> {
        try {
            return throwingFunction.apply(t);
        } catch (Exception ex) {
            return handler.apply(ex);
        }
    };
}

// This seems like clutter
// public static <T,R,E extends Exception> Function<T,R> handleExFunction(
//     ThrowingFunction<T, R, Exception> throwingFunction, Class<E> exClass, Logger logger)
// {
//     Function<Exception,R> handler = exLogHandler(exClass, logger);
//     return handleExFunction(throwingFunction, handler);
// }

/**
 * Handle an exception; log the exception as a warning if it's what's expected,
 * return null. Wrap and throw if not the specified type. This can be used at the
 * {@code handler} for {@code handleExFunction}.
 * <p>
 * Could define something like this where needed and
 * use a hard coded exception type and an in scope logger/level. For example
 * <pre>{@code 
 * Function<Exception,String> exLogHandlerExample()
 * {
 *     Function<Exception,String> handler = (ex) -> {
 *         if(ex instanceof IOException) {
 *             LOG.info(() -> "Exception: " + ex.getMessage());
 *             return null;
 *         }
 *         throw new FunctionalExWrapper(ex);
 *     };
 *     return handler;
 * }
 * }</pre>
 * 
 * @param <E> type of exception to log
 * @param <R> return type
 * @param exClass exception class to log
 * @param logger use this logger, may be null
 * @return null
 */
public static <E extends Exception,R> Function<Exception,R> exLogHandler(
        Class<E> exClass, Logger logger)
{
    Function<Exception,R> handler = (ex) -> {
        try {
            E targetEx = exClass.cast(ex);
            if(logger != null)
                logger.warning(() -> "Exception: " + targetEx.getMessage());
            return null;
        } catch(ClassCastException ccEx) {
            throw new FunctionalExWrapper(ex);
        }
    };
    return handler;
}

private LambdaEx() { }

}
