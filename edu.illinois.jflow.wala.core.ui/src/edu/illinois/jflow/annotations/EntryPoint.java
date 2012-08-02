/**
 * This class derives from https://github.com/reprogrammer/keshmesh/ and is licensed under Illinois Open Source License.
 */
package edu.illinois.jflow.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modified from EntryPoint.java, originally from Keshmesh. Authored by Mohsen Vakilian and Stas
 * Negara. Modified by Nicholas Chen.
 * 
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface EntryPoint {

}
