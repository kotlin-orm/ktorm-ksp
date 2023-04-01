/*
 * Copyright 2022-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.api

/**
 * Specify the table for an entity class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Table(

    /**
     * The name of the table.
     *
     * If not specified, the name will be generated by a naming strategy. This naming strategy can be configured
     * by KSP option `ktorm.dbNamingStrategy`, which accepts the following values:
     *
     * - lower-snake-case (default): generate lower snake-case names, for example: UserProfile --> user_profile.
     *
     * - upper-snake-case: generate upper snake-case names, for example: UserProfile --> USER_PROFILE.
     *
     * - Class name of a custom naming strategy, which should be an implementation of
     * `org.ktorm.ksp.spi.DatabaseNamingStrategy`.
     */
    val name: String = "",

    /**
     * The alias of the table.
     */
    val alias: String = "",

    /**
     * The catalog of the table.
     *
     * The default value can be configured by KSP option `ktorm.catalog`.
     */
    val catalog: String = "",

    /**
     * The schema of the table.
     *
     * The default value can be configured by KSP option `ktorm.schema`.
     */
    val schema: String = "",

    /**
     * The name of the corresponding table class in the generated code.
     *
     * If not specified, the name will be the plural form of the annotated class's name,
     * for example: UserProfile --> UserProfiles. This behavior can be configured by KSP option
     * `ktorm.codingNamingStrategy`, which accepts an implementation class name of
     * `org.ktorm.ksp.spi.CodingNamingStrategy`.
     */
    val className: String = "",

    /**
     * The name of the corresponding entity sequence in the generated code.
     *
     * If not specified, the name will be the plural form of the annotated class's name, with the first word in
     * lower case, for example: UserProfile --> userProfiles. This behavior can be configured by KSP option
     * `ktorm.codingNamingStrategy`, which accepts an implementation class name of
     * `org.ktorm.ksp.spi.CodingNamingStrategy`.
     */
    val entitySequenceName: String = "",

    /**
     * Specify properties that should be ignored for generating column definitions.
     */
    val ignoreProperties: Array<String> = []
)
