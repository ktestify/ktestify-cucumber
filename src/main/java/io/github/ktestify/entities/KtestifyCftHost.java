/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.entities;

import lombok.Builder;
import lombok.Value;

/**
 * Represents a CFT (Connectivity / File Transfer) host registered in a Cucumber scenario via {@code Given CFT hosts}.
 *
 * <p>Used by the {@code When file is sent using CFT} step to establish a remote file transfer connection.
 */
@Value
@Builder
public class KtestifyCftHost {

    /** Remote host address (IP or hostname). */
    String host;

    /** Remote port. */
    int port;

    /** Authentication username. */
    String username;

    /** Authentication password. */
    String password;

    /** Base path on the remote server for file transfers. */
    String basePath;

    /** Optional alias used in step definitions to reference this host. */
    String cftAlias;
}
