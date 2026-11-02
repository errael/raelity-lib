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

import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.raelity.lib.lambda.LambdaEx.FunctionalExWrapper;

import static org.junit.jupiter.api.Assertions.*;

import static com.raelity.lib.lambda.LambdaEx.throwExFunction;
import static com.raelity.lib.lambda.LambdaEx.handleExFunction;

/**
 *
 * @author err
 */
public class LambdaExTest
{

public LambdaExTest()
{
}

@BeforeAll
public static void setUpClass()
{
}

@AfterAll
public static void tearDownClass()
{
}

@BeforeEach
public void setUp()
{
}

@AfterEach
public void tearDown()
{
}

private String useFunction(Function<Integer,String> func, Integer arg)
{
    return func.apply(arg);
}

boolean exceptionHandled;
String resultMethod;

private String sampleMethod(Integer i) throws IOException
{
    resultMethod = i.toString();
    return resultMethod;
}

private String sampleMethodIOEx(Integer i) throws IOException
{
    if(Boolean.FALSE)
        return i.toString();
    throw new IOException();
}

private String sampleMethodIll(Integer i) throws IOException
{
    if(Boolean.FALSE)
        return i.toString();
    throw new IllegalArgumentException();
}

/**
 * Test of throwExFunction method, of class LambdaEx.
 */
@Test
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "ThrowableResultIgnored"})
public void testThrowExFunction()
{
    System.out.println("throwExFunction");

    // useFunction((i) -> sampleMethodIOEx(i), 4);

    resultMethod = null;
    useFunction(throwExFunction((i) -> sampleMethod(i)), 4);
    assertEquals("4", resultMethod);

    resultMethod = null;
    Exception ex = assertThrows(Exception.class, () ->
            useFunction(throwExFunction((i) -> sampleMethodIOEx(i)), 4));
    assertInstanceOf(FunctionalExWrapper.class, ex);
    assertInstanceOf(IOException.class, ex.getCause());
    assertNull(resultMethod);
}


/**
 * Note that if there is a final Logger, LOG, declared in this class and that's
 * the logger to use, then this method could be like:
 * <pre>{@code
 <E extends Exception,R> Function<Exception,R> exLogHandler(Class<E> exClass)
 {
 ...
          LOG.warning(() -> "Handling Exception: " + targetEx.getMessage());
 ...
 }
 }</pre>
 */
<E extends Exception,R> Function<Exception,R> exLogHandler(
        Class<E> exClass, Logger logger)
{
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    Function<Exception,R> handler = (ex) -> {
        try {
            E targetEx = exClass.cast(ex);
            exceptionHandled = true;
            System.out.println("Handling Exception: " + targetEx.getMessage());
            // Typically LOG....
            if(Boolean.FALSE)
                logger.warning(() -> "Handling Exception: " + targetEx.getMessage());
            // NOTE: if return null (after all it did get an exception)
            //       then don't need any funny business with (R).
            // In this case want to force a String and risk a clas cast exception.
            // If want to return something other than null,
            // then probably don't generecize
            @SuppressWarnings("unchecked")
            R rval = (R)"xyzzy";
            return rval;
        } catch(ClassCastException ccEx) {
            throw new FunctionalExWrapper(ex);
        }
    };
    return handler;
}

/**
 * Test of handleExFunction method, of class LambdaEx.
 */
@Test
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "ThrowableResultIgnored"})
public void testHandleExFunction_LambdaExThrowingFunction_Function()
{
    System.out.println("handleExFunction");

    Function<Integer, String> handleExFunction
            = handleExFunction((i) -> sampleMethodIOEx(i),
                               exLogHandler(IOException.class, null));
    exceptionHandled = false;
    resultMethod = null;
    String result = useFunction(handleExFunction, 4);
    assertTrue(exceptionHandled);
    assertNull(resultMethod);
    assertEquals("xyzzy", result);

    Function<Integer, String> handleExFunction2
            = handleExFunction((i) -> sampleMethodIll(i),
                               exLogHandler(IOException.class, null));
    exceptionHandled = false;
    resultMethod = null;
    Exception ex = assertThrows(Exception.class,
                                () -> useFunction(handleExFunction2, 4));
    assertFalse(exceptionHandled);
    assertNull(resultMethod);
    assertInstanceOf(FunctionalExWrapper.class, ex);
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
}

}
